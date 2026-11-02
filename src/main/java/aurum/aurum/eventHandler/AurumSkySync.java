package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.Red.SyncAurumSkyPayload;
import aurum.aurum.worldgen.Dimension.AurumSkyData;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Manda a cada jugador, una vez por segundo, el estado de la noche del nivel en el que está.
 * Así el cliente siempre está al día (incluido tras entrar al mundo, cambiar de dimensión o usar
 * /Aurum sky set) sin necesidad de eventos de login ni de cambio de dimensión.
 * El paquete es muy pequeño: 2 booleanos y 3 enteros.
 */
@EventBusSubscriber(modid = Aurum.MODID)
public class AurumSkySync {

    public static final int SYNC_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(build(player.serverLevel()));
        }
    }

    public static SyncAurumSkyPayload build(ServerLevel level) {
        boolean raining = AurumBlightRain.isRainingIn(level);
        if (level.dimension() == AurumWeatherEvents.AURUM_DIMENSION_KEY) {
            AurumSkyData data = AurumSkyData.get(level);
            int next = data.next() == null ? -1 : data.next().ordinal();
            return new SyncAurumSkyPayload(true, data.current().ordinal(), next, data.ticksRemaining(), raining);
        }
        return new SyncAurumSkyPayload(false, 0, -1, 0, raining);
    }
}