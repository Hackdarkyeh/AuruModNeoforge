package aurum.aurum.structures.structures;

import aurum.aurum.init.ModTags;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BridgeNetwork {
    public static final int T = 48, MAX_MEDIOS = 64, DECK = 45, PROBE_Y = 64;
    private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static final ResourceKey<Structure> PLATFORM = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("aurum", "fortaleza_portal_dim_a"));

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** origin = esquina mínima (x,z) de la pieza central 'e'; boxY = Y mínima de los cubos del puente. */
    public record Arm(Direction dir, int medios, boolean reachesLand) {}
    public record Plan(BlockPos origin, int boxY, List<Arm> arms) {}
    private record Key(long seed, long chunk) {}
    private static final Cache<Key, Optional<Plan>> CACHE = CacheBuilder.newBuilder().maximumSize(4096).build();

    /** Jigsaw de la cara cercana del segmento j (0 = tapón, 1.. = tramos medio). */
    public static BlockPos near(int ox, int oz, int y, Direction d, int j) {
        int o = T * j;
        return switch (d) {
            case NORTH -> new BlockPos(ox + 24, y, oz - 49 - o);
            case SOUTH -> new BlockPos(ox + 23, y, oz + 96 + o);
            case EAST  -> new BlockPos(ox + 96 + o, y, oz + 23);
            default    -> new BlockPos(ox - 49 - o, y, oz + 23);
        };
    }

    static boolean isVoid(BiomeSource bs, Climate.Sampler s, int x, int y, int z) {
        return bs.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), s)
                 .is(ModTags.Biomes.IS_DIM1_BIOME7_VOID);
    }

    public static Optional<Plan> plan(long seed, ChunkPos pc, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h) {
        return CACHE.asMap().computeIfAbsent(new Key(seed, pc.toLong()), k -> compute(seed, pc, gen, rs, h));
    }

    private static Optional<Plan> compute(long seed, ChunkPos pc, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h) {
        int ox = pc.getMinBlockX(), oz = pc.getMinBlockZ();
        BiomeSource bs = gen.getBiomeSource();
        Climate.Sampler s = rs.sampler();

        // la plataforma entera (144x144) debe estar en el vacío
        for (int[] q : new int[][]{{-48, -48}, {95, -48}, {-48, 95}, {95, 95}, {24, 24}}) {
            int qx = ox + q[0], qz = oz + q[1];
            if (!isVoid(bs, s, qx, PROBE_Y, qz)) {
                LOG.info("[bridge] {} descartada: no es vacío en ({}, {}); biomas por Y: {}", pc, qx, qz,
                        java.util.stream.IntStream.of(-32, 0, 64, 128, 192).mapToObj(yy ->
                                yy + "=" + bs.getNoiseBiome(QuartPos.fromBlock(qx), QuartPos.fromBlock(yy),
                                                QuartPos.fromBlock(qz), s).unwrapKey()
                                        .map(k -> k.location().getPath()).orElse("?")).toList());
                return Optional.empty();
            }
        }

        int[] land = {-1, -1, -1, -1};
        int main = -1;
        for (int i = 0; i < 4; i++) {
            scan:
            for (int j = 1; j <= MAX_MEDIOS; j++)
                for (int k = 0; k < T; k += 8) {
                    BlockPos p = near(ox, oz, 0, DIRS[i], j).relative(DIRS[i], k);
                    if (!isVoid(bs, s, p.getX(), PROBE_Y, p.getZ())) {
                        land[i] = (j - 1) * T + k;
                        break scan;
                    }
                }
            if (land[i] >= 0 && (main < 0 || land[i] < land[main])) main = i;
        }
        if (main < 0) {
            LOG.info("[bridge] {} descartada: ningún lado llega a tierra en {} bloques", pc, MAX_MEDIOS * T);
            return Optional.empty();
        }

        BlockPos end = near(ox, oz, 0, DIRS[main], 1).relative(DIRS[main], land[main] + 16);
        int surface = gen.getBaseHeight(end.getX(), end.getZ(), Heightmap.Types.WORLD_SURFACE_WG, h, rs);
        int boxY = Mth.clamp(surface - DECK, h.getMinBuildHeight(), h.getMaxBuildHeight() - 136);

        RandomSource rnd = RandomSource.create(seed ^ (pc.toLong() * 0x9E3779B97F4A7C15L));
        List<Arm> arms = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (land[i] >= 0) {
                arms.add(new Arm(DIRS[i], Math.max(1, Mth.ceil((land[i] + 8) / (float) T)), true));
            } else if (rnd.nextFloat() < 0.5f) {
                arms.add(new Arm(DIRS[i], 1 + rnd.nextInt(12), false)); // brazo cortado
            }
        }
        LOG.info("[bridge] {} OK boxY={} surface={} principal={} brazos={}", pc, boxY, surface, DIRS[main], arms);
        return Optional.of(new Plan(new BlockPos(ox, 0, oz), boxY, arms));
    }

    public static List<Plan> plansNear(ServerLevel level, ChunkPos chunk) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        ChunkGeneratorStructureState st = level.getChunkSource().getGeneratorState();
        Holder<Structure> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(PLATFORM);
        List<Plan> out = new ArrayList<>();
        for (StructurePlacement p : st.getPlacementsForStructure(structure)) {
            if (!(p instanceof RandomSpreadStructurePlacement rsp)) continue;
            int sp = rsp.spacing(), r = Mth.ceil((MAX_MEDIOS * 3 + 12) / (float) sp);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                ChunkPos pc = rsp.getPotentialStructureChunk(st.getLevelSeed(),
                        (Math.floorDiv(chunk.x, sp) + dx) * sp, (Math.floorDiv(chunk.z, sp) + dz) * sp);
                int ax = chunk.x - pc.x, az = chunk.z - pc.z;
                if (!(ax >= -1 && ax <= 2) && !(az >= 0 && az <= 2)) continue;  // solo chunks de la "cruz"
                if (!rsp.isStructureChunk(st, pc.x, pc.z)) continue;
                plan(st.getLevelSeed(), pc, gen, rs, level).ifPresent(out::add);
            }
        }
        return out;
    }
}