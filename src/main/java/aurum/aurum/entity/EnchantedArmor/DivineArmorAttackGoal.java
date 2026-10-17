package aurum.aurum.entity.EnchantedArmor;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class DivineArmorAttackGoal extends MeleeAttackGoal {
    private final DivineArmorEntity armorEntity;
    private int attackDelay = 4;
    private int ticksUntilNextAttack = 12;

    public DivineArmorAttackGoal(PathfinderMob pMob, double speed, boolean followingTargetEvenIfNotSeen) {
        super(pMob, speed, followingTargetEvenIfNotSeen);
        this.armorEntity = (DivineArmorEntity) pMob;
    }

    @Override
    public void start() {
        super.start();
        attackDelay = 4;
        ticksUntilNextAttack = 12;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy) {
        super.checkAndPerformAttack(pEnemy);
        if (isEnemyWithinAttackDistance(pEnemy, this.mob.distanceToSqr(pEnemy))) {
            if (this.ticksUntilNextAttack <= 0) {
                this.mob.getLookControl().setLookAt(pEnemy.getX(), pEnemy.getEyeY(), pEnemy.getZ());
                this.resetAttackCooldown();
                this.mob.swing(InteractionHand.MAIN_HAND);
                this.mob.doHurtTarget(pEnemy);
            }
        }
    }

    private boolean isEnemyWithinAttackDistance(LivingEntity pEnemy, double pDistToEnemySpr) {
        return pDistToEnemySpr <= this.getAttackReachSqr(pEnemy);
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(attackDelay * 2);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
    }

    // Devuelve el radio de alcance del ataque basado en el tamaño de la entidad
    protected double getAttackReachSqr(LivingEntity target) {
        return this.mob.getBbWidth() * this.mob.getBbWidth() * 4.0F + target.getBbWidth();
    }
}