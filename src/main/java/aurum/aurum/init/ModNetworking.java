package aurum.aurum.init;

import aurum.aurum.client.gui.Player.OpenArmorMenuPacket;
import aurum.aurum.client.gui.Player.OpenSoulMenuPacket;
import aurum.aurum.Red.SyncAurumBloodPayload;
import aurum.aurum.Red.SyncSoulContainerPayload;
import aurum.aurum.client.gui.Player.ServerboundSetPassivePacket;
import aurum.aurum.client.gui.Player.ServerboundUpgradeSkillPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1"); // Versión del protocolo

        registrar.playToServer(
                OpenSoulMenuPacket.TYPE,
                OpenSoulMenuPacket.STREAM_CODEC,
                OpenSoulMenuPacket::handle
        );
        registrar.playToServer(
                OpenArmorMenuPacket.TYPE,
                OpenArmorMenuPacket.STREAM_CODEC,
                OpenArmorMenuPacket::handle
        );
        // Registramos el paquete hacia el cliente
        registrar.playToClient(
                SyncAurumBloodPayload.TYPE,
                SyncAurumBloodPayload.CODEC,
                aurum.aurum.init.ModNetworking::handleDataOnClient
        );

        // NÚEVO: Registrar el envío del contenedor de alma hacia el cliente
        registrar.playToClient(
                SyncSoulContainerPayload.TYPE,
                SyncSoulContainerPayload.CODEC,
                ModNetworking::handleSoulOnClient
        );

        registrar.playToServer(
                ServerboundUpgradeSkillPacket.TYPE,
                ServerboundUpgradeSkillPacket.STREAM_CODEC,
                ServerboundUpgradeSkillPacket::handle
        );

        registrar.playToServer(
                ServerboundSetPassivePacket.TYPE,
                ServerboundSetPassivePacket.STREAM_CODEC,
                ServerboundSetPassivePacket::handle
        );


    }

    // NUEVO: El cliente recibe el alma y la guarda en su propia instancia del jugador
    private static void handleSoulOnClient(final SyncSoulContainerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                player.setData(ModAttachments.ACTIVE_SOUL, payload.container());
            }
        });
    }

    // Cambiado CustomPayloadEvent.Context por IPayloadContext
    private static void handleDataOnClient(final SyncAurumBloodPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // El contexto moderno nos da acceso directo al jugador en el cliente
            var player = context.player();
            if (player != null) {
                player.setData(ModAttachments.AURUM_BLOOD, payload.value());
            }
        });
    }
}
