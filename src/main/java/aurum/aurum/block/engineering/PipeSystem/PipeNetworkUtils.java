package aurum.aurum.block.engineering.PipeSystem;

// PipeNetworkUtils.java - Clase reutilizable para ambos tipos de energía

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class PipeNetworkUtils {

    public static final float ENERGY_LOSS_PER_PIPE = 0.12f; // 12% de pérdida por tubería

    /** Máximo de bloques que se exploran en una búsqueda, para evitar redes gigantes. */
    public static final int MAX_NODES = 2048;

    public interface IEnergyDevice {
        boolean canReceiveEnergy();
        float getDarkEnergyStored();
        float getDarkEnergyCapacity();
        void receiveDarkEnergy(float amount);
    }

    /** Dispositivo encontrado y número de tuberías hasta él. */
    public record Found<T>(T device, int pipes) {}

    /**
     * Un único recorrido BFS que devuelve cada dispositivo receptor junto con su distancia en tuberías.
     * Sustituye a llamar a calculateEnergyLoss una vez por dispositivo.
     */
    public static <T extends IEnergyDevice> Map<BlockPos, Found<T>> findConnectedDevicesWithDistance(
            Level level, BlockPos startPos, Class<T> deviceClass, Class<?> pipeClass) {

        Map<BlockPos, Found<T>> result = new HashMap<>();
        Map<BlockPos, Integer> distances = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(startPos);
        distances.put(startPos, 0);

        while (!queue.isEmpty() && distances.size() < MAX_NODES) {
            BlockPos currentPos = queue.poll();
            int currentDistance = distances.get(currentPos);

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                if (distances.containsKey(adjacentPos) || result.containsKey(adjacentPos)) continue;
                // No cargar ni recorrer chunks que no están cargados
                if (!level.hasChunkAt(adjacentPos)) continue;

                if (pipeClass.isInstance(level.getBlockState(adjacentPos).getBlock())) {
                    distances.put(adjacentPos, currentDistance + 1);
                    queue.add(adjacentPos);
                } else {
                    BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
                    if (deviceClass.isInstance(blockEntity)) {
                        T device = deviceClass.cast(blockEntity);
                        if (device.canReceiveEnergy()) {
                            result.put(adjacentPos, new Found<>(device, currentDistance));
                        }
                    }
                }
            }
        }

        return result;
    }

    /** Misma API que antes: solo los dispositivos conectados. */
    public static <T extends IEnergyDevice> Map<BlockPos, T> findConnectedDevices(Level level, BlockPos startPos,
                                                                                  Class<T> deviceClass,
                                                                                  Class<?> pipeClass) {
        Map<BlockPos, T> devices = new HashMap<>();
        findConnectedDevicesWithDistance(level, startPos, deviceClass, pipeClass)
                .forEach((pos, found) -> devices.put(pos, found.device()));
        return devices;
    }

    public static float calculateEnergyLoss(Level level, BlockPos startPos, BlockPos endPos, Class<?> pipeClass) {
        int pipeDistance = calculatePipeDistance(level, startPos, endPos, pipeClass);
        return pipeDistance * ENERGY_LOSS_PER_PIPE;
    }

    private static int calculatePipeDistance(Level level, BlockPos startPos, BlockPos endPos, Class<?> pipeClass) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> distances = new HashMap<>();

        queue.add(startPos);
        distances.put(startPos, 0);

        while (!queue.isEmpty() && distances.size() < MAX_NODES) {
            BlockPos currentPos = queue.poll();
            int currentDistance = distances.get(currentPos);

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                // Si llegamos al dispositivo destino, la distancia es hasta la tubería anterior
                if (adjacentPos.equals(endPos)) {
                    return currentDistance;
                }

                if (distances.containsKey(adjacentPos) || !level.hasChunkAt(adjacentPos)) continue;

                // Solo seguir por tuberías del tipo correcto
                if (pipeClass.isInstance(level.getBlockState(adjacentPos).getBlock())) {
                    distances.put(adjacentPos, currentDistance + 1);
                    queue.add(adjacentPos);
                }
            }
        }

        return Integer.MAX_VALUE; // No hay ruta conectada
    }
}