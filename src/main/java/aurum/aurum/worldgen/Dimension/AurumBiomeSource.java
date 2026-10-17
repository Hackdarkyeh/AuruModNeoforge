package aurum.aurum.worldgen.Dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import java.util.stream.Stream;

public class AurumBiomeSource extends BiomeSource {

    public static final MapCodec<AurumBiomeSource> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            MultiNoiseBiomeSource.CODEC.codec().fieldOf("delegate").forGetter(s -> s.delegate),
            Biome.CODEC.fieldOf("volcano_biome").forGetter(s -> s.volcanoBiome),
            Biome.CODEC.fieldOf("around_biome").forGetter(s -> s.aroundBiome),
            Codec.FLOAT.optionalFieldOf("volcano_threshold", 1.5F).forGetter(s -> s.volcanoThreshold),
            Codec.FLOAT.optionalFieldOf("around_threshold", 1.1F).forGetter(s -> s.aroundThreshold)
    ).apply(inst, AurumBiomeSource::new));

    private final MultiNoiseBiomeSource delegate;
    private final Holder<Biome> volcanoBiome;
    private final Holder<Biome> aroundBiome;
    private final float volcanoThreshold;
    private final float aroundThreshold;

    public AurumBiomeSource(MultiNoiseBiomeSource delegate, Holder<Biome> volcanoBiome, Holder<Biome> aroundBiome,
                            float volcanoThreshold, float aroundThreshold) {
        this.delegate = delegate;
        this.volcanoBiome = volcanoBiome;
        this.aroundBiome = aroundBiome;
        this.volcanoThreshold = volcanoThreshold;
        this.aroundThreshold = aroundThreshold;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(volcanoBiome, aroundBiome));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Climate.TargetPoint target = sampler.sample(x, y, z);
        float w = Climate.unquantizeCoord(target.weirdness());
        if (w >= volcanoThreshold) return volcanoBiome;
        if (w >= aroundThreshold) return aroundBiome;
        return delegate.getNoiseBiome(target);
    }
}