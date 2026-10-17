package aurum.aurum.eventHandler;

import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModItems; // Asegúrate de importar tu ítem o bloque correspondiente
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class EndBlockBreakHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.END) {
            BlockPos pos = event.getPos();

            // Verificamos si el bloque que se está rompiendo es nuestra obsidiana inoculada en la zona de la fuente del End (ej: X=0, Z=0)
            if (pos.getX() == 0 && pos.getZ() == 0 && serverLevel.getBlockState(pos).is(ModBlocks.INOCULATED_OBSIDIAN.get())) {

                // 1. Dar el ítem a TODOS los jugadores conectados en el servidor
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    // Creamos el ItemStack que se les entregará (cambia ModItems.TU_ITEM por el que desees repartir)
                    ItemStack rewardItem = new ItemStack(ModBlocks.INOCULATED_OBSIDIAN.get()); // O un ítem especial de recompensa

                    boolean added = player.getInventory().add(rewardItem);
                    if (!added) {
                        // Si el inventario está lleno, lo soltamos en los pies del jugador
                        player.drop(rewardItem, false);
                    }
                }

                // Mensaje global opcional en el chat
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal("¡El núcleo del portal ha sido reclamado! Todos los jugadores han recibido la reliquia."),
                        false
                );
            }
        }
    }
}