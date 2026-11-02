package aurum.aurum.entity.AurumVillager;

import aurum.aurum.worldgen.Dimension.AurumNightCycle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Aldeano propio de Aurum.
 *
 * Es un PathfinderMob (no un Monster ni un Villager):
 *  - un Monster se elimina solo en dificultad Pacífica y los golems vanilla lo atacan;
 *  - un Villager arrastra cerebro, camas, comercio y conversión a zombi que aquí no se quieren.
 *
 * FASE 3:
 *  - Pasivo en el periodo tranquilo y hostil en el periodo hostil (AurumNightCycle), solo en su dimensión de origen.
 *  - Mientras la divine_armor de su pueblo siga viva no se transforma (AurumVillageData).
 *  - Si algún aldeano recibe daño, los de alrededor lo atacan también (HurtByTargetGoal con alerta).
 *
 * Aún NO hay: contratos (Fase 5), reputación y hostilidad permanente (Fase 4).
 */
public class AurumVillagerEntity extends PathfinderMob {

    /** NINI = aldeano sin profesión (los "verdes"). */
    public enum Profession {
        NINI,
        FIGHTER,    // luchador
        MINER,      // minero
        BANKER,     // banquero (cambista)
        LIBRARIAN;  // bibliotecario (función por definir)

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Profession byId(@Nullable String id) {
            if (id != null) {
                for (Profession p : values()) {
                    if (p.id().equals(id)) {
                        return p;
                    }
                }
            }
            return NINI;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_PROFESSION =
            SynchedEntityData.defineId(AurumVillagerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE_FORM =
            SynchedEntityData.defineId(AurumVillagerEntity.class, EntityDataSerializers.BOOLEAN);

    /** Cada cuántos ticks se revisa si toca cambiar de forma. */
    private static final int STATE_CHECK_INTERVAL = 20;

    /** Pueblo al que pertenece (UUID de su divine_armor). Solo en el servidor. */
    @Nullable
    private UUID villageId;
    /** Dimensión en la que nació: solo allí se vuelve hostil. */
    @Nullable
    private ResourceLocation homeDimension;

    public AurumVillagerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // ===== Atributos (valores provisionales: "bastante fuertes", se ajustan al probar) =====
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ===== Datos sincronizados =====
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFESSION, Profession.NINI.ordinal());
        builder.define(DATA_HOSTILE_FORM, false);
    }

    public Profession getProfession() {
        int index = this.entityData.get(DATA_PROFESSION);
        Profession[] values = Profession.values();
        return index >= 0 && index < values.length ? values[index] : Profession.NINI;
    }

    public void setProfession(Profession profession) {
        this.entityData.set(DATA_PROFESSION, profession.ordinal());
    }

    /** true = en forma hostil. */
    public boolean isHostileForm() {
        return this.entityData.get(DATA_HOSTILE_FORM);
    }

    public void setHostileForm(boolean hostile) {
        this.entityData.set(DATA_HOSTILE_FORM, hostile);
    }

    @Nullable
    public UUID getVillageId() {
        return villageId;
    }

    // ===== IA =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidExposedPlayerGoal(this, 12.0F, 1.0D, 1.3D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new FollowContractOwnerGoal(this, 1.1D, 6.0F, 3.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Quien hiere a un aldeano, sea cuando sea, se gana a los aldeanos de alrededor (radio = FOLLOW_RANGE).
        // La hostilidad de todo el pueblo y permanente llegará en la Fase 4.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());

        // En forma hostil, ataca a los jugadores que vea.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return AurumVillagerEntity.this.isHostileForm() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return AurumVillagerEntity.this.isHostileForm() && super.canContinueToUse();
            }
        });
    }

    // ===== Forma hostil / tranquila =====
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % STATE_CHECK_INTERVAL == 0) {
            this.updateHostileState();
        }
    }

    private void updateHostileState() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ResourceLocation dimension = serverLevel.dimension().location();
        if (this.homeDimension == null) {
            this.homeDimension = dimension;
        }
        boolean inHomeDimension = dimension.equals(this.homeDimension);

        AurumVillageData data = AurumVillageData.get(serverLevel);
        if (this.villageId == null) {
            // Su divine_armor puede entrar al nivel después que él: se reintenta en cada revisión.
            AurumVillageData.Village found = data.findNearest(this.blockPosition());
            if (found != null) {
                this.villageId = found.id;
            }
        }
        AurumVillageData.Village village = this.villageId == null ? null : data.get(this.villageId);
        // La divine_armor protege mientras viva, salvo en la noche de la luna morada, en la que se retira.
        boolean protectedByGolem = village != null && village.golemAlive
                && !AurumNightCycle.isProtectionSuspended(serverLevel);

        boolean shouldBeHostile = inHomeDimension
                && AurumNightCycle.isHostilePeriod(serverLevel)
                && !protectedByGolem;

        if (shouldBeHostile != this.isHostileForm()) {
            this.setHostileForm(shouldBeHostile);
            if (!shouldBeHostile) {
                this.setTarget(null);
            }
        }
    }

    /** No desaparecen al alejarse el jugador. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ===== Guardado =====
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Profession", this.getProfession().id());
        compound.putBoolean("HostileForm", this.isHostileForm());
        if (this.villageId != null) {
            compound.putString("VillageId", this.villageId.toString());
        }
        if (this.homeDimension != null) {
            compound.putString("HomeDimension", this.homeDimension.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Profession", Tag.TAG_STRING)) {
            this.setProfession(Profession.byId(compound.getString("Profession")));
        }
        this.setHostileForm(compound.getBoolean("HostileForm"));
        if (compound.contains("VillageId", Tag.TAG_STRING)) {
            try {
                this.villageId = UUID.fromString(compound.getString("VillageId"));
            } catch (IllegalArgumentException ignored) {
                this.villageId = null;
            }
        }
        if (compound.contains("HomeDimension", Tag.TAG_STRING)) {
            this.homeDimension = ResourceLocation.tryParse(compound.getString("HomeDimension"));
        }
    }

    // ===== Sonidos provisionales (los de aldeano vanilla) =====
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}