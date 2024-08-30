package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.client.model.ModModelLayers;
import aurum.aurum.client.model.ModelCooperGolem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@EventBusSubscriber(modid= Aurum.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.COOPER_GOLEM_LAYER, ModelCooperGolem::createBodyLayer);
    }
}
