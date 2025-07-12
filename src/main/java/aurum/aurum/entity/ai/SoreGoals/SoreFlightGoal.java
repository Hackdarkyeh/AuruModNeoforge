package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SoreFlightGoal extends Goal {
    private final SoreBossEntity boss;
    private boolean isFlying = false;
    private int flightDuration = 0;

    public SoreFlightGoal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Condiciones para empezar a volar
        return (boss.getHealth() < boss.getMaxHealth() * 0.5 && boss.getRandom().nextFloat() < 0.1f) ||
                (boss.getTarget() != null && boss.distanceTo(boss.getTarget()) > 12);
    }

    @Override
    public void start() {
        isFlying = true;
        flightDuration = 100 + boss.getRandom().nextInt(100);
        boss.setNoGravity(true);
    }

    @Override
    public void tick() {
        if (isFlying) {
            // Movimiento en vuelo
            LivingEntity target = boss.getTarget();
            if (target != null) {
                Vec3 targetPos = target.position().add(0, 3, 0); // Volar sobre el objetivo
                Vec3 moveDir = targetPos.subtract(boss.position()).normalize();
                boss.setDeltaMovement(moveDir.scale(0.3));
            }

            // Ataques aéreos
            if (flightDuration-- <= 0 || boss.getHealth() > boss.getMaxHealth() * 0.8) {
                stopFlying();
            }
        }
    }

    private void stopFlying() {
        isFlying = false;
        boss.setNoGravity(false);
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(0.5, 0, 0.5));
    }

    @Override
    public boolean canContinueToUse() {
        return isFlying;
    }
}