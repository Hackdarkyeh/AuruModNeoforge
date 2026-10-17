package aurum.aurum.entity.EnderDragonAltered;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class EnderDragonAlteredPhaseManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final EnderDragonAltered dragon;
    private final DragonAlteredPhaseInstance[] phases = new DragonAlteredPhaseInstance[EnderDragonAlteredPhase.getCount()];
    @Nullable
    private DragonAlteredPhaseInstance currentPhase;

    public EnderDragonAlteredPhaseManager(EnderDragonAltered dragon) {
        this.dragon = dragon;
        this.setPhase(EnderDragonAlteredPhase.HOVERING);
    }

    public void setPhase(EnderDragonAlteredPhase<?> phase) {
        if (this.currentPhase == null || phase != this.currentPhase.getPhase()) {
            if (this.currentPhase != null) {
                this.currentPhase.end();
            }

            this.currentPhase = this.getPhase((EnderDragonAlteredPhase<DragonAlteredPhaseInstance>)phase);
            if (!this.dragon.level().isClientSide) {
                this.dragon.getEntityData().set(EnderDragon.DATA_PHASE, phase.getId());
            }

            LOGGER.debug("Dragon is now in phase {} on the {}", phase, this.dragon.level().isClientSide ? "client" : "server");
            this.currentPhase.begin();
        }
    }

    public DragonAlteredPhaseInstance getCurrentPhase() {
        return this.currentPhase;
    }

    public <T extends DragonAlteredPhaseInstance> T getPhase(EnderDragonAlteredPhase<T> phase) {
        int i = phase.getId();
        if (this.phases[i] == null) {
            this.phases[i] = phase.createInstance(this.dragon);
        }

        return (T)this.phases[i];
    }
}
