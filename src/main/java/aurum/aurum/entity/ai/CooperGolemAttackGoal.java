package aurum.aurum.entity.ai;

import aurum.aurum.entity.CooperGolemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class CooperGolemAttackGoal extends MeleeAttackGoal {
    private final CooperGolemEntity cooperGolem;
    private int attackDelay = 4;
    private int ticksUntilNextAttack = 12;
    private boolean shouldCountTillNextAttack = false;


    public CooperGolemAttackGoal(PathfinderMob pMob, double p_25553_, boolean p_25554_) {
        super(pMob, p_25553_, p_25554_);
        cooperGolem = (CooperGolemEntity) pMob;
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
            shouldCountTillNextAttack = true;

            if(isTimeToStartAttackAnimation()) {
                cooperGolem.setAttacking(true);
            }

            if(isTimeToAttack()) {
                this.mob.getLookControl().setLookAt(pEnemy.getX(), pEnemy.getEyeY(), pEnemy.getZ());
                performAttack(pEnemy);
            }
        } else {
            resetAttackCooldown();
            shouldCountTillNextAttack = false;
            cooperGolem.setAttacking(false);
            cooperGolem.attackAnimationTimeOut = 0;
        }
    }

    private boolean isEnemyWithinAttackDistance(LivingEntity pEnemy, double pDistToEnemySpr){
        return pDistToEnemySpr <= this.getAttackReachSqr(pEnemy);
    }

    private double getAttackReachSqr(LivingEntity target) {
        // Ancho del mob multiplicado por 2 para obtener un rango adecuado para el ataque.
        return this.mob.getBbWidth() * this.mob.getBbWidth() * 4.0F + target.getBbWidth();
    }

    protected  void resetAttackCooldown(){
        this.ticksUntilNextAttack = this.adjustedTickDelay(attackDelay * 2);
    }

    protected  boolean isTimeToAttack(){
        return this.ticksUntilNextAttack <= 0;
    }

    protected boolean isTimeToStartAttackAnimation(){
        return this.ticksUntilNextAttack <= attackDelay;
    }

    protected int getTicksUntilNextAttack(){
        return this.ticksUntilNextAttack;
    }

    protected void performAttack(LivingEntity pEnemy) {
        this.resetAttackCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.doHurtTarget(pEnemy);
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldCountTillNextAttack) {
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        }
    }

    @Override
    public void stop() {
        super.stop();
        cooperGolem.setAttacking(false);
    }
}
