package aurum.aurum.eventHandler;

import aurum.aurum.init.ModBlocks;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class InoculatedObsidianBreakHandler {

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        // Comprobamos si el bloque que se intenta romper es nuestra obsidiana inoculada
        if (event.getTargetBlock().is(ModBlocks.INOCULATED_OBSIDIAN.get())) {
            // Verificamos que el jugador se encuentre estrictamente en The End
            if (event.getEntity().level().dimension() == Level.END) {
                // Forzamos que se considere herramienta válida (permitiendo pico de hierro o superior)
                event.setCanHarvest(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // Hacemos que se rompa mucho más rápido exclusivamente en el End
        if (event.getState().is(ModBlocks.INOCULATED_OBSIDIAN.get())) {
            if (event.getEntity().level().dimension() == Level.END) {
                // Usamos el método setNewSpeed en lugar de modificar el campo privado directamente
                float boostedSpeed = event.getOriginalSpeed() * 4.0F;
                event.setNewSpeed(boostedSpeed);
            }
        }
    }
}