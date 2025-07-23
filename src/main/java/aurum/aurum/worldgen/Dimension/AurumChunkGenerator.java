package aurum.aurum.worldgen.Dimension;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

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

    private final NoiseGenerator noiseGenerator;

    public AurumChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.noiseGenerator = new NoiseGenerator(settings.value().getRandomSource().ordinal()); // Corrected: Extract seed from NoiseGeneratorSettings
    }



    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    mutablePos.set(x, y, z);
                    double noiseValue = noiseGenerator.perlinNoise(worldX * 0.01, y * 0.01, worldZ * 0.01); // Adjust scale as needed

                    if (noiseValue > 0.1) { // Threshold for solid block
                        chunk.setBlockState(mutablePos, Blocks.STONE.defaultBlockState(), false);
                    } else { // Air or void
                        chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }


    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunk.getPos().getMinBlockX() + x;
                    int worldZ = chunk.getPos().getMinBlockZ() + z;

                    for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                        mutablePos.set(x, y, z);
                        double noiseValue = noiseGenerator.perlinNoise(worldX * 0.01, y * 0.01, worldZ * 0.01); // Adjust scale as needed

                        if (noiseValue > 0.1) { // Threshold for solid block
                            chunk.setBlockState(mutablePos, Blocks.STONE.defaultBlockState(), false);
                        } else { // Air or void
                            chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
            return chunk;
        });
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
        super.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk, carving);
    }


    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
        double noiseValue = noiseGenerator.perlinNoise(x * 0.01, 0, z * 0.01); // Only 2D noise for base height
        return (int) (64 + noiseValue * 32); // Base height around Y=64, with variation
    }



    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] states = new BlockState[height.getHeight()];
        for (int y = height.getMinBuildHeight(); y < height.getMaxBuildHeight(); y++) {
            double noiseValue = noiseGenerator.perlinNoise(x * 0.01, y * 0.01, z * 0.01);
            if (noiseValue > 0.1) {
                states[y - height.getMinBuildHeight()] = Blocks.STONE.defaultBlockState();
            } else {
                states[y - height.getMinBuildHeight()] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(height.getMinBuildHeight(), states);
    }

}