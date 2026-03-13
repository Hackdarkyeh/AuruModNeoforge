package aurum.aurum.eventHandler;

import aurum.aurum.init.STStructures;
import aurum.aurum.structures.FortressBridgeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Event handler que genera los puentes dinámicos DESPUÉS de que la fortaleza se haya generado.
 * Utiliza SavedData para evitar regenerar puentes múltiples veces.
 */
@EventBusSubscriber
public class FortressBridgeEventHandler {

    private static final String FORTRESS_BRIDGES_DATA = "fortress_bridges_generated";

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Solo en el servidor
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Obtener el gestor de datos persistentes
        FortressBridgeTrackerData trackerData = serverLevel.getDataStorage()
                .computeIfAbsent(FortressBridgeTrackerData.FACTORY, FORTRESS_BRIDGES_DATA);

        // Buscar estructuras en el chunk
        var structureManager = serverLevel.structureManager();
        var chunkPos = event.getChunk().getPos();

        // Verificar si hay estructuras de tipo fortaleza en este chunk
        var structuresAtPos = structureManager.getAllStructuresAt(chunkPos.getWorldPosition());

        for (var structureEntry : structuresAtPos.entrySet()) {
            Structure structure = structureEntry.getKey();

            // Verificar si es nuestra estructura de fortaleza
            if (structure.type() == STStructures.FORTALEZA_PORTAL_DIM_A.get()) {
                // Obtener la información de la estructura generada para este chunk
                var structureStart = structureManager.getStructureAt(chunkPos.getWorldPosition(), structure);

                if (structureStart.isValid()) {
                    // Obtener el bounding box de la estructura
                    BoundingBox boundingBox = structureStart.getBoundingBox();
                    BlockPos center = new BlockPos(
                        boundingBox.minX() + (boundingBox.maxX() - boundingBox.minX()) / 2,
                        boundingBox.minY(),
                        boundingBox.minZ() + (boundingBox.maxZ() - boundingBox.minZ()) / 2
                    );

                    // Generar los puentes desde este centro (solo una vez por estructura)
                    if (isFirstChunkOfStructure(chunkPos, boundingBox)) {
                        // Verificar si ya fue procesada esta estructura
                        String structureKey = center.toShortString();

                        if (!trackerData.hasProcessedStructure(structureKey)) {
                            System.out.println("[FortressBridge] Generating bridges for fortress at: " + center);
                            FortressBridgeGenerator.generateBridgesFromBounds(serverLevel, boundingBox);

                            // Marcar como procesada
                            trackerData.markStructureProcessed(structureKey);
                            trackerData.setDirty();
                        }
                    }
                }

                break;
            }
        }
    }

    /**
     * Verifica si este es el primer chunk de la estructura (chunk que contiene el centro)
     */
    private static boolean isFirstChunkOfStructure(net.minecraft.world.level.ChunkPos chunkPos, BoundingBox boundingBox) {
        int centerChunkX = (boundingBox.minX() + (boundingBox.maxX() - boundingBox.minX()) / 2) >> 4;
        int centerChunkZ = (boundingBox.minZ() + (boundingBox.maxZ() - boundingBox.minZ()) / 2) >> 4;

        return chunkPos.x == centerChunkX && chunkPos.z == centerChunkZ;
    }

    /**
     * SavedData para rastrear qué estructuras ya han tenido sus puentes generados
     */
    public static class FortressBridgeTrackerData extends SavedData {
        private final Set<String> processedStructures = new HashSet<>();

        public FortressBridgeTrackerData() {}

        public static final SavedData.Factory<FortressBridgeTrackerData> FACTORY = new SavedData.Factory<>(
                FortressBridgeTrackerData::new,
                (tag, provider) -> FortressBridgeTrackerData.load(tag),
                null
        );

        public static FortressBridgeTrackerData load(CompoundTag tag) {
            FortressBridgeTrackerData data = new FortressBridgeTrackerData();
            // Cargar el conjunto de estructuras procesadas desde NBT
            if (tag.contains("processed_structures")) {
                CompoundTag structuresTag = tag.getCompound("processed_structures");
                for (String key : structuresTag.getAllKeys()) {
                    if (structuresTag.getBoolean(key)) {
                        data.processedStructures.add(key);
                    }
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            CompoundTag structuresTag = new CompoundTag();
            for (String structure : processedStructures) {
                structuresTag.putBoolean(structure, true);
            }
            tag.put("processed_structures", structuresTag);
            return tag;
        }

        public boolean hasProcessedStructure(String structureKey) {
            return processedStructures.contains(structureKey);
        }

        public void markStructureProcessed(String structureKey) {
            processedStructures.add(structureKey);
        }
    }
}
