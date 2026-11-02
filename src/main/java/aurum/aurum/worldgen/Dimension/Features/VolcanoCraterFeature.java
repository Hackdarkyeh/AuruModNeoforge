package aurum.aurum.worldgen.Dimension.Features;

import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModFluids;   // <-- ajusta al paquete/clase donde tienes AURUM_ROSA
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class VolcanoCraterFeature extends Feature<VolcanoCraterFeature.Config> {

    private static final double LAKE_N = 0.89;   // dentro del borde del cráter
    private static final double CORE_N = 0.90;   // núcleo: aquí todo es fluido

    public record Config(int diamondRarity, int lavaY) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("diamond_rarity").forGetter(Config::diamondRarity),
                Codec.INT.fieldOf("lava_y").forGetter(Config::lavaY)      // nivel del lago (el nombre se queda por compatibilidad)
        ).apply(inst, Config::new));
    }

    public VolcanoCraterFeature(Codec<Config> codec) { super(codec); }

    private static int hash(int x, int z) {
        int h = (x * 73856093) ^ (z * 19349663);
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    /** Bloque del fluido del mod (fuente). Si no tiene bloque asignado, lava de respaldo. */
    private static BlockState lakeState() {
        BlockState s = ModFluids.AURUM_ROSA.get().getSource(false).createLegacyBlock();
        return s.isAir() ? Blocks.LAVA.defaultBlockState() : s;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        Config cfg = ctx.config();

        // Siempre trabajamos sobre el chunk completo, aunque el origen venga movido
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;

        // router.lava = aurum:dim1_volcano_noise
        DensityFunction noise = level.getLevel().getChunkSource().randomState().router().lavaNoise();
        int minY = level.getMinBuildHeight() + 1;
        BlockState lake = lakeState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;

        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double n = noise.compute(new DensityFunction.SinglePointContext(x, 0, z));
                if (n < LAKE_N) continue;

                int existing = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (existing <= minY) continue;                                   // sin suelo

                boolean core = n >= CORE_N;
                if (!core && existing >= cfg.lavaY()) continue;                   // borde: no se toca

                // Núcleo: fuera cualquier resto de roca por encima del nivel del lago
                if (core)
                    for (int y = cfg.lavaY() + 1; y <= existing; y++)
                        level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);

                // Lago compacto, sin huecos
                int bottom = Math.max(minY, Math.min(existing, cfg.lavaY()) - 12);
                for (int y = bottom; y <= cfg.lavaY(); y++)
                    level.setBlock(p.set(x, y, z), lake, 2);

                // Diamantes siempre sumergidos
                int depth = cfg.lavaY() - bottom;
                if (depth >= 6) {
                    int v = hash(x, z);
                    if (v % cfg.diamondRarity() == 0) {
                        int y = bottom + 2 + (v >>> 8) % (depth - 3);
                        level.setBlock(p.set(x, y, z), ModBlocks.AURELITE_ORE.get().defaultBlockState(), 2);
                    }
                }
                any = true;
            }
        }
        return any;
    }
}