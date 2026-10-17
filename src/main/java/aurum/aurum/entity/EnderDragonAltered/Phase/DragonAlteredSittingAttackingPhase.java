package aurum.aurum.entity.EnderDragonAltered.Phase;

import aurum.aurum.entity.EnderDragonAltered.EnderDragonAltered;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAlteredPhase;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;


public class DragonAlteredSittingAttackingPhase extends AbstractDragonAlteredSittingPhase {
    private static final int ROAR_DURATION = 40;
    private int attackingTicks;

    public DragonAlteredSittingAttackingPhase(EnderDragonAltered dragon) {
        super(dragon);
    }

    @Override
    public void doClientTick() {
        this.dragon
                .level()
                .playLocalSound(
                        this.dragon.getX(),
                        this.dragon.getY(),
                        this.dragon.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL,
                        this.dragon.getSoundSource(),
                        2.5F,
                        0.8F + this.dragon.getRandom().nextFloat() * 0.3F,
                        false
                );
    }

    @Override
    public void doServerTick() {
        if (this.attackingTicks++ >= 40) {
            this.dragon.getPhaseManager().setPhase(EnderDragonAlteredPhase.SITTING_FLAMING);
        }
    }

    @Override
    public void begin() {
        this.attackingTicks = 0;
    }

    @Override
    public EnderDragonAlteredPhase<DragonAlteredSittingAttackingPhase> getPhase() {
        return EnderDragonAlteredPhase.SITTING_ATTACKING;
    }
}

