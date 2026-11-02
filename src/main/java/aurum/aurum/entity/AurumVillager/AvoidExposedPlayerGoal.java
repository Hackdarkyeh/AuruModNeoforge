package aurum.aurum.entity.AurumVillager;

import aurum.aurum.eventHandler.AurumExposureTier;
import aurum.aurum.init.ModAttachments;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;

/**
 * El aldeano huye de los jugadores muy expuestos a Aurum (como si ya fueran uno de ellos).
 * Solo en forma tranquila: si está hostil, ataca a los jugadores como siempre.
 */
public class AvoidExposedPlayerGoal extends AvoidEntityGoal<Player> {

    /** Tramo de exposición a partir del cual huyen (HIGH = 75 %). */
    private static final AurumExposureTier FLEE_FROM_TIER = AurumExposureTier.HIGH;

    private final AurumVillagerEntity villager;

    public AvoidExposedPlayerGoal(AurumVillagerEntity villager, float maxDistance, double walkSpeed, double sprintSpeed) {
        // [COMPROBAR] firma con dos predicados: (mob, clase, predicadoAEvitar, distancia, vel. andando, vel. corriendo, predicadoExtra)
        super(villager, Player.class,
                target -> target instanceof Player p && isExposed(p),
                maxDistance, walkSpeed, sprintSpeed,
                target -> !(target instanceof Player p) || (!p.isCreative() && !p.isSpectator()));
        this.villager = villager;
    }

    private static boolean isExposed(Player player) {
        float exposure = player.getData(ModAttachments.AURUM_EXPOSURE);
        return AurumExposureTier.fromExposure(exposure).ordinal() >= FLEE_FROM_TIER.ordinal();
    }

    @Override
    public boolean canUse() {
        return !villager.isHostileForm() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !villager.isHostileForm() && super.canContinueToUse();
    }
}