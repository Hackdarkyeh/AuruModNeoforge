package aurum.aurum.init;

import aurum.aurum.eventHandler.AurumExposureHandler;
import aurum.aurum.item.Battery.BatteryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModItemProperties {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.BATTERY_ITEM.get(), // ajusta al nombre real de tu batería
                    ResourceLocation.fromNamespaceAndPath("aurum", "battery_state"),
                    (stack, level, entity, seed) -> BatteryItem.getVisualState(stack));

            // Aguja del Geiger: exposición acumulada del jugador, de 0.0 a 1.0
            ItemProperties.register(
                    ModItems.GEIGER_COUNTER.get(),
                    ResourceLocation.fromNamespaceAndPath("aurum", "exposure_level"),
                    (stack, level, entity, seed) -> {
                        Player p = entity instanceof Player pl ? pl : Minecraft.getInstance().player;
                        if (p == null) {
                            return 0.0F;
                        }
                        float exposure = p.getData(ModAttachments.AURUM_EXPOSURE);
                        return Math.max(0.0F, Math.min(1.0F, exposure / AurumExposureHandler.MAX_EXPOSURE));
                    });
        });
    }
}