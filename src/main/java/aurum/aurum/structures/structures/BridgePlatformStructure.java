package aurum.aurum.structures.structures;

import aurum.aurum.init.STStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;


public class BridgePlatformStructure extends Structure {
    public static final MapCodec<BridgePlatformStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.size)
    ).apply(i, BridgePlatformStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    public BridgePlatformStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        LOG.info("[bridge] candidato evaluado en chunk {}", ctx.chunkPos());
        Optional<BridgeNetwork.Plan> plan = BridgeNetwork.plan(ctx.seed(), ctx.chunkPos(),
                ctx.chunkGenerator(), ctx.randomState(), ctx.heightAccessor());
        if (plan.isEmpty()) return Optional.empty();

        long sd = ctx.seed() ^ ctx.chunkPos().toLong();
        while (new LegacyRandomSource(sd).nextInt(4) != 0) sd++;   // fuerza Rotation.NONE
        GenerationContext fixed = new GenerationContext(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
                ctx.randomState(), ctx.structureTemplateManager(), new WorldgenRandom(new LegacyRandomSource(sd)),
                ctx.seed(), ctx.chunkPos(), ctx.heightAccessor(), ctx.validBiome());

        BlockPos pos = new BlockPos(ctx.chunkPos().getMinBlockX(),
                plan.get().boxY() + BridgeNetwork.DECK, ctx.chunkPos().getMinBlockZ());
        return JigsawPlacement.addPieces(fixed, startPool, Optional.empty(), size, pos, false, Optional.empty(), 128,
                PoolAliasLookup.EMPTY, JigsawStructure.DEFAULT_DIMENSION_PADDING, LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public StructureType<?> type() {
        return STStructureTypes.BRIDGE_PLATFORM.get();
    }
}