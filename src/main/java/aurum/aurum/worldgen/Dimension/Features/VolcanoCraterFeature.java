package aurum.aurum.worldgen.Dimension.Features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class VolcanoCraterFeature extends Feature<VolcanoCraterFeature.Config> {

    private static final double CRATER_N = 0.84;   // ruido a partir del cual estamos dentro del borde

    public record Config(int diamondRarity, int lavaY) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("diamond_rarity").forGetter(Config::diamondRarity),
                Codec.INT.fieldOf("lava_y").forGetter(Config::lavaY)
        ).apply(inst, Config::new));
    }

    public VolcanoCraterFeature(Codec<Config> codec) { super(codec); }

    private static int hash(int x, int z) {
        int h = (x * 73856093) ^ (z * 19349663);
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        Config cfg = ctx.config();
        int x0 = ctx.origin().getX(), z0 = ctx.origin().getZ();

        // router.lava = aurum:dim1_volcano_noise
        DensityFunction noise = level.getLevel().getChunkSource().randomState().router().lavaNoise();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;

        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double n = noise.compute(new DensityFunction.SinglePointContext(x, 0, z));
                if (n < CRATER_N) continue;                                   // fuera del cráter

                int existing = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (existing <= level.getMinBuildHeight() + 1) continue;      // sin suelo
                if (existing >= cfg.lavaY()) continue;                        // el borde: no se toca

                for (int y = existing + 1; y <= cfg.lavaY(); y++)
                    level.setBlock(p.set(x, y, z), Blocks.LAVA.defaultBlockState(), 2);

                int depth = cfg.lavaY() - existing;
                if (depth >= 3) {
                    int v = hash(x, z);
                    if (v % cfg.diamondRarity() == 0) {
                        int y = existing + 1 + (v >>> 8) % (depth - 1);
                        level.setBlock(p.set(x, y, z), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
                    }
                }
                any = true;
            }
        }
        return any;
    }
}