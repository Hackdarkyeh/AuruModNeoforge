package aurum.aurum.worldgen.structure;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Pueblo jigsaw con validacion de huella para dim1 (tipos "ground" y "upper").
 * Muestrea una rejilla alrededor del punto de inicio y rechaza el pueblo si la huella toca vacio,
 * sale de la ventana de alturas, pisa un bioma prohibido, es demasiado irregular o tiene poco bioma valido.
 * Corre en hilos de worldgen: solo usa lo que da el contexto (nada de ServerLevel).
 */
public class AurumVillageStructure extends Structure {

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Reject { VOID, Y_WINDOW, FORBIDDEN_BIOME, DEVIATION, BIOME_FRACTION }

    // Contadores compartidos entre todas las estructuras de este tipo (solo para depurar).
    private static final AtomicIntegerArray REJECTS = new AtomicIntegerArray(Reject.values().length);
    private static final AtomicInteger ACCEPTED = new AtomicInteger();

    public static final MapCodec<AurumVillageStructure> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            settingsCodec(inst),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.size),
            Codec.intRange(1, 116).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            Codec.INT.fieldOf("min_surface_y").forGetter(s -> s.minSurfaceY),
            Codec.INT.fieldOf("max_surface_y").forGetter(s -> s.maxSurfaceY),
            Codec.intRange(16, 96).fieldOf("sample_radius").forGetter(s -> s.sampleRadius),
            Codec.intRange(4, 32).optionalFieldOf("sample_step", 16).forGetter(s -> s.sampleStep),
            Codec.intRange(0, 64).fieldOf("max_height_deviation").forGetter(s -> s.maxDeviation),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("required_biome_fraction", 0.8).forGetter(s -> s.requiredBiomeFraction),
            Biome.LIST_CODEC.fieldOf("forbidden_biomes").forGetter(s -> s.forbiddenBiomes),
            Codec.BOOL.optionalFieldOf("debug_log", true).forGetter(s -> s.debugLog)
    ).apply(inst, AurumVillageStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final int maxDistance;
    private final int minSurfaceY;
    private final int maxSurfaceY;
    private final int sampleRadius;
    private final int sampleStep;
    private final int maxDeviation;
    private final double requiredBiomeFraction;
    private final HolderSet<Biome> forbiddenBiomes;
    private final boolean debugLog;
    private final String label;

    public AurumVillageStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size,
                                 int maxDistance, int minSurfaceY, int maxSurfaceY, int sampleRadius, int sampleStep,
                                 int maxDeviation, double requiredBiomeFraction, HolderSet<Biome> forbiddenBiomes,
                                 boolean debugLog) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.maxDistance = maxDistance;
        this.minSurfaceY = minSurfaceY;
        this.maxSurfaceY = maxSurfaceY;
        this.sampleRadius = sampleRadius;
        this.sampleStep = sampleStep;
        this.maxDeviation = maxDeviation;
        this.requiredBiomeFraction = requiredBiomeFraction;
        this.forbiddenBiomes = forbiddenBiomes;
        this.debugLog = debugLog;
        this.label = startPool.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    private Optional<GenerationStub> reject(Reject why, String detail) {
        int n = REJECTS.incrementAndGet(why.ordinal());
        if (debugLog) {
            LOGGER.info("[AurumVillage] RECHAZO {} (#{}) en {} | {} | ventana Y [{}, {}]",
                    why, n, label, detail, minSurfaceY, maxSurfaceY);
        }
        return Optional.empty();
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkGenerator gen = ctx.chunkGenerator();
        LevelHeightAccessor level = ctx.heightAccessor();
        RandomState rs = ctx.randomState();

        int minY = gen.getMinY();
        // Mismo punto XZ en el que JigsawStructure pone el inicio (esquina minima del chunk).
        int ox = ctx.chunkPos().getMinBlockX();
        int oz = ctx.chunkPos().getMinBlockZ();

        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        int total = 0;
        int okBiome = 0;

        for (int dx = -sampleRadius; dx <= sampleRadius; dx += sampleStep) {
            for (int dz = -sampleRadius; dz <= sampleRadius; dz += sampleStep) {
                int x = ox + dx;
                int z = oz + dz;

                int y = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
                String where = "muestra (" + x + ", " + z + ") y=" + y;

                // 1) Vacio / borde del continente: el heightmap devuelve el fondo del mundo.
                if (y <= minY + 1) {
                    return reject(Reject.VOID, where);
                }
                // 2) Ventana de alturas: separa ground (continente) de upper (capa de islas).
                if (y < minSurfaceY || y > maxSurfaceY) {
                    return reject(Reject.Y_WINDOW, where + " lo=" + (lo == Integer.MAX_VALUE ? "-" : lo)
                            + " hi=" + (hi == Integer.MIN_VALUE ? "-" : hi));
                }
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);

                Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), rs.sampler());

                // 3) Biomas prohibidos (volcanes y, en ground/upper, void/void_river).
                if (forbiddenBiomes.contains(biome)) {
                    return reject(Reject.FORBIDDEN_BIOME, where + " bioma="
                            + biome.unwrapKey().map(k -> k.location().toString()).orElse("?"));
                }
                total++;
                if (ctx.validBiome().test(biome)) {
                    okBiome++;
                }
            }
        }

        // 4) Planitud de la huella.
        if (hi - lo > maxDeviation) {
            return reject(Reject.DEVIATION, "lo=" + lo + " hi=" + hi + " (max desviacion " + maxDeviation + ")");
        }
        // 5) Fraccion de bioma valido.
        if (okBiome < Math.ceil(total * requiredBiomeFraction)) {
            return reject(Reject.BIOME_FRACTION, okBiome + "/" + total + " muestras validas (min "
                    + requiredBiomeFraction + ")");
        }

        BlockPos start = new BlockPos(ox, 0, oz);
        Optional<GenerationStub> stub = JigsawPlacement.addPieces(
                ctx, startPool, Optional.empty(), size, start, true,
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG), maxDistance,
                PoolAliasLookup.EMPTY,
                JigsawStructure.DEFAULT_DIMENSION_PADDING,
                JigsawStructure.DEFAULT_LIQUID_SETTINGS);

        if (debugLog) {
            LOGGER.info("[AurumVillage] ACEPTADO (#{}) en {} | chunk ({}, {}) lo={} hi={} biomas validos {}/{} | piezas={}",
                    ACCEPTED.incrementAndGet(), label, ctx.chunkPos().x, ctx.chunkPos().z, lo, hi,
                    okBiome, total, stub.isPresent() ? "si" : "no");
        }
        return stub;
    }

    @Override
    public StructureType<?> type() {
        return AurumStructureTypes.VILLAGE.get();
    }
}
