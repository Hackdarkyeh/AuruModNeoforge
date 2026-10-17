package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.Red.SyncAurumBloodPayload;
import aurum.aurum.Red.SyncSoulContainerPayload;
import aurum.aurum.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerEvents {

    // 1. Al entrar al mundo: Carga el progreso guardado del disco y lo manda al cliente
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBloodData(player);
        }
    }

    // 2. Al cambiar de dimensión: Mantiene al cliente sincronizado
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBloodData(player);
        }
    }

    // 3. Al reaparecer (Morir): Como NO usamos copyOnDeath, el server ya lo habrá reseteado a 0.
    // Usamos este evento para avisarle al cliente que ahora su valor es 0.
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBloodData(player);
        }
    }

    // Método auxiliar para enviar el paquete
    private static void syncBloodData(ServerPlayer player) {
        int currentBlood = player.getData(ModAttachments.AURUM_BLOOD);
        player.connection.send(new SyncAurumBloodPayload(currentBlood));

        // ¡NUEVO! Sincroniza también el alma para que el HUD pueda calcular el nuevo máximo
        var soulContainer = player.getData(ModAttachments.ACTIVE_SOUL);
        player.connection.send(new SyncSoulContainerPayload(soulContainer));
    }
}