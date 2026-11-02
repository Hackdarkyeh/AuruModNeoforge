package aurum.aurum.entity.BlazeCorrupted;

import aurum.aurum.init.ModEntities;
import aurum.aurum.init.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CorruptedFireball extends Fireball {
    private static final float EXPLOSION_RADIUS = 2.0F;

    public CorruptedFireball(EntityType<? extends CorruptedFireball> type, Level level) {
        super(type, level);
    }

    public CorruptedFireball(Level level, LivingEntity shooter, Vec3 movement) {
        super(ModEntities.CORRUPTED_FIREBALL.get(), shooter, movement, level);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.CORRUPTED_FIRE_CHARGE.get());
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) return;

        Entity owner = this.getOwner();
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return entity != owner;   // no daña al blaze que la lanzó
            }
        };
        this.level().explode(this, null, calculator,
                getX(), getY(), getZ(), EXPLOSION_RADIUS,
                false, Level.ExplosionInteraction.MOB);
        this.discard();
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override protected boolean shouldBurn() { return false; }               // sin llama naranja encima
    @Override protected ParticleOptions getTrailParticle() { return ParticleTypes.WITCH; } // estela morada
}