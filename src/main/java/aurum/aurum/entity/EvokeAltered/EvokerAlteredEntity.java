package aurum.aurum.entity.EvokeAltered;

import aurum.aurum.Aurum;
import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.entity.VexAltered.VexAlteredEntity;
import aurum.aurum.init.ModAttributes;
import aurum.aurum.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;

import javax.annotation.Nullable;

public class EvokerAlteredEntity extends SpellcasterIllager implements ICorruptedMob {
    @Override public LivingEntity self() { return this; }

    // ===== Aurum en sangre (mismo sistema que ZombieAlteredEntity) =====
    private static final ResourceLocation AURUM_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_health_bonus_evoker");
    private static final ResourceLocation AURUM_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_damage_bonus_evoker");

    /** +25 % de vida base por punto de aurum sobre 1 (igual que el zombi). */
    private static final double HEALTH_PER_AURUM = 0.25D;
    /** Daño: el zombi usa 0.15; aquí 0.05 porque los colmillos pegan varias veces. Súbelo a 0.15 para igualarlo. */
    private static final double DAMAGE_PER_AURUM = 0.05D;

    // ===== Invocación =====
    private static final int VEXES_PER_CAST = 5;

    public EvokerAlteredEntity(EntityType<? extends EvokerAlteredEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 20;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EvokerCastingSpellGoal());
        // Sin AvoidEntityGoal: no huye del jugador
        this.goalSelector.addGoal(4, new EvokerSummonSpellGoal());
        this.goalSelector.addGoal(5, new EvokerAttackSpellGoal());
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.55)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(ModAttributes.AURUM_IN_BLOOD, 1.0);
    }

    // ===== Spawn: tirada de aurum =====
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        rollAurumBlood();
        return spawnGroupData;
    }

    private void rollAurumBlood() {
        float aurum = 1.0F + this.random.nextFloat() * 99.0F;
        this.getAttribute(ModAttributes.AURUM_IN_BLOOD).setBaseValue(aurum);

        this.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
                new AttributeModifier(AURUM_HEALTH_ID, (aurum - 1.0) * HEALTH_PER_AURUM,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        this.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
                new AttributeModifier(AURUM_DAMAGE_ID, (aurum - 1.0) * DAMAGE_PER_AURUM,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        this.setHealth(this.getMaxHealth());
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    /** Aliado de sus propios vexes alterados (y de los aliados de su dueño). */
    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == null) {
            return false;
        } else if (entity == this) {
            return true;
        } else if (super.isAlliedTo(entity)) {
            return true;
        } else {
            return entity instanceof VexAlteredEntity vex && vex.getOwner() != null && this.isAlliedTo(vex.getOwner());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getCastingSoundEvent() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
    }

    // =====================================================================
    // Hechizo: colmillos (EvokerFangsAltered: daño normal + daño Aurum)
    // =====================================================================
    class EvokerAttackSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 100;
        }

        @Override
        protected void performSpellCasting() {
            LivingEntity target = EvokerAlteredEntity.this.getTarget();
            if (target == null) return;
            double minY = Math.min(target.getY(), EvokerAlteredEntity.this.getY());
            double maxY = Math.max(target.getY(), EvokerAlteredEntity.this.getY()) + 1.0;
            float angle = (float) Mth.atan2(target.getZ() - EvokerAlteredEntity.this.getZ(),
                    target.getX() - EvokerAlteredEntity.this.getX());
            if (EvokerAlteredEntity.this.distanceToSqr(target) < 9.0) {
                for (int i = 0; i < 5; i++) {
                    float a = angle + (float) i * (float) Math.PI * 0.4F;
                    this.createSpellEntity(EvokerAlteredEntity.this.getX() + (double) Mth.cos(a) * 1.5,
                            EvokerAlteredEntity.this.getZ() + (double) Mth.sin(a) * 1.5, minY, maxY, a, 0);
                }
                for (int k = 0; k < 8; k++) {
                    float a = angle + (float) k * (float) Math.PI * 2.0F / 8.0F + (float) (Math.PI * 2.0 / 5.0);
                    this.createSpellEntity(EvokerAlteredEntity.this.getX() + (double) Mth.cos(a) * 2.5,
                            EvokerAlteredEntity.this.getZ() + (double) Mth.sin(a) * 2.5, minY, maxY, a, 3);
                }
            } else {
                for (int l = 0; l < 16; l++) {
                    double dist = 1.25 * (double) (l + 1);
                    this.createSpellEntity(EvokerAlteredEntity.this.getX() + (double) Mth.cos(angle) * dist,
                            EvokerAlteredEntity.this.getZ() + (double) Mth.sin(angle) * dist, minY, maxY, angle, l);
                }
            }
        }

        private void createSpellEntity(double x, double z, double minY, double maxY, float yRot, int warmupDelay) {
            Level level = EvokerAlteredEntity.this.level();
            BlockPos pos = BlockPos.containing(x, maxY, z);
            boolean found = false;
            double extraY = 0.0;

            do {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    if (!level.isEmptyBlock(pos)) {
                        BlockState state = level.getBlockState(pos);
                        VoxelShape shape = state.getCollisionShape(level, pos);
                        if (!shape.isEmpty()) {
                            extraY = shape.max(Direction.Axis.Y);
                        }
                    }
                    found = true;
                    break;
                }
                pos = pos.below();
            } while (pos.getY() >= Mth.floor(minY) - 1);

            if (found) {
                level.addFreshEntity(new EvokerFangsAltered(level, x, (double) pos.getY() + extraY, z, yRot,
                        warmupDelay, EvokerAlteredEntity.this));
                level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(x, (double) pos.getY() + extraY, z),
                        GameEvent.Context.of(EvokerAlteredEntity.this));
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_ATTACK;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.FANGS;
        }
    }

    class EvokerCastingSpellGoal extends SpellcasterIllager.SpellcasterCastingSpellGoal {
        @Override
        public void tick() {
            if (EvokerAlteredEntity.this.getTarget() != null) {
                EvokerAlteredEntity.this.getLookControl().setLookAt(EvokerAlteredEntity.this.getTarget(),
                        (float) EvokerAlteredEntity.this.getMaxHeadYRot(),
                        (float) EvokerAlteredEntity.this.getMaxHeadXRot());
            }
        }
    }

    // =====================================================================
    // Hechizo: invocar vexes alterados
    // =====================================================================
    class EvokerSummonSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private final TargetingConditions vexCountTargeting =
                TargetingConditions.forNonCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            int vexes = EvokerAlteredEntity.this.level()
                    .getNearbyEntities(VexAlteredEntity.class, this.vexCountTargeting, EvokerAlteredEntity.this,
                            EvokerAlteredEntity.this.getBoundingBox().inflate(16.0))
                    .size();
            return EvokerAlteredEntity.this.random.nextInt(10) + 1 > vexes;
        }

        @Override
        protected int getCastingTime() {
            return 100;
        }

        @Override
        protected int getCastingInterval() {
            return 200;
        }

        @Override
        protected void performSpellCasting() {
            ServerLevel serverlevel = (ServerLevel) EvokerAlteredEntity.this.level();
            PlayerTeam team = EvokerAlteredEntity.this.getTeam();

            for (int i = 0; i < VEXES_PER_CAST; i++) {
                BlockPos pos = EvokerAlteredEntity.this.blockPosition().offset(
                        -2 + EvokerAlteredEntity.this.random.nextInt(5), 1,
                        -2 + EvokerAlteredEntity.this.random.nextInt(5));
                VexAlteredEntity vex = ModEntities.VEX_ALTERED.get().create(serverlevel);
                if (vex != null) {
                    vex.moveTo(pos, 0.0F, 0.0F);
                    vex.finalizeSpawn(serverlevel, serverlevel.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
                    vex.setOwner(EvokerAlteredEntity.this);
                    vex.setBoundOrigin(pos);
                    vex.setLimitedLife(20 * (30 + EvokerAlteredEntity.this.random.nextInt(90)));
                    if (team != null) {
                        serverlevel.getScoreboard().addPlayerToTeam(vex.getScoreboardName(), team);
                    }
                    serverlevel.addFreshEntityWithPassengers(vex);
                    serverlevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(EvokerAlteredEntity.this));
                }
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.SUMMON_VEX;
        }
    }
}