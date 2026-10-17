package aurum.aurum.entity.EnderDragonAltered.Phase;

import aurum.aurum.entity.EnderDragonAltered.AbstractDragonAlteredPhaseInstance;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAltered;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAlteredPhase;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;


public class DragonAlteredHoverPhase extends AbstractDragonAlteredPhaseInstance {
    @Nullable
    private Vec3 targetLocation;

    public DragonAlteredHoverPhase(EnderDragonAltered dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick() {
        if (this.targetLocation == null) {
            this.targetLocation = this.dragon.position();
        }
    }

    @Override
    public boolean isSitting() {
        return true;
    }

    @Override
    public void begin() {
        this.targetLocation = null;
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
    public EnderDragonAlteredPhase<DragonAlteredHoverPhase> getPhase() {
        return EnderDragonAlteredPhase.HOVERING;
    }
}
