package aurum.aurum.entity.DolphinAltered;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class DolphinAlteredEntity extends Monster {
    private static final EntityDataAccessor<BlockPos> TREASURE_POS = SynchedEntityData.defineId(DolphinAlteredEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> GOT_FISH = SynchedEntityData.defineId(DolphinAlteredEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOISTNESS_LEVEL = SynchedEntityData.defineId(DolphinAlteredEntity.class, EntityDataSerializers.INT);
    static final TargetingConditions SWIM_WITH_PLAYER_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
    public static final int TOTAL_AIR_SUPPLY = 4800;
    private static final int TOTAL_MOISTNESS_LEVEL = 2400;
    public static final Predicate<ItemEntity> ALLOWED_ITEMS = p_350083_ -> !p_350083_.hasPickUpDelay() && p_350083_.isAlive() && p_350083_.isInWater();

    public DolphinAlteredEntity(EntityType<? extends DolphinAlteredEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setCanPickUpLoot(true);

        // ¡LA MAGIA PARA QUE NO CAIGA!
        this.setNoGravity(true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.setAirSupply(this.getMaxAirSupply());
        this.setXRot(0.0F);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public void setTreasurePos(BlockPos pos) {
        this.entityData.set(TREASURE_POS, pos);
    }

    public BlockPos getTreasurePos() {
        return this.entityData.get(TREASURE_POS);
    }

    public boolean gotFish() {
        return this.entityData.get(GOT_FISH);
    }

    public void setGotFish(boolean gotFish) {
        this.entityData.set(GOT_FISH, gotFish);
    }

    public int getMoistnessLevel() {
        return this.entityData.get(MOISTNESS_LEVEL);
    }

    public void setMoisntessLevel(int moistnessLevel) {
        this.entityData.set(MOISTNESS_LEVEL, moistnessLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TREASURE_POS, BlockPos.ZERO);
        builder.define(GOT_FISH, false);
        builder.define(MOISTNESS_LEVEL, 2400);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TreasurePosX", this.getTreasurePos().getX());
        compound.putInt("TreasurePosY", this.getTreasurePos().getY());
        compound.putInt("TreasurePosZ", this.getTreasurePos().getZ());
        compound.putBoolean("GotFish", this.gotFish());
        compound.putInt("Moistness", this.getMoistnessLevel());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        int i = compound.getInt("TreasurePosX");
        int j = compound.getInt("TreasurePosY");
        int k = compound.getInt("TreasurePosZ");
        this.setTreasurePos(new BlockPos(i, j, k));
        super.readAdditionalSaveData(compound);
        this.setGotFish(compound.getBoolean("GotFish"));
        this.setMoisntessLevel(compound.getInt("Moistness"));
    }

    @Override
    protected void registerGoals() {
        // --- QUIÉN ES EL ENEMIGO (TARGETS) ---
        // Prioridad 1: Vengarse si le pegan (y llamar a sus amigos)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // Prioridad 2: Buscar jugadores a la vista para matarlos
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // --- QUÉ HACER (GOALS) ---
        // Prioridad 1: ¡ATACAR! (Debe ser la más alta para interrumpir el vuelo pacífico)
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2F, true));

        // Prioridad 2: Huir de los Guardianes si ve uno
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Guardian.class, 8.0F, 1.0, 1.0));

        // Prioridad 4: Volar por las nubes libremente (solo lo hará si no está atacando)
        this.goalSelector.addGoal(4, new CloudSwimmerWanderGoal(this));

        // Prioridad 5: Jugar con objetos tirados (opcional, si quieres que se distraigan)
        this.goalSelector.addGoal(5, new PlayWithItemsGoal());

        // Prioridad 6: Comportamientos inactivos (mirar al jugador y alrededor)
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FLYING_SPEED, 0.6F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingNavigation = new FlyingPathNavigation(this, level);
        flyingNavigation.setCanOpenDoors(false);
        flyingNavigation.setCanFloat(true);
        return flyingNavigation;
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack itemstack) {
        EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(itemstack);
        return !this.getItemBySlot(equipmentslot).isEmpty() ? false : equipmentslot == EquipmentSlot.MAINHAND && super.canTakeItem(itemstack);
    }

    /**
     * Tests if this entity should pick up a weapon or an armor piece. Entity drops current weapon or armor if the new one is better.
     */
    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            ItemStack itemstack = itemEntity.getItem();
            if (this.canHoldItem(itemstack)) {
                this.onItemPickup(itemEntity);
                this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
                this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                this.take(itemEntity, itemstack.getCount());
                itemEntity.discard();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // ¡IMPORTANTE! Hemos eliminado todo lo relacionado con MOISTNESS_LEVEL y this.damageSources().dryOut()

        // Efecto visual: Partículas mágicas al volar
        if (this.level().isClientSide && !this.onGround() && this.getDeltaMovement().lengthSqr() > 0.03) {
            Vec3 vec3 = this.getViewVector(0.0F);
            float f = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * 0.3F;
            float f1 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * 0.3F;
            float f2 = 1.2F - this.random.nextFloat() * 0.7F;

            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getX() - vec3.x * (double)f2 + (double)f,
                        this.getY() - vec3.y,
                        this.getZ() - vec3.z * (double)f2 + (double)f1,
                        0.0, 0.0, 0.0);
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getX() - vec3.x * (double)f2 - (double)f,
                        this.getY() - vec3.y,
                        this.getZ() - vec3.z * (double)f2 - (double)f1,
                        0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 38) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void addParticlesAroundSelf(ParticleOptions particleOption) {
        for (int i = 0; i < 7; i++) {
            double d0 = this.random.nextGaussian() * 0.01;
            double d1 = this.random.nextGaussian() * 0.01;
            double d2 = this.random.nextGaussian() * 0.01;
            this.level().addParticle(particleOption, this.getRandomX(1.0), this.getRandomY() + 0.2, this.getRandomZ(1.0), d0, d1, d2);
        }
    }
/*
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {

        ItemStack itemstack = player.getItemInHand(hand);
        if (!itemstack.isEmpty() && itemstack.is(ItemTags.FISHES)) {
            if (!this.level().isClientSide) {
                this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
            }

            this.setGotFish(true);
            itemstack.consume(1, player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }
*/
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    protected boolean closeToNextPos() {
        BlockPos blockpos = this.getNavigation().getTargetPos();
        return blockpos != null ? blockpos.closerToCenterThan(this.position(), 12.0) : false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            this.moveRelative(0.04F, travelVector); // Define cómo acelera en el aire
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D)); // Fricción del aire
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    static class DolphinAlteredSwimToTreasureGoal extends Goal {
        private final DolphinAlteredEntity dolphin;
        private boolean stuck;

        DolphinAlteredSwimToTreasureGoal(DolphinAlteredEntity dolphin) {
            this.dolphin = dolphin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean canUse() {
            return this.dolphin.gotFish() && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos blockpos = this.dolphin.getTreasurePos();
            return !BlockPos.containing((double)blockpos.getX(), this.dolphin.getY(), (double)blockpos.getZ()).closerToCenterThan(this.dolphin.position(), 4.0)
                    && !this.stuck
                    && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public void start() {
            if (this.dolphin.level() instanceof ServerLevel) {
                ServerLevel serverlevel = (ServerLevel)this.dolphin.level();
                this.stuck = false;
                this.dolphin.getNavigation().stop();
                BlockPos blockpos = this.dolphin.blockPosition();
                BlockPos blockpos1 = serverlevel.findNearestMapStructure(StructureTags.DOLPHIN_LOCATED, blockpos, 50, false);
                if (blockpos1 != null) {
                    this.dolphin.setTreasurePos(blockpos1);
                    serverlevel.broadcastEntityEvent(this.dolphin, (byte)38);
                } else {
                    this.stuck = true;
                }
            }
        }

        @Override
        public void stop() {
            BlockPos blockpos = this.dolphin.getTreasurePos();
            if (BlockPos.containing((double)blockpos.getX(), this.dolphin.getY(), (double)blockpos.getZ()).closerToCenterThan(this.dolphin.position(), 4.0)
                    || this.stuck) {
                this.dolphin.setGotFish(false);
            }
        }

        @Override
        public void tick() {
            Level level = this.dolphin.level();
            if (this.dolphin.closeToNextPos() || this.dolphin.getNavigation().isDone()) {
                Vec3 vec3 = Vec3.atCenterOf(this.dolphin.getTreasurePos());
                Vec3 vec31 = DefaultRandomPos.getPosTowards(this.dolphin, 16, 1, vec3, (float) (Math.PI / 8));
                if (vec31 == null) {
                    vec31 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 4, vec3, (float) (Math.PI / 2));
                }

                if (vec31 != null) {
                    BlockPos blockpos = BlockPos.containing(vec31);
                    if (!level.getFluidState(blockpos).is(FluidTags.WATER) || !level.getBlockState(blockpos).isPathfindable(PathComputationType.WATER)) {
                        vec31 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 5, vec3, (float) (Math.PI / 2));
                    }
                }

                if (vec31 == null) {
                    this.stuck = true;
                    return;
                }

                this.dolphin
                        .getLookControl()
                        .setLookAt(vec31.x, vec31.y, vec31.z, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
                this.dolphin.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, 1.3);
                if (level.random.nextInt(this.adjustedTickDelay(80)) == 0) {
                    level.broadcastEntityEvent(this.dolphin, (byte)38);
                }
            }
        }
    }

    static class DolphinAlteredSwimWithPlayerGoal extends Goal {
        private final DolphinAlteredEntity dolphin;
        private final double speedModifier;
        @Nullable
        private Player player;

        DolphinAlteredSwimWithPlayerGoal(DolphinAlteredEntity dolphin, double speedModifier) {
            this.dolphin = dolphin;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = this.dolphin.level().getNearestPlayer(DolphinAlteredEntity.SWIM_WITH_PLAYER_TARGETING, this.dolphin);
            return this.player == null ? false : this.player.isSwimming() && this.dolphin.getTarget() != this.player;
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0;
        }

        @Override
        public void start() {
            this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
        }

        @Override
        public void stop() {
            this.player = null;
            this.dolphin.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.dolphin.getLookControl().setLookAt(this.player, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.player) < 6.25) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
            }

            if (this.player.isSwimming() && this.player.level().random.nextInt(6) == 0) {
                this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
            }
        }
    }

    class PlayWithItemsGoal extends Goal {
        private int cooldown;

        @Override
        public boolean canUse() {
            if (this.cooldown > DolphinAlteredEntity.this.tickCount) {
                return false;
            } else {
                List<ItemEntity> list = DolphinAlteredEntity.this.level()
                        .getEntitiesOfClass(ItemEntity.class, DolphinAlteredEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), DolphinAlteredEntity.ALLOWED_ITEMS);
                return !list.isEmpty() || !DolphinAlteredEntity.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
            }
        }

        @Override
        public void start() {
            List<ItemEntity> list = DolphinAlteredEntity.this.level()
                    .getEntitiesOfClass(ItemEntity.class, DolphinAlteredEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), DolphinAlteredEntity.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                DolphinAlteredEntity.this.getNavigation().moveTo(list.get(0), 1.2F);
                DolphinAlteredEntity.this.playSound(SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
            }

            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemstack = DolphinAlteredEntity.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemstack.isEmpty()) {
                this.drop(itemstack);
                DolphinAlteredEntity.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldown = DolphinAlteredEntity.this.tickCount + DolphinAlteredEntity.this.random.nextInt(100);
            }
        }

        @Override
        public void tick() {
            List<ItemEntity> list = DolphinAlteredEntity.this.level()
                    .getEntitiesOfClass(ItemEntity.class, DolphinAlteredEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), DolphinAlteredEntity.ALLOWED_ITEMS);
            ItemStack itemstack = DolphinAlteredEntity.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemstack.isEmpty()) {
                this.drop(itemstack);
                DolphinAlteredEntity.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldown = DolphinAlteredEntity.this.tickCount + DolphinAlteredEntity.this.random.nextInt(100);
            } else if (!list.isEmpty()) {
                DolphinAlteredEntity.this.getNavigation().moveTo(list.get(0), 1.2F);
            }
        }

        private void drop(ItemStack stack) {
            if (!stack.isEmpty()) {
                double d0 = DolphinAlteredEntity.this.getEyeY() - 0.3F;
                ItemEntity itementity = new ItemEntity(DolphinAlteredEntity.this.level(), DolphinAlteredEntity.this.getX(), d0, DolphinAlteredEntity.this.getZ(), stack);
                itementity.setPickUpDelay(40);
                itementity.setThrower(DolphinAlteredEntity.this);
                float f = 0.3F;
                float f1 = DolphinAlteredEntity.this.random.nextFloat() * (float) (Math.PI * 2);
                float f2 = 0.02F * DolphinAlteredEntity.this.random.nextFloat();
                itementity.setDeltaMovement(
                        (double)(
                                0.3F * -Mth.sin(DolphinAlteredEntity.this.getYRot() * (float) (Math.PI / 180.0)) * Mth.cos(DolphinAlteredEntity.this.getXRot() * (float) (Math.PI / 180.0))
                                        + Mth.cos(f1) * f2
                        ),
                        (double)(0.3F * Mth.sin(DolphinAlteredEntity.this.getXRot() * (float) (Math.PI / 180.0)) * 1.5F),
                        (double)(
                                0.3F * Mth.cos(DolphinAlteredEntity.this.getYRot() * (float) (Math.PI / 180.0)) * Mth.cos(DolphinAlteredEntity.this.getXRot() * (float) (Math.PI / 180.0))
                                        + Mth.sin(f1) * f2
                        )
                );
                DolphinAlteredEntity.this.level().addFreshEntity(itementity);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    class CloudSwimmerWanderGoal extends Goal {
        private final DolphinAlteredEntity dolphin;

        public CloudSwimmerWanderGoal(DolphinAlteredEntity dolphin) {
            this.dolphin = dolphin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Al activarse cuando la navegación ha terminado o de forma aleatoria,
            // garantizamos que la entidad SIEMPRE esté buscando un nuevo destino y nunca frene del todo.
            return this.dolphin.getNavigation().isDone() || this.dolphin.getRandom().nextInt(15) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            // Obliga al juego a seguir ejecutando este movimiento
            return !this.dolphin.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 currentPos = this.dolphin.position();

            // 1. INERCIA: Obtenemos hacia dónde está mirando el pez para que siga nadando hacia adelante
            // Esto evita que haga giros robóticos de 180 grados y se vea fluido.
            Vec3 forward = this.dolphin.getLookAngle().scale(8.0);

            // Añadimos una ligera curva aleatoria (izquierda/derecha)
            double dx = currentPos.x + forward.x + (this.dolphin.getRandom().nextDouble() - 0.5) * 8.0;
            double dz = currentPos.z + forward.z + (this.dolphin.getRandom().nextDouble() - 0.5) * 8.0;

            Level level = this.dolphin.level();

            // 2. ESCANEO DEL SUELO: Buscamos el bloque sólido más alto en esas coordenadas (X, Z)
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)dx, (int)dz);

            // 3. LA REGLA DE LOS 30 BLOQUES: Su límite máximo de vuelo es el suelo + 30.
            // Si hay un vacío, surfaceY será muy bajo, permitiéndoles volar sobre el agujero con normalidad.
            int maxY = surfaceY + 30;

            // Calculamos una variación de altura para que "ondule" al volar
            double dy = currentPos.y + (this.dolphin.getRandom().nextDouble() - 0.5) * 6.0;

            // Aplicamos el límite: Si se pasa de alto, lo empujamos de vuelta al límite de los 30 bloques
            if (dy > maxY) {
                dy = maxY;
            }

            // (Opcional) Evitamos que se estrelle contra el suelo, manteniéndolo al menos a 3 bloques de altura mínima
            if (dy < surfaceY + 3) {
                dy = surfaceY + 3;
            }

            // Damos la orden de moverse sin pausa
            this.dolphin.getNavigation().moveTo(dx, dy, dz, 1.0);
        }
    }
}

