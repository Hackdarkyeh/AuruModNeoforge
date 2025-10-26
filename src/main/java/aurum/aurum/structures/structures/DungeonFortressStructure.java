package aurum.aurum.structures.structures;

import aurum.aurum.init.STStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;

public class DungeonFortressStructure extends Structure {
    public static final int ALTURA_FIJA = 120;
    public static final int DISTANCIA_MINIMA = 500;
    public static final ResourceKey<Level> DIMENSION_OBJETIVO = Level.OVERWORLD; // Cambia si es otra dimensión
    public static final String BIOMA_OBJETIVO = "minecraft:plains"; // Cambia por tu bioma


    // Pool inicial de piezas para la estructura
    private final Holder<StructureTemplatePool> startPool;
    // Nombre opcional del jigsaw de inicio
    private final Optional<ResourceLocation> startJigsawName;
    // Profundidad máxima de ramificación de piezas
    private final int size;
    // Proveedor de altura para el inicio de la estructura
    private final HeightProvider startHeight;
    // Tipo de heightmap para proyectar la estructura
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    // Distancia máxima desde el centro para las piezas
    private final int maxDistanceFromCenter;
    // Padding de dimensión para evitar generar cerca de bordes
    private final DimensionPadding dimensionPadding;
    // Configuración de líquidos para la estructura
    private final LiquidSettings liquidSettings;
    /**
     * Constructor que inicializa todos los parámetros de la estructura.
     */
    public DungeonFortressStructure(StructureSettings config,
                               Holder<StructureTemplatePool> startPool,
                               Optional<ResourceLocation> startJigsawName,
                               int size,
                               HeightProvider startHeight,
                               Optional<Heightmap.Types> projectStartToHeightmap,
                               int maxDistanceFromCenter,
                               DimensionPadding dimensionPadding,
                               LiquidSettings liquidSettings)
    {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    // Define el CODEC para DungeonFortressStructure
    public static final MapCodec<DungeonFortressStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    DungeonFortressStructure.settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING).forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS).forGetter(s -> s.liquidSettings)
            ).apply(instance, DungeonFortressStructure::new)
    );

    protected static boolean extraSpawningChecks(GenerationContext context) {
        // Obtiene la posición del chunk.
        ChunkPos chunkPos = context.chunkPos();
        BlockPos pos = new BlockPos(chunkPos.getMiddleBlockX(), ALTURA_FIJA, chunkPos.getMiddleBlockZ());

        // Obtiene el biomeSource del generador de chunks.
        BiomeSource biomeSource = context.chunkGenerator().getBiomeSource();

        // Obtiene el bioma en la posición usando el BiomeSource y el Sampler del RandomState.
        Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ()),
                context.randomState().sampler()
        );

        // Verifica si el bioma es el objetivo.
        if (!biomeHolder.is(ResourceLocation.parse(BIOMA_OBJETIVO))) {
            return false;
        }

        // Verifica la distancia mínima desde el origen.
        BlockPos spawn = new BlockPos(0, ALTURA_FIJA, 0);
        if (pos.distSqr(spawn) < DISTANCIA_MINIMA * DISTANCIA_MINIMA) {
            return false;
        }

        return true;
    }

    /**
     * Determina el punto de generación de la estructura.
     * Si las comprobaciones adicionales fallan, no se genera la estructura en ese chunk.
     * Si pasan, configura la posición y llama al sistema de piezas Jigsaw para crear la estructura.
     */
    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {

        // Verifica si el lugar es válido para la estructura
        if (!DungeonFortressStructure.extraSpawningChecks(context)) {
            return Optional.empty();
        }

        // Calcula la altura de inicio usando el HeightProvider
        int startY = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));

        // Convierte las coordenadas del chunk a coordenadas de bloque
        ChunkPos chunkPos = context.chunkPos();
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());

        // Genera las piezas de la estructura usando el sistema Jigsaw
        Optional<GenerationStub> structurePiecesGenerator =
                JigsawPlacement.addPieces(
                        context,
                        this.startPool,
                        this.startJigsawName,
                        this.size,
                        blockPos,
                        false,
                        this.projectStartToHeightmap,
                        this.maxDistanceFromCenter,
                        PoolAliasLookup.EMPTY,
                        this.dimensionPadding,
                        this.liquidSettings);

        // Devuelve el generador de piezas para que el juego cree la estructura
        return structurePiecesGenerator;
    }

    // Puente autoconstruido
    public static void generarPuente(Level level, BlockPos startPos, Direction dir, BlockState puenteBlock) {
        BlockPos pos = startPos;
        while (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, puenteBlock, 3);
            pos = pos.relative(dir);
        }
    }

    // Flag para controlar instancia única
    public static class UniqueDungeonFlag extends SavedData {
        public UniqueDungeonFlag() {}

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            return tag;
        }

        public UniqueDungeonFlag(CompoundTag tag) {}
    }

    @Override
    public StructureType<?> type() {
        return STStructures.FORTALEZA_PORTAL_DIM_A.get();
    }
}