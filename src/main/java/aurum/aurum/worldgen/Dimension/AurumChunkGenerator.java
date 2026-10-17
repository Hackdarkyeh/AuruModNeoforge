package aurum.aurum.worldgen.Dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * IMPORTANTE — qué cambió respecto a la versión anterior y por qué:
 *
 * Antes, esta clase sobreescribía buildSurface(), fillFromNoise(), getBaseColumn()
 * y getBaseHeight() con un Perlin noise casero (NoiseGenerator) que no tenía
 * absolutamente ninguna relación con el biome_source (multi_noise) definido en
 * dimension1.json. Resultado: el bioma se elegía con un sistema de ruido climático
 * (continentalness/erosion/weirdness/temperature/humidity) y el terreno con otro
 * completamente distinto -> desincronización total (biomas "vacíos" en zonas que
 * deberían ser purple_plains, etc).
 *
 * El noise_router y el surface_rule que ya tienes en tu noise settings
 * ("aurum:dimension1") están correctamente atados por bioma (dry_withered_grass_block
 * para volcano_around, withered_grass_block para purple_plains, etc). El problema
 * es que esos overrides impedían que ese sistema se ejecutara nunca.
 *
 * Al NO sobreescribir esos métodos, NoiseBasedChunkGenerator (la clase padre) usa
 * directamente el RandomState/NoiseGeneratorSettings que le pasamos — es decir,
 * tu noise_router + surface_rule + final_density — para generar el terreno,
 * garantizando que biomas y terreno comparten exactamente la misma fuente de ruido.
 *
 * El seed bug (`settings.value().getRandomSource().ordinal()`) también desaparece
 * porque ya no necesitamos un NoiseGenerator propio para el terreno base. Si en el
 * futuro quieres usar Perlin noise propio para otra cosa (decoración, elegir
 * variante de cráter, etc.), pásale la seed real del mundo, no el ordinal de un enum.
 */
public class AurumChunkGenerator extends NoiseBasedChunkGenerator {

    public static final MapCodec<AurumChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings)
            ).apply(instance, instance.stable(AurumChunkGenerator::new)));

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public AurumChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    // Sin overrides de buildSurface / fillFromNoise / getBaseColumn / getBaseHeight /
    // applyCarvers / applyBiomeDecoration: se heredan de NoiseBasedChunkGenerator tal
    // cual, que es exactamente lo que necesitamos para que el terreno respete tu
    // noise_router y tu surface_rule por bioma.
}