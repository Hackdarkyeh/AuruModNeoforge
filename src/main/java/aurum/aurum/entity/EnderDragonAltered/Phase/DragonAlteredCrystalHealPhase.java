package aurum.aurum.entity.EnderDragonAltered.Phase;

import aurum.aurum.entity.EnderDragonAltered.AbstractDragonAlteredPhaseInstance;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAltered;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAlteredPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * El dragón, herido, vuela en círculos junto a un End Crystal para curarse (la curación en sí
 * la hace EnderDragonAltered.checkCrystals cuando hay un cristal a menos de 32 bloques).
 *
 * Órbita siempre por el lado de DENTRO del cristal (hacia el centro de la arena), así nunca
 * se acerca a la barrera. Los jugadores pueden destruir el cristal mientras tanto.
 */
public class DragonAlteredCrystalHealPhase extends AbstractDragonAlteredPhaseInstance {
    private static final int MAX_TICKS = 900;              // 45 s como máximo curándose
    private static final float STOP_HEALTH_FRACTION = 0.85F;
    private static final int COOLDOWN_AFTER_HEAL = 600;    // 30 s sin volver a curarse
    private static final int COOLDOWN_AFTER_ATTACK = 300;
    private static final double ORBIT_RADIUS = 9.0D;
    private static final double INWARD_OFFSET = 13.0D;     // desplazamiento del centro de la órbita hacia el centro de la arena
    private static final double HEIGHT_OFFSET = 10.0D;

    @Nullable
    private Vec3 targetLocation;
    @Nullable
    private EndCrystal crystal;
    private double angle;
    private int ticks;

    public DragonAlteredCrystalHealPhase(EnderDragonAltered dragon) {
        super(dragon);
    }

    @Override
    public void begin() {
        this.ticks = 0;
        this.targetLocation = null;
        this.crystal = null;
        this.angle = this.dragon.getRandom().nextDouble() * Math.PI * 2.0D;
    }

    @Override
    public void doServerTick() {
        this.ticks++;

        if (this.crystal == null || !this.crystal.isAlive()) {
            this.crystal = this.dragon.findNearestAliveCrystal();
            this.targetLocation = null;
        }

        boolean healed = this.dragon.getHealth() >= this.dragon.getMaxHealth() * STOP_HEALTH_FRACTION;
        if (this.crystal == null || healed || this.ticks > MAX_TICKS) {
            this.dragon.setHealCooldown(COOLDOWN_AFTER_HEAL);
            this.dragon.getPhaseManager().setPhase(EnderDragonAlteredPhase.HOLDING_PATTERN);
            return;
        }

        if (this.targetLocation == null || this.targetLocation.distanceToSqr(this.dragon.position()) < 36.0D) {
            this.angle += 0.9D;
            this.updateTarget();
        }
    }

    private void updateTarget() {
        Vec3 c = this.crystal.position();
        BlockPos o = this.dragon.getFightOrigin();
        Vec3 inward = new Vec3(o.getX() + 0.5D - c.x, 0.0D, o.getZ() + 0.5D - c.z);
        inward = inward.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : inward.normalize();

        Vec3 center = c.add(inward.scale(INWARD_OFFSET)).add(0.0D, HEIGHT_OFFSET, 0.0D);
        this.targetLocation = center.add(Math.cos(this.angle) * ORBIT_RADIUS, 0.0D, Math.sin(this.angle) * ORBIT_RADIUS);
    }

    @Override
    public void onCrystalDestroyed(EndCrystal destroyed, BlockPos pos, DamageSource damageSource, @Nullable Player player) {
        if (destroyed == this.crystal) {
            this.crystal = null;
            this.targetLocation = null;
        }
        // Quien destruye un cristal se gana un ataque
        if (player != null && this.dragon.canAttack(player)) {
            this.dragon.setHealCooldown(COOLDOWN_AFTER_ATTACK);
            this.dragon.getPhaseManager().setPhase(EnderDragonAlteredPhase.STRAFE_PLAYER);
            this.dragon.getPhaseManager().getPhase(EnderDragonAlteredPhase.STRAFE_PLAYER).setTarget(player);
        }
    }

    @Override
    public float getFlySpeed() {
        return 1.0F;
    }

    @Nullable
    @Override
    public Vec3 getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public EnderDragonAlteredPhase<DragonAlteredCrystalHealPhase> getPhase() {
        return EnderDragonAlteredPhase.CRYSTAL_HEAL;
    }
}