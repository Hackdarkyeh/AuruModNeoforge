package aurum.aurum.worldgen.Dimension;

import aurum.aurum.Aurum;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AurumDimension {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Aurum.MODID);

    public static final Supplier<MapCodec<AurumBiomeSource>> VOLCANO_OVERRIDE =
            BIOME_SOURCES.register("volcano_override", () -> AurumBiomeSource.CODEC);

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, "aurum");
    public static final Supplier<MapCodec<AurumChunkGenerator>> AURUM_NOISE =
            CHUNK_GENERATORS.register("aurum_noise", () -> AurumChunkGenerator.CODEC);
}