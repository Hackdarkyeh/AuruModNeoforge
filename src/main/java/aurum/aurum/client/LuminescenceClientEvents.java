package aurum.aurum.client;

import aurum.aurum.Aurum;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = Aurum.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class LuminescenceClientEvents {

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LuminescenceState.clear();
    }
}