package aurum.aurum.block.engineering;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PipePlacer {
    // Método que inicia la colocación de bloques hacia abajo
    public static boolean placePipesDownwards(Level level, BlockPos startPos, BlockState pipeBlockState) {
        // Verifica si el nivel es del tipo ServerLevel para asegurarse de que esto solo se ejecute en el servidor
        if (level instanceof ServerLevel serverLevel) {
            BlockPos currentPos = startPos.below(); // Comienza un bloque por debajo del inicial

            // Recorre hacia abajo hasta encontrar un bloque de mineral, un bloque sólido o el límite del mundo
            while (serverLevel.getBlockState(currentPos).isAir() && currentPos.getY() > level.getMinBuildHeight()) {
                // Comprueba si el bloque de abajo es un mineral
                if (isMineralBlock(serverLevel, currentPos.below())) {
                    return true; // Mineral encontrado, detén la colocación de tuberías y extrae el mineral
                }

                // Coloca el bloque de la tubería
                serverLevel.setBlockAndUpdate(currentPos, pipeBlockState);

                // Mueve la posición hacia abajo
                currentPos = currentPos.below();
            }

            // Si encontró un bloque sólido que no es aire ni mineral
            return serverLevel.getBlockState(currentPos).isAir() || isMineralBlock(serverLevel, currentPos); // Detén la máquina ya que hay un obstáculo sólido
// Tubería colocada sin obstáculos
        }
        return false;
    }

    // Método auxiliar para verificar si un bloque es un mineral (puedes expandir esta lógica)
    private static boolean isMineralBlock(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        // Aquí solo comprueba si es un bloque de diamante, puedes añadir más minerales si lo deseas
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE;
    }
}

