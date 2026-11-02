package aurum.aurum.entity.SkeletonAltered;

import aurum.aurum.effectsPlayer.AurumBlight;
import aurum.aurum.effectsPlayer.AurumBlightDamageSource;
import aurum.aurum.init.ModEntities; // ajusta a tu clase de registro
import aurum.aurum.init.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import javax.annotation.Nullable;

public class AurumArrow extends AbstractArrow {
    private static final float RADIUS = 3.5F;
    private static final float DAMAGE = 8.0F;      // en el centro; baja hacia el borde
    private static final int FUSE_TICKS = 100;     // explota sola si no impacta
    private static final DustParticleOptions PURPLE =
            new DustParticleOptions(new Vector3f(0.6F, 0.1F, 0.9F), 1.5F);

    // Requerido para el registro del EntityType
    public AurumArrow(EntityType<? extends AurumArrow> type, Level level) {
        super(type, level);
    }

    public AurumArrow(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.AURUM_ARROW.get(), owner, level, ammo.copyWithCount(1), weapon);
        this.pickup = AbstractArrow.Pickup.DISALLOWED; // explota al impactar, no hay nada que recoger
    }

    // Para el dispensador
    public AurumArrow(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.AURUM_ARROW.get(), x, y, z, level, ammo.copyWithCount(1), weapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.AURUM_ARROW.get());
    }


    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof AbstractAlteredSkeleton);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (!this.inGround) {
                this.level().addParticle(PURPLE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        } else if (this.tickCount > FUSE_TICKS) {
            this.explode();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        this.explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.explode();
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel server) || this.isRemoved()) return;

        Vec3 c = this.position();
        DamageSource src = AurumBlightDamageSource.createAurumBlightDamageSource(server.registryAccess());

        for (LivingEntity t : server.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(RADIUS))) {
            if (t == this.getOwner() || t instanceof AbstractAlteredSkeleton) continue;
            double dist = t.getBoundingBox().getCenter().distanceTo(c);
            if (dist > RADIUS) continue;

            float dmg = DAMAGE * (1.0F - 0.6F * (float) (dist / RADIUS));
            t.hurt(src, dmg);
            // Opcional: además contagia el efecto Aurum Blight
            AurumBlight.applyCorruptedBlight(t, 1.0D);
        }

        server.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        server.sendParticles(PURPLE, c.x, c.y, c.z, 60, RADIUS / 3, RADIUS / 3, RADIUS / 3, 0.05);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.0F, 1.2F);

        this.discard();
    }
}