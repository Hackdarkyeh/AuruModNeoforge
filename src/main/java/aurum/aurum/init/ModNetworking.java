package aurum.aurum.init;

import aurum.aurum.Red.SyncAurumExposurePayload;
import aurum.aurum.Red.SyncAurumSkyPayload;
import aurum.aurum.Red.VillagerActionPayload;
import aurum.aurum.Red.VillagerMenuStatePayload;
import aurum.aurum.client.AurumSkyClient;
import aurum.aurum.client.AurumVillagerMenuClient;
import aurum.aurum.client.gui.Player.*;
import aurum.aurum.Red.SyncAurumBloodPayload;
import aurum.aurum.Red.SyncSoulContainerPayload;
import aurum.aurum.entity.AurumVillager.AurumContractService;
import net.minecraft.server.level.ServerPlayer;
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

        registrar.playToClient(
                SyncAurumSkyPayload.TYPE,
                SyncAurumSkyPayload.CODEC,
                ModNetworking::handleSkyOnClient
        );

        // Exposición a Aurum del jugador
        registrar.playToClient(
                SyncAurumExposurePayload.TYPE,
                SyncAurumExposurePayload.CODEC,
                ModNetworking::handleExposureOnClient
        );

        // Menú de interacción de aldeanos contratables
        registrar.playToClient(
                VillagerMenuStatePayload.TYPE,
                VillagerMenuStatePayload.CODEC,
                ModNetworking::handleVillagerMenuOnClient
        );
        registrar.playToServer(
                VillagerActionPayload.TYPE,
                VillagerActionPayload.CODEC,
                ModNetworking::handleVillagerActionOnServer
        );

        registrar.playToClient(ClientboundSetPassivesPacket.TYPE, ClientboundSetPassivesPacket.STREAM_CODEC,
                ClientboundSetPassivesPacket::handle);

        registrar.playToClient(ClientboundLuminescencePacket.TYPE, ClientboundLuminescencePacket.STREAM_CODEC,
                ClientboundLuminescencePacket::handle);

    }

    private static void handleSkyOnClient(final SyncAurumSkyPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> AurumSkyClient.update(payload, context.player().level().getGameTime()));
    }

    private static void handleExposureOnClient(final SyncAurumExposurePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                player.setData(ModAttachments.AURUM_EXPOSURE, payload.value());
            }
        });
    }

    private static void handleVillagerMenuOnClient(final VillagerMenuStatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> AurumVillagerMenuClient.handle(payload));
    }

    private static void handleVillagerActionOnServer(final VillagerActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                AurumContractService.handleAction(serverPlayer, payload);
            }
        });
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
