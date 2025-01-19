package aurum.aurum.eventHandler;

import aurum.aurum.effectsPlayer.AurumBlight;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
public class BlockChecker {


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Asegúrate de que este es el tick final, y que estamos en el lado del servidor
            Player player = event.getEntity();

            BlockState blockState = player.getBlockStateOn();
            Block block = blockState.getBlock();
        if (block.getDescriptionId().equals("block.aurum.withered_grass_block")) {
            // Realiza cualquier acción cuando el jugador esté sobre el bloque específico
            AurumBlight.getEffectTier1(player);
        }
    }
}
