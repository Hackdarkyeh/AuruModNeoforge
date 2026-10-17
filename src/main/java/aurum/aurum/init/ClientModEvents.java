package aurum.aurum.init;

import aurum.aurum.Aurum;
import aurum.aurum.client.AurumDimensionEffects;
import aurum.aurum.client.ModKeyBindings.ModKeyBindings;
import aurum.aurum.client.gui.Player.AurumBarOverlay;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.lang.reflect.Field;
import java.util.Map;

@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.TOGGLE_AURUM_BAR);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_bar"),  // ResourceLocation, no un String
                new AurumBarOverlay()
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DimensionSpecialEffects.EFFECTS.put(
                    ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dimension_1"),
                    new AurumDimensionEffects()
            );

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AURUM_VINES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AURUM_VINES_PLANT.get(), RenderType.cutout());
        });
    }
}