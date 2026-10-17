package aurum.aurum.worldgen.Dimension.Features;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.slf4j.Logger;

public class VolcanoCraterFeature extends Feature<VolcanoCraterFeature.Config> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public record Config(int diamondRarity) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("diamond_rarity").forGetter(Config::diamondRarity)
        ).apply(inst, Config::new));
    }

    public VolcanoCraterFeature(Codec<Config> codec) { super(codec); }

    private static double at(DensityFunction f, int x, int y, int z) {
        return f.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    private static int hash(int x, int z) {
        int h = (x * 73856093) ^ (z * 19349663);
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX(), z0 = ctx.origin().getZ();

        RandomState rs = level.getLevel().getChunkSource().randomState();
        DensityFunction noise = rs.router().lavaNoise();      // = aurum:dim1_volcano_noise
        DensityFunction ridges = rs.router().ridges();
        DensityFunction density = rs.router().finalDensity();

        // 1. Cima: ascenso por rejilla absoluta de 4 bloques (mismo resultado en todos los chunks del volcán)
        int cx = ((x0 + 8) >> 2) << 2, cz = ((z0 + 8) >> 2) << 2;
        double best = at(noise, cx, 0, cz);
        if (best < 0.55) return false;
        for (int it = 0; it < 150; it++) {
            int nx = cx, nz = cz; double nv = best;
            for (int dx = -4; dx <= 4; dx += 4)
                for (int dz = -4; dz <= 4; dz += 4) {
                    if (dx == 0 && dz == 0) continue;
                    double v = at(noise, cx + dx, 0, cz + dz);
                    if (v > nv) { nv = v; nx = cx + dx; nz = cz + dz; }
                }
            if (nv <= best) break;
            cx = nx; cz = nz; best = nv;
        }

        // 2. Solo si en la cima hay bioma volcano (misma regla que AurumBiomeSource)
        if (at(ridges, cx, 0, cz) < 1.5) return false;

        // 3. Altura de la cima, bajando por la densidad final (empieza bajo las islas)
        int hs = Integer.MIN_VALUE;
        for (int y = 185; y > level.getMinBuildHeight(); y--) {
            if (at(density, cx, y, cz) > 0) { hs = y; break; }
        }
        if (hs < 50) return false;

        // 4. Dimensiones proporcionales a la altura del volcán
        double h = hs - 40;
        double rimR = Mth.clamp(8 + 0.4 * h, 12, 26);
        int crest = hs + (int) Mth.clamp(h * 0.12, 3, 7);
        int depth = (int) Mth.clamp(0.55 * h + 10, 14, 40);
        int floorY = crest - depth;
        int lava = floorY + (int) Math.round(depth * 0.55);
        double outerR = rimR + 34;

        if (cx >= x0 && cx < x0 + 16 && cz >= z0 && cz < z0 + 16)
            LOGGER.info("[Aurum] Volcán en {}, {}: cima y={}, radio borde={}, lava y={}", cx, cz, hs, (int) rimR, lava);

        // 5. Cada chunk dibuja solo sus propias columnas
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > outerR) continue;
                int existing = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (existing <= level.getMinBuildHeight() + 1) continue;   // sin suelo: no construir sobre el vacío

                int target;
                if (d <= rimR) {
                    double t = d / rimR;
                    target = floorY + (int) Math.round(depth * t * t);                       // cuenco
                } else {
                    target = (int) Math.round(Math.max(existing, crest - (d - rimR) * 2.0)); // rampa exterior
                }

                BlockState surface = level.getBlockState(p.set(x, existing, z));
                BlockState fill = (surface.isAir() || !surface.getFluidState().isEmpty())
                        ? Blocks.STONE.defaultBlockState() : surface;
                for (int y = target + 1; y <= existing; y++) level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                for (int y = existing + 1; y <= target; y++) level.setBlock(p.set(x, y, z), fill, 2);

                if (d <= rimR && target < lava) {
                    for (int y = target + 1; y <= lava; y++) level.setBlock(p.set(x, y, z), Blocks.LAVA.defaultBlockState(), 2);
                    if (d <= rimR * 0.7 && target + 4 < lava) {
                        int v = hash(x, z);
                        if (v % ctx.config().diamondRarity() == 0) {
                            int y = target + 2 + (v >>> 8) % Math.max(1, lava - target - 3);
                            level.setBlock(p.set(x, y, z), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        return true;
    }
}