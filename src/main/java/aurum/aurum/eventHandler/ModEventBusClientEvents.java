package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.client.model.AAModModelLayers;
import aurum.aurum.client.model.ModelCooperGolem;
import aurum.aurum.client.model.SoreModel;
import aurum.aurum.client.renderer.EnderDragonAlteredRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@EventBusSubscriber(modid= Aurum.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AAModModelLayers.COOPER_GOLEM_LAYER, ModelCooperGolem::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.SORE_LAYER, SoreModel::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.ZOMBIE_ALTERED_LAYER, aurum.aurum.client.model.ZombieAlteredModel::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.SPIDER_CORRUPTED_LAYER, aurum.aurum.client.model.SpiderCorruptedModel::createSpiderBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.SKELETON_ALTERED_LAYER, aurum.aurum.client.model.SkeletonAlteredModel::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.DOLPHIN_ALTERED_LAYER, aurum.aurum.client.model.DolphinAlteredModel::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.DRAGON_ALTERED_LAYER, EnderDragonAlteredRenderer::createBodyLayer);
        event.registerLayerDefinition(AAModModelLayers.VEX_ALTERED_LAYER, aurum.aurum.client.model.VexAlteredModel::createBodyLayer);
    }
}