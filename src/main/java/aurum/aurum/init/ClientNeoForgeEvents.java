package aurum.aurum.init;

import aurum.aurum.Aurum;
import aurum.aurum.client.ModKeyBindings.ModKeyBindings;
import aurum.aurum.client.gui.Player.AurumBarOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Aurum.MODID, value = Dist.CLIENT)
public class ClientNeoForgeEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Comprobamos si nuestra tecla fue pulsada
        if (ModKeyBindings.TOGGLE_AURUM_BAR.consumeClick()) {
            AurumBarOverlay.visible = !AurumBarOverlay.visible;
            //mc.player.displayClientMessage(
            //        Component.literal("Aurum Bar: " + (AurumBarOverlay.visible ? "ON" : "OFF")), true);
        }
    }
}
