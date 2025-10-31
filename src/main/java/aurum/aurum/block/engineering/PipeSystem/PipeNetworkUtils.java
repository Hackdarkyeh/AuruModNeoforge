package aurum.aurum.block.engineering.PipeSystem;

// PipeNetworkUtils.java - Clase reutilizable para ambos tipos de energía

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PipeNetworkUtils {

    public static final float ENERGY_LOSS_PER_PIPE = 0.12f; // 10% de pérdida por tubería

    public interface IEnergyDevice {
        boolean canReceiveEnergy();
        float getDarkEnergyStored();
        float getDarkEnergyCapacity();
        void receiveDarkEnergy(float amount);
    }

    public static <T extends IEnergyDevice> Map<BlockPos, T> findConnectedDevices(Level level, BlockPos startPos,
                                                                                  Class<T> deviceClass,
                                                                                  Class<?> pipeClass) {
        Map<BlockPos, T> connectedDevices = new HashMap<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                if (visited.contains(adjacentPos)) continue;
                visited.add(adjacentPos);

                BlockState adjacentState = level.getBlockState(adjacentPos);
                BlockEntity blockEntity = level.getBlockEntity(adjacentPos);

                // Si es tubería, seguir explorando
                if (pipeClass.isInstance(adjacentState.getBlock())) {
                    queue.add(adjacentPos);
                }
                // Si es el tipo de dispositivo que buscamos
                else if (deviceClass.isInstance(blockEntity)) {
                    T device = deviceClass.cast(blockEntity);
                    if (device.canReceiveEnergy()) {
                        connectedDevices.put(adjacentPos, device);
                    }
                }
            }
        }

        return connectedDevices;
    }

    public static float calculateEnergyLoss(Level level, BlockPos startPos, BlockPos endPos, Class<?> pipeClass) {
        int pipeDistance = calculatePipeDistance(level, startPos, endPos, pipeClass);
        return pipeDistance * ENERGY_LOSS_PER_PIPE;
    }

    private static int calculatePipeDistance(Level level, BlockPos startPos, BlockPos endPos, Class<?> pipeClass) {
        Queue<BlockPos> queue = new LinkedList<>();
        Map<BlockPos, Integer> distances = new HashMap<>();

        queue.add(startPos);
        distances.put(startPos, 0);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            int currentDistance = distances.get(currentPos);

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                // Si llegamos al dispositivo destino, retornar la distancia
                if (adjacentPos.equals(endPos)) {
                    return currentDistance; // La distancia es hasta la tubería anterior
                }

                if (distances.containsKey(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);

                // Solo seguir por tuberías del tipo correcto
                if (pipeClass.isInstance(adjacentState.getBlock())) {
                    distances.put(adjacentPos, currentDistance + 1);
                    queue.add(adjacentPos);
                }
            }
        }

        return Integer.MAX_VALUE; // No hay ruta conectada
    }
}
