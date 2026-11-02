package aurum.aurum.block.engineering.PortalAurum;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Registro de las dimensiones cuya noche está bloqueada por una raid del portal.
 * Mientras una dimensión esté aquí, las camas no funcionan.
 *
 * Lo rellena AbstractInoculatedObsidianBlockEntity (lock/unlock).
 */
@EventBusSubscriber(modid = "aurum")
public class RaidNightLock {

    private static final Set<ResourceKey<Level>> LOCKED = Collections.synchronizedSet(new HashSet<>());

    public static void lock(ResourceKey<Level> dimension) {
        LOCKED.add(dimension);
    }

    public static void unlock(ResourceKey<Level> dimension) {
        LOCKED.remove(dimension);
    }

    public static boolean isLocked(ResourceKey<Level> dimension) {
        return LOCKED.contains(dimension);
    }

    @SubscribeEvent
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        if (isLocked(player.level().dimension())) {
            event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.aurum.raid_no_sleep"), true);
            }
        }
    }
}