package aurum.aurum.entity.VexAltered;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;


public class VexAlteredEntity extends Monster implements TraceableEntity {
    public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
    public static final int TICKS_PER_FLAP = Mth.ceil((float) (Math.PI * 5.0 / 4.0));
    protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(VexAlteredEntity.class, EntityDataSerializers.BYTE);
    private static final int FLAG_IS_CHARGING = 1;
    @Nullable
    Mob owner;
    @Nullable
    private BlockPos boundOrigin;
    private boolean hasLimitedLife;
    private int limitedLifeTicks;

    public VexAlteredEntity(EntityType<? extends VexAlteredEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new VexAlteredEntity.VexMoveControl(this);
        this.xpReward = 3;
    }

    @Override
    public boolean isFlapping() {
        return this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        super.move(type, pos);
        this.checkInsideBlocks();
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);
        if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
            this.limitedLifeTicks = 20;
            this.hurt(this.damageSources().starve(), 1.0F);
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new VexAlteredEntity.VexChargeAttackGoal());
        this.goalSelector.addGoal(8, new VexAlteredEntity.VexRandomMoveGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(2, new VexAlteredEntity.VexCopyOwnerTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS_ID, (byte)0);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BoundX")) {
            this.boundOrigin = new BlockPos(compound.getInt("BoundX"), compound.getInt("BoundY"), compound.getInt("BoundZ"));
        }

        if (compound.contains("LifeTicks")) {
            this.setLimitedLife(compound.getInt("LifeTicks"));
        }
    }

    /**
     * Prepares this entity in new dimension by copying NBT data from entity in old dimension
     */
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof net.minecraft.world.entity.monster.Vex vex) {
            this.owner = vex.getOwner();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.boundOrigin != null) {
            compound.putInt("BoundX", this.boundOrigin.getX());
            compound.putInt("BoundY", this.boundOrigin.getY());
            compound.putInt("BoundZ", this.boundOrigin.getZ());
        }

        if (this.hasLimitedLife) {
            compound.putInt("LifeTicks", this.limitedLifeTicks);
        }
    }

    @Nullable
    public Mob getOwner() {
        return this.owner;
    }

    @Nullable
    public BlockPos getBoundOrigin() {
        return this.boundOrigin;
    }

    public void setBoundOrigin(@Nullable BlockPos boundOrigin) {
        this.boundOrigin = boundOrigin;
    }

    private boolean getVexFlag(int mask) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        return (i & mask) != 0;
    }

    private void setVexFlag(int mask, boolean value) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }

        this.entityData.set(DATA_FLAGS_ID, (byte)(i & 0xFF));
    }

    public boolean isCharging() {
        return this.getVexFlag(1);
    }

    public void setIsCharging(boolean charging) {
        this.setVexFlag(1, charging);
    }

    public void setOwner(Mob owner) {
        this.owner = owner;
    }

    public void setLimitedLife(int limitedLifeTicks) {
        this.hasLimitedLife = true;
        this.limitedLifeTicks = limitedLifeTicks;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        RandomSource randomsource = level.getRandom();
        this.populateDefaultEquipmentSlots(randomsource, difficulty);
        this.populateDefaultEquipmentEnchantments(level, randomsource, difficulty);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    class VexChargeAttackGoal extends Goal {
        public VexChargeAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = VexAlteredEntity.this.getTarget();
            return livingentity != null
                    && livingentity.isAlive()
                    && !VexAlteredEntity.this.getMoveControl().hasWanted()
                    && VexAlteredEntity.this.random.nextInt(reducedTickDelay(7)) == 0
                    ? VexAlteredEntity.this.distanceToSqr(livingentity) > 4.0
                    : false;
        }

        @Override
        public boolean canContinueToUse() {
            return VexAlteredEntity.this.getMoveControl().hasWanted() && VexAlteredEntity.this.isCharging() && VexAlteredEntity.this.getTarget() != null && VexAlteredEntity.this.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity livingentity = VexAlteredEntity.this.getTarget();
            if (livingentity != null) {
                Vec3 vec3 = livingentity.getEyePosition();
                VexAlteredEntity.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0);
            }

            VexAlteredEntity.this.setIsCharging(true);
            VexAlteredEntity.this.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            VexAlteredEntity.this.setIsCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingentity = VexAlteredEntity.this.getTarget();
            if (livingentity != null) {
                if (VexAlteredEntity.this.getBoundingBox().intersects(livingentity.getBoundingBox())) {
                    VexAlteredEntity.this.doHurtTarget(livingentity);
                    VexAlteredEntity.this.setIsCharging(false);
                } else {
                    double d0 = VexAlteredEntity.this.distanceToSqr(livingentity);
                    if (d0 < 9.0) {
                        Vec3 vec3 = livingentity.getEyePosition();
                        VexAlteredEntity.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0);
                    }
                }
            }
        }
    }

    class VexCopyOwnerTargetGoal extends TargetGoal {
        private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

        public VexCopyOwnerTargetGoal(PathfinderMob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            return VexAlteredEntity.this.owner != null && VexAlteredEntity.this.owner.getTarget() != null && this.canAttack(VexAlteredEntity.this.owner.getTarget(), this.copyOwnerTargeting);
        }

        @Override
        public void start() {
            VexAlteredEntity.this.setTarget(VexAlteredEntity.this.owner.getTarget());
            super.start();
        }
    }

    class VexMoveControl extends MoveControl {
        public VexMoveControl(VexAlteredEntity vex) {
            super(vex);
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                Vec3 vec3 = new Vec3(this.wantedX - VexAlteredEntity.this.getX(), this.wantedY - VexAlteredEntity.this.getY(), this.wantedZ - VexAlteredEntity.this.getZ());
                double d0 = vec3.length();
                if (d0 < VexAlteredEntity.this.getBoundingBox().getSize()) {
                    this.operation = MoveControl.Operation.WAIT;
                    VexAlteredEntity.this.setDeltaMovement(VexAlteredEntity.this.getDeltaMovement().scale(0.5));
                } else {
                    VexAlteredEntity.this.setDeltaMovement(VexAlteredEntity.this.getDeltaMovement().add(vec3.scale(this.speedModifier * 0.05 / d0)));
                    if (VexAlteredEntity.this.getTarget() == null) {
                        Vec3 vec31 = VexAlteredEntity.this.getDeltaMovement();
                        VexAlteredEntity.this.setYRot(-((float)Mth.atan2(vec31.x, vec31.z)) * (180.0F / (float)Math.PI));
                        VexAlteredEntity.this.yBodyRot = VexAlteredEntity.this.getYRot();
                    } else {
                        double d2 = VexAlteredEntity.this.getTarget().getX() - VexAlteredEntity.this.getX();
                        double d1 = VexAlteredEntity.this.getTarget().getZ() - VexAlteredEntity.this.getZ();
                        VexAlteredEntity.this.setYRot(-((float)Mth.atan2(d2, d1)) * (180.0F / (float)Math.PI));
                        VexAlteredEntity.this.yBodyRot = VexAlteredEntity.this.getYRot();
                    }
                }
            }
        }
    }

    class VexRandomMoveGoal extends Goal {
        public VexRandomMoveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !VexAlteredEntity.this.getMoveControl().hasWanted() && VexAlteredEntity.this.random.nextInt(reducedTickDelay(7)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos blockpos = VexAlteredEntity.this.getBoundOrigin();
            if (blockpos == null) {
                blockpos = VexAlteredEntity.this.blockPosition();
            }

            for (int i = 0; i < 3; i++) {
                BlockPos blockpos1 = blockpos.offset(VexAlteredEntity.this.random.nextInt(15) - 7, VexAlteredEntity.this.random.nextInt(11) - 5, VexAlteredEntity.this.random.nextInt(15) - 7);
                if (VexAlteredEntity.this.level().isEmptyBlock(blockpos1)) {
                    VexAlteredEntity.this.moveControl
                            .setWantedPosition((double)blockpos1.getX() + 0.5, (double)blockpos1.getY() + 0.5, (double)blockpos1.getZ() + 0.5, 0.25);
                    if (VexAlteredEntity.this.getTarget() == null) {
                        VexAlteredEntity.this.getLookControl()
                                .setLookAt((double)blockpos1.getX() + 0.5, (double)blockpos1.getY() + 0.5, (double)blockpos1.getZ() + 0.5, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }
}
