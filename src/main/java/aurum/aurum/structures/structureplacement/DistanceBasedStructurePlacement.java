package aurum.aurum.structures.structureplacement;

import aurum.aurum.init.STStructurePlacements;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Esta clase extiende `RandomSpreadStructurePlacement` para permitir la colocación de estructuras
 * en función de una distancia mínima desde el origen del mundo en Minecraft.
 *
 * Características principales:
 * - Permite definir una distancia mínima (`min_distance_from_world_origin`) para evitar que las estructuras
 *   aparezcan cerca del centro del mundo.
 * - Valida que el valor de `spacing` siempre sea mayor que `separation` para evitar errores de generación.
 * - Sobrescribe el método `isPlacementChunk` para comprobar la distancia antes de decidir si se puede
 *   colocar una estructura en una posición determinada.
 * - Utiliza un `MapCodec` personalizado para serializar/deserializar la configuración de la colocación.
 */
public class DistanceBasedStructurePlacement extends RandomSpreadStructurePlacement {

    /**
     * Codec especial que añade el campo "min_distance_from_world_origin" para permitir
     * que las estructuras se generen en función de la distancia al centro del mundo.
     */
    public static final MapCodec<DistanceBasedStructurePlacement> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            // Offset opcional para la localización de la estructura (por defecto Vec3i.ZERO)
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(DistanceBasedStructurePlacement::locateOffset),
            // Método de reducción de frecuencia (por defecto FrequencyReductionMethod.DEFAULT)
            FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT).forGetter(DistanceBasedStructurePlacement::frequencyReductionMethod),
            // Frecuencia de aparición de la estructura (por defecto 1.0)
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(DistanceBasedStructurePlacement::frequency),
            // Sal (salt) para la generación aleatoria
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(DistanceBasedStructurePlacement::salt),
            // Zona de exclusión opcional para evitar que estructuras se generen cerca de otras
            ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(DistanceBasedStructurePlacement::exclusionZone),
            // Espaciado mínimo entre estructuras
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("spacing").forGetter(DistanceBasedStructurePlacement::spacing),
            // Separación mínima entre estructuras
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("separation").forGetter(DistanceBasedStructurePlacement::separation),
            // Tipo de dispersión aleatoria (por defecto LINEAR)
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(DistanceBasedStructurePlacement::spreadType),
            // Distancia mínima desde el origen del mundo (opcional)
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min_distance_from_world_origin").forGetter(DistanceBasedStructurePlacement::minDistanceFromWorldOrigin)
    ).apply(instance, instance.stable(DistanceBasedStructurePlacement::new)));

    /**
     * Distancia mínima opcional desde el origen del mundo para la generación de estructuras.
     */
    private final Optional<Integer> minDistanceFromWorldOrigin;

    /**
     * Constructor de la clase. Inicializa todos los parámetros necesarios para la colocación de la estructura.
     * Realiza una validación para asegurar que el espaciado siempre sea mayor que la separación.
     *
     * @param locationOffset Offset de localización de la estructura.
     * @param frequencyReductionMethod Método de reducción de frecuencia.
     * @param frequency Frecuencia de aparición.
     * @param salt Sal para la aleatoriedad.
     * @param exclusionZone Zona de exclusión opcional.
     * @param spacing Espaciado entre estructuras.
     * @param separation Separación mínima entre estructuras.
     * @param spreadType Tipo de dispersión.
     * @param minDistanceFromWorldOrigin Distancia mínima desde el origen del mundo.
     */
    public DistanceBasedStructurePlacement(Vec3i locationOffset,
                                           FrequencyReductionMethod frequencyReductionMethod,
                                           float frequency,
                                           int salt,
                                           Optional<ExclusionZone> exclusionZone,
                                           int spacing,
                                           int separation,
                                           RandomSpreadType spreadType,
                                           Optional<Integer> minDistanceFromWorldOrigin
    ) {
        super(locationOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.minDistanceFromWorldOrigin = minDistanceFromWorldOrigin;

        // Validación para asegurar que el espaciado sea mayor que la separación.
        if (spacing <= separation) {
            throw new RuntimeException("""
                El valor de spacing no puede ser menor o igual que separation.
                Corrige este error, ya que no se puede generar la estructura correctamente.
                    Spacing: %s
                    Separation: %s.
            """.formatted(spacing, separation));
        }
    }

    /**
     * Devuelve la distancia mínima opcional desde el origen del mundo.
     *
     * @return Distancia mínima desde el origen del mundo.
     */
    public Optional<Integer> minDistanceFromWorldOrigin() {
        return this.minDistanceFromWorldOrigin;
    }

    /**
     * Sobrescribe el método para añadir la comprobación de coordenadas.
     * Comprueba si la estructura está demasiado cerca del centro del mundo y, si es así, no permite su generación.
     * Si está suficientemente lejos, utiliza la lógica normal para decidir si se puede colocar la estructura.
     *
     * @param chunkGeneratorStructureState Estado del generador de estructuras.
     * @param x Posición X del chunk.
     * @param z Posición Z del chunk.
     * @return true si el chunk es válido para colocar la estructura, false en caso contrario.
     */
    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState chunkGeneratorStructureState, int x, int z) {
        if (minDistanceFromWorldOrigin.isPresent()) {
            // Convierte la posición del chunk a posición en bloques.
            long xBlockPos = x * 16L;
            long zBlockPos = z * 16L;

            // Comprobación rápida de distancia sin raíz cuadrada. El umbral es circular alrededor del origen.
            if ((xBlockPos * xBlockPos) + (zBlockPos * zBlockPos) < (((long) minDistanceFromWorldOrigin.get()) * minDistanceFromWorldOrigin.get())) {
                return false;
            }
        }

        // Lógica estándar para determinar si el chunk es válido para la estructura.
        ChunkPos chunkpos = this.getPotentialStructureChunk(chunkGeneratorStructureState.getLevelSeed(), x, z);
        return chunkpos.x == x && chunkpos.z == z;
    }

    /**
     * Devuelve el tipo de colocación de estructura personalizado registrado en STStructurePlacements.
     *
     * @return Tipo de colocación de estructura.
     */
    @Override
    public StructurePlacementType<?> type() {
        return STStructurePlacements.DISTANCE_BASED_STRUCTURE_PLACEMENT.get();
    }

}