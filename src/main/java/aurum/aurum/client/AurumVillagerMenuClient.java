package aurum.aurum.client;

import aurum.aurum.Red.VillagerMenuStatePayload;
import net.minecraft.client.Minecraft;

/** Recibe el estado del menú de un aldeano: abre la pantalla o refresca la que ya está abierta (solo cliente). */
public final class AurumVillagerMenuClient {

    private AurumVillagerMenuClient() {}

    public static void handle(VillagerMenuStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof VillagerInteractionScreen screen && screen.entityId() == payload.entityId()) {
            screen.update(payload);
        } else if (payload.open()) {
            mc.setScreen(new VillagerInteractionScreen(payload));
        }
    }
}
