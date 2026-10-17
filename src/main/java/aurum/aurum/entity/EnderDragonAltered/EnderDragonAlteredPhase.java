package aurum.aurum.entity.EnderDragonAltered;

import aurum.aurum.entity.EnderDragonAltered.Phase.*;
import net.minecraft.world.entity.boss.enderdragon.phases.*;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class EnderDragonAlteredPhase<T extends DragonAlteredPhaseInstance> {
    private static EnderDragonAlteredPhase<?>[] phases = new EnderDragonAlteredPhase[0];
    public static final EnderDragonAlteredPhase<DragonAlteredHoldingPatternPhase> HOLDING_PATTERN = create(DragonAlteredHoldingPatternPhase.class, "HoldingPattern");
    public static final EnderDragonAlteredPhase<DragonAlteredStrafePlayerPhase> STRAFE_PLAYER = create(DragonAlteredStrafePlayerPhase.class, "StrafePlayer");
    public static final EnderDragonAlteredPhase<DragonAlteredLandingApproachPhase> LANDING_APPROACH = create(DragonAlteredLandingApproachPhase.class, "LandingApproach");
    public static final EnderDragonAlteredPhase<DragonAlteredLandingPhase> LANDING = create(DragonAlteredLandingPhase.class, "Landing");
    public static final EnderDragonAlteredPhase<DragonAlteredTakeoffPhase> TAKEOFF = create(DragonAlteredTakeoffPhase.class, "Takeoff");
    public static final EnderDragonAlteredPhase<DragonAlteredSittingFlamingPhase> SITTING_FLAMING = create(DragonAlteredSittingFlamingPhase.class, "SittingFlaming");
    public static final EnderDragonAlteredPhase<DragonAlteredSittingScanningPhase> SITTING_SCANNING = create(DragonAlteredSittingScanningPhase.class, "SittingScanning");
    public static final EnderDragonAlteredPhase<DragonAlteredSittingAttackingPhase> SITTING_ATTACKING = create(DragonAlteredSittingAttackingPhase.class, "SittingAttacking");
    public static final EnderDragonAlteredPhase<DragonAlteredChargePlayerPhase> CHARGING_PLAYER = create(DragonAlteredChargePlayerPhase.class, "ChargingPlayer");
    public static final EnderDragonAlteredPhase<DragonAlteredDeathPhase> DYING = create(DragonAlteredDeathPhase.class, "Dying");
    public static final EnderDragonAlteredPhase<DragonAlteredHoverPhase> HOVERING = create(DragonAlteredHoverPhase.class, "Hover");
    private final Class<? extends DragonAlteredPhaseInstance> instanceClass;
    private final int id;
    private final String name;

    private EnderDragonAlteredPhase(int id, Class<? extends DragonAlteredPhaseInstance> instanceClass, String name) {
        this.id = id;
        this.instanceClass = instanceClass;
        this.name = name;
    }

    public DragonAlteredPhaseInstance createInstance(EnderDragonAltered dragon) {
        try {
            Constructor<? extends DragonAlteredPhaseInstance> constructor = this.getConstructor();
            return constructor.newInstance(dragon);
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    protected Constructor<? extends DragonAlteredPhaseInstance> getConstructor() throws NoSuchMethodException {
        return this.instanceClass.getConstructor(EnderDragonAltered.class);
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.name + " (#" + this.id + ")";
    }

    /**
     * Gets a phase by its ID. If the phase is out of bounds (negative or beyond the end of the phase array), returns {@link #HOLDING_PATTERN}.
     */
    public static EnderDragonAlteredPhase<?> getById(int id) {
        return id >= 0 && id < phases.length ? phases[id] : HOLDING_PATTERN;
    }

    public static int getCount() {
        return phases.length;
    }

    private static <T extends DragonAlteredPhaseInstance> EnderDragonAlteredPhase<T> create(Class<T> phase, String name) {
        EnderDragonAlteredPhase<T> enderdragonphase = new EnderDragonAlteredPhase<>(phases.length, phase, name);
        phases = Arrays.copyOf(phases, phases.length + 1);
        phases[enderdragonphase.getId()] = enderdragonphase;
        return enderdragonphase;
    }
}
