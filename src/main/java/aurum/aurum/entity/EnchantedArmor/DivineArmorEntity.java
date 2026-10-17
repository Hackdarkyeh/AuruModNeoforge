package aurum.aurum.entity.EnchantedArmor;

import aurum.aurum.init.ModItems;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class DivineArmorEntity extends TamableAnimal {

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
        
        // Evitamos que suelte la armadura como botín estándar (puedes gestionarlo por LootTables)
        this.setDropChance(EquipmentSlot.HEAD, 0.0f);
        this.setDropChance(EquipmentSlot.CHEST, 0.0f);
        this.setDropChance(EquipmentSlot.LEGS, 0.0f);
        this.setDropChance(EquipmentSlot.FEET, 0.0f);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
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
        return super.hurt(damagesource, amount);
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