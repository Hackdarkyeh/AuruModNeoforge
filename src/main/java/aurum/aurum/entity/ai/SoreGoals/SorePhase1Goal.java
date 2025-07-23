package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SorePhase1Goal extends Goal {
    private final SoreBossEntity boss;
    private int teleportCooldown = 0;
    private int tntCooldown = 0;

    public SorePhase1Goal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return boss.getCurrentPhase() == 1;
    }

    // En SorePhase1Goal.java
    @Override
    public void tick() {
        super.tick();
        LivingEntity target = boss.getTarget();

        if (target != null && target.isAlive()) {
            // Teletransporte defensivo
            if (teleportCooldown-- <= 0 && (boss.getHealth() < boss.getMaxHealth() * 0.2 || boss.hurtTime > 0)) { // Más flexible
                teleportAwayFromDanger();
                teleportCooldown = 100 + boss.getRandom().nextInt(50);
            }

            // Ataque con TNT: Más propenso si está rodeado o el target está cerca.
            if (tntCooldown-- <= 0 && boss.getRandom().nextFloat() < 0.2f) { // Menor probabilidad, pero siempre posible
                // Si el jugador está cerca o hay múltiples entidades cerca
                if (boss.distanceTo(target) < 5 || boss.level().getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(5), e -> e != boss && e.isAttackable()).size() > 2) {
                    launchTNT();
                    tntCooldown = 150 + boss.getRandom().nextInt(50);
                }
            }
        }
    }

    private void teleportAwayFromDanger() {
        LivingEntity target = boss.getTarget();
        if (target != null) {
            Vec3 awayDir = boss.position().subtract(target.position()).normalize();
            Vec3 teleportPos = boss.position().add(awayDir.scale(10 + boss.getRandom().nextInt(5)));
            boss.randomTeleport(teleportPos.x, teleportPos.y, teleportPos.z, true);
        }
    }

    private void launchTNT() {
        for (int i = 0; i < 3; i++) {
            PrimedTnt tnt = new PrimedTnt(boss.level(),
                    boss.getX(),
                    boss.getY() + 2,
                    boss.getZ(),
                    null);
            tnt.setFuse(40);
            Vec3 targetPos = boss.getTarget() != null ?
                    boss.getTarget().position() :
                    boss.position().add(boss.getLookAngle().scale(10));
            tnt.setDeltaMovement(targetPos.subtract(tnt.position()).normalize().scale(0.5));
            boss.level().addFreshEntity(tnt);
        }
    }
}