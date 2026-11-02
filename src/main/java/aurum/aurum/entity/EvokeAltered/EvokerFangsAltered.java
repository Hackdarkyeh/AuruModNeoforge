package aurum.aurum.entity.EvokeAltered;

import aurum.aurum.effectsPlayer.AurumBlightDamageSource;
import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Copia de EvokerFangs con daño doble:
 *  1) daño mágico normal (ATTACK_DAMAGE del evocador, que ya escala con su aurum)
 *  2) daño Aurum (AurumBlightDamageSource), que intercepta ArmorBlightShield
 */
public class EvokerFangsAltered extends Entity implements TraceableEntity {
    public static final int ATTACK_DURATION = 20;
    public static final int LIFE_OFFSET = 2;
    public static final int ATTACK_TRIGGER_TICKS = 14;

    /** Daño de respaldo si no hay dueño (como vanilla). */
    private static final float FALLBACK_DAMAGE = 6.0F;
    /** Daño Aurum = daño normal * este factor. */
    private static final float AURUM_DAMAGE_RATIO = 0.5F;

    private int warmupDelayTicks;
    private boolean sentSpikeEvent;
    private int lifeTicks = 22;
    private boolean clientSideAttackStarted;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public EvokerFangsAltered(EntityType<? extends EvokerFangsAltered> entityType, Level level) {
        super(entityType, level);
    }

    public EvokerFangsAltered(Level level, double x, double y, double z, float yRot, int warmupDelay, LivingEntity owner) {
        this(ModEntities.EVOKER_FANGS_ALTERED.get(), level);
        this.warmupDelayTicks = warmupDelay;
        this.setOwner(owner);
        this.setYRot(yRot * (180.0F / (float) Math.PI));
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
        return this.owner;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.warmupDelayTicks = compound.getInt("Warmup");
        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Warmup", this.warmupDelayTicks);
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.clientSideAttackStarted) {
                this.lifeTicks--;
                if (this.lifeTicks == 14) {
                    for (int i = 0; i < 12; i++) {
                        double d0 = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * (double) this.getBbWidth() * 0.5;
                        double d1 = this.getY() + 0.05 + this.random.nextDouble();
                        double d2 = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * (double) this.getBbWidth() * 0.5;
                        double d3 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        double d4 = 0.3 + this.random.nextDouble() * 0.3;
                        double d5 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        this.level().addParticle(ParticleTypes.CRIT, d0, d1 + 1.0, d2, d3, d4, d5);
                    }
                }
            }
        } else if (--this.warmupDelayTicks < 0) {
            if (this.warmupDelayTicks == -8) {
                for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class,
                        this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
                    this.dealDamageTo(living);
                }
            }

            if (!this.sentSpikeEvent) {
                this.level().broadcastEntityEvent(this, (byte) 4);
                this.sentSpikeEvent = true;
            }

            if (--this.lifeTicks < 0) {
                this.discard();
            }
        }
    }

    private void dealDamageTo(LivingEntity target) {
        LivingEntity caster = this.getOwner();
        if (!target.isAlive() || target.isInvulnerable() || target == caster) return;

        if (caster == null) {
            target.hurt(this.damageSources().magic(), FALLBACK_DAMAGE);
            return;
        }
        if (caster.isAlliedTo(target)) return;

        float normalDamage = (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // 1) Daño normal (mágico indirecto)
        DamageSource magic = this.damageSources().indirectMagic(this, caster);
        if (target.hurt(magic, normalDamage) && this.level() instanceof ServerLevel serverlevel) {
            EnchantmentHelper.doPostAttackEffects(serverlevel, target, magic);
        }

        // 2) Daño Aurum (solo si el dueño es un mob corrupto)
        if (caster instanceof ICorruptedMob && target.isAlive()) {
            DamageSource aurumSource = AurumBlightDamageSource.createAurumBlightDamageSource(this.level().registryAccess());
            target.invulnerableTime = 0; // si no, los i-frames del golpe normal se tragan este
            target.hurt(aurumSource, normalDamage * AURUM_DAMAGE_RATIO);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 4) {
            this.clientSideAttackStarted = true;
            if (!this.isSilent()) {
                this.level().playLocalSound(
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.EVOKER_FANGS_ATTACK, this.getSoundSource(),
                        1.0F, this.random.nextFloat() * 0.2F + 0.85F, false);
            }
        }
    }

    public float getAnimationProgress(float partialTicks) {
        if (!this.clientSideAttackStarted) {
            return 0.0F;
        } else {
            int i = this.lifeTicks - 2;
            return i <= 0 ? 1.0F : 1.0F - ((float) i - partialTicks) / 20.0F;
        }
    }
}