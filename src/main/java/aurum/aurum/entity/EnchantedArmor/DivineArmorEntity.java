package aurum.aurum.entity.EnchantedArmor;

import aurum.aurum.Aurum;
import aurum.aurum.entity.AurumVillager.AurumVillageData;
import aurum.aurum.init.ModItems;
import aurum.aurum.item.Veilpiercer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

public class DivineArmorEntity extends TamableAnimal {

    // =====================================================================================
    // RETIRADA CON EL VEILPIERCER
    // Cuando la golem calcula que va a morir antes de poder retirarse (o que ya toca retirarse), usa el
    // Veilpiercer: canaliza CHANNEL_TICKS sin recibir daño y sin atacar. Cada golpe de un jugador durante ese
    // tiempo le da LEVELS_PER_HIT niveles de EXP y FRAGMENTS_PER_HIT fragmentos de aurelite. Al terminar se
    // retira para siempre y el pueblo queda desprotegido. Valores provisionales: se ajustan al probar.
    // =====================================================================================
    /** Id del Veilpiercer en el registro (aurum:veilpiercer). AJUSTAR si lo registraste con otro nombre. */
    private static final String VEILPIERCER_ID = "veilpiercer";

    /** 5 segundos. */
    public static final int CHANNEL_TICKS = 100;
    /** Margen: se retira si el tiempo que cree que le queda es menor que CHANNEL_TICKS * este factor. */
    private static final float RETREAT_SAFETY = 1.25F;
    /** Red de seguridad: si baja de esta fracción de vida, se retira aunque el cálculo no lo pida. */
    private static final float RETREAT_MIN_HEALTH_FRACTION = 0.10F;
    /** Ventana, en ticks, con la que estima el daño por segundo que recibe. */
    private static final int DPS_WINDOW_TICKS = 60;
    /** Mínimo de ticks entre dos recompensas (evita abusar haciendo clic sin parar). */
    private static final int REWARD_INTERVAL_TICKS = 10;
    private static final int LEVELS_PER_HIT = 20;
    private static final int FRAGMENTS_PER_HIT = 7;

    private static final EntityDataAccessor<Boolean> DATA_CHANNELING =
            SynchedEntityData.defineId(DivineArmorEntity.class, EntityDataSerializers.BOOLEAN);

    private record Hit(int tick, float amount) {}

    private final ArrayDeque<Hit> recentHits = new ArrayDeque<>();
    private int channelTicks = 0;
    private int lastRewardTick = -REWARD_INTERVAL_TICKS;

    public DivineArmorEntity(EntityType<? extends TamableAnimal> type, Level world) {
        super(type, world);
        this.xpReward = 5;
    }

    // --- ATRIBUTOS (Juggernaut) ---
    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0) // Mucha vida
                .add(Attributes.MOVEMENT_SPEED, 0.25F) // Lento e implacable
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8) // Casi inamovible
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHANNELING, false);
    }

    /** true mientras canaliza el Veilpiercer (el cliente puede usarlo para efectos visuales). */
    public boolean isChanneling() {
        return this.entityData.get(DATA_CHANNELING);
    }

    // --- COMPORTAMIENTO E IA ---
    @Override
    protected void registerGoals() {
        // Objetivos básicos de movimiento y ataque
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this)); // Se queda quieta defendiendo una zona
        this.goalSelector.addGoal(3, new DivineArmorAttackGoal(this, 1.2, true)); // Tu IA de ataque adaptada
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Objetivos de combate (Targets)
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, false)); // Ataca a cualquier monstruo
    }

    // --- EQUIPACIÓN AUTOMÁTICA ---
    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        // Forzamos a que siempre lleve tu armadura y espada al generarse
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.AURELITE_HELMET_GOLD.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.AURELITE_CHESTPLATE_GOLD.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.AURELITE_LEGGINGS_GOLD.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.AURELITE_BOOTS_GOLD.get()));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.AURELITE_SWORD.get()));
        // NUEVO: lleva el Veilpiercer en la mano izquierda, listo para usarlo al retirarse
        this.setItemSlot(EquipmentSlot.OFFHAND, createVeilpiercer());

        // Evitamos que suelte la armadura como botín estándar (puedes gestionarlo por LootTables)
        this.setDropChance(EquipmentSlot.HEAD, 0.0f);
        this.setDropChance(EquipmentSlot.CHEST, 0.0f);
        this.setDropChance(EquipmentSlot.LEGS, 0.0f);
        this.setDropChance(EquipmentSlot.FEET, 0.0f);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        // NUEVO: si muere, suelta el Veilpiercer tal cual está (un valor mayor que 1 conserva su durabilidad;
        // con 1.0 el juego le pondría una durabilidad aleatoria al soltarlo)
        this.setDropChance(EquipmentSlot.OFFHAND, 2.0f);
    }

    private static ItemStack createVeilpiercer() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, VEILPIERCER_ID));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        return spawnData;
    }



    // --- SISTEMA DE CONTRATO (Taming) ---
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Si no tiene dueño y le das un lingote de Aurelite (ejemplo de pago)
        if (!this.isTame() && itemstack.is(ModItems.AURELITE_INGOT.get())) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            if (!this.level().isClientSide) {
                this.tame(player);
                this.level().broadcastEntityEvent(this, (byte) 7); // Partículas de corazones/éxito
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Si ya es tuya, clic derecho con mano vacía cambia entre modo Patrulla/Seguir y modo Guardia (Sentada)
        if (this.isTame() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND && itemstack.isEmpty()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    // --- INMUNIDADES POR SER UNA ARMADURA VACÍA ---
    @Override
    public boolean hurt(DamageSource damagesource, float amount) {
        // Inmune a caídas, ahogamiento, magia/veneno y marchitamiento
        if (damagesource.is(DamageTypes.FALL) ||
                damagesource.is(DamageTypes.DROWN) ||
                damagesource.is(DamageTypes.MAGIC) ||
                damagesource.is(DamageTypes.WITHER)) {
            return false;
        }

        // NUEVO: mientras canaliza no recibe daño; los golpes de un jugador dan recompensa
        if (this.isChanneling()) {
            this.rewardAttacker(damagesource);
            return false;
        }

        float healthBefore = this.getHealth();
        boolean hurt = super.hurt(damagesource, amount);

        // NUEVO: apunta el daño real recibido y decide si le toca retirarse
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            float dealt = healthBefore - this.getHealth();
            if (dealt > 0.0F) {
                this.recentHits.addLast(new Hit(this.tickCount, dealt));
                if (this.shouldRetreat()) {
                    this.startChannel();
                }
            }
        }
        return hurt;
    }

    // --- RETIRADA: decisión ---
    /** Daño por segundo estimado con los golpes de los últimos DPS_WINDOW_TICKS ticks. */
    private float currentDps() {
        int now = this.tickCount;
        while (!this.recentHits.isEmpty() && now - this.recentHits.peekFirst().tick() > DPS_WINDOW_TICKS) {
            this.recentHits.removeFirst();
        }
        if (this.recentHits.isEmpty()) {
            return 0.0F;
        }
        float sum = 0.0F;
        for (Hit hit : this.recentHits) {
            sum += hit.amount();
        }
        // Al menos 1 segundo, para que un solo golpe grande no dispare el cálculo hasta el infinito
        float seconds = Math.max(1.0F, (now - this.recentHits.peekFirst().tick()) / 20.0F);
        return sum / seconds;
    }

    private boolean hasUsableVeilpiercer() {
        ItemStack stack = this.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!(stack.getItem() instanceof Veilpiercer)) {
            return false;
        }
        // Ya gastado (durabilidad 1): no se puede usar otra vez
        return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    private boolean shouldRetreat() {
        // Una golem domada es un aliado del jugador, no se retira; y sin Veilpiercer lucha hasta el final
        if (this.isTame() || this.isChanneling() || !this.hasUsableVeilpiercer()) {
            return false;
        }
        float health = this.getHealth();
        if (health <= this.getMaxHealth() * RETREAT_MIN_HEALTH_FRACTION) {
            return true;
        }
        float dps = this.currentDps();
        if (dps <= 0.0F) {
            return false;
        }
        // Tiempo que cree que le queda de vida frente a lo que tarda en retirarse
        float secondsToDie = health / dps;
        return secondsToDie <= (CHANNEL_TICKS / 20.0F) * RETREAT_SAFETY;
    }

    // --- RETIRADA: canalización ---
    private void startChannel() {
        ItemStack stack = this.getItemBySlot(EquipmentSlot.OFFHAND);
        if (stack.isDamageableItem()) {
            stack.setDamageValue(stack.getMaxDamage() - 1); // el Veilpiercer queda gastado: durabilidad 1
        }
        this.entityData.set(DATA_CHANNELING, true);
        this.channelTicks = CHANNEL_TICKS;
        this.recentHits.clear();
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoAi(true);
        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 0.8F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Golems de mundos anteriores que no llevaban el Veilpiercer: se les da al cargarse
            if (this.tickCount == 1 && !this.isTame() && this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                this.setItemSlot(EquipmentSlot.OFFHAND, createVeilpiercer());
                this.setDropChance(EquipmentSlot.OFFHAND, 2.0f);
            }
            if (this.isChanneling()) {
                this.tickChannel();
            }
        }
    }

    private void tickChannel() {
        this.channelTicks--;
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(),
                    8, 0.4, 0.8, 0.4, 0.5);
        }
        if (this.channelTicks <= 0) {
            this.completeRetreat();
        }
    }

    /** Fin de la canalización: el pueblo pierde a su guardiana para siempre y la golem desaparece. */
    private void completeRetreat() {
        if (this.level() instanceof ServerLevel serverLevel) {
            AurumVillageData.get(serverLevel).markGolemGone(this.getUUID());
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(),
                    40, 0.5, 1.0, 0.5, 0.3);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.7F);
        }
        this.discard();
    }

    // --- RETIRADA: recompensa por golpe ---
    private void rewardAttacker(DamageSource source) {
        if (this.level().isClientSide || !(source.getEntity() instanceof Player player)) {
            return;
        }
        if (this.tickCount - this.lastRewardTick < REWARD_INTERVAL_TICKS) {
            return;
        }
        this.lastRewardTick = this.tickCount;
        player.giveExperienceLevels(LEVELS_PER_HIT);
        ItemStack fragments = new ItemStack(ModItems.AURELITE_FRAGMENT.get(), FRAGMENTS_PER_HIT);
        if (!player.getInventory().add(fragments)) {
            player.drop(fragments, false);
        }
    }

    // --- Guardado de la canalización (para no perderla si se cierra el mundo a medias) ---
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ChannelTicks", this.channelTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.channelTicks = compound.getInt("ChannelTicks");
        this.entityData.set(DATA_CHANNELING, this.channelTicks > 0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
}