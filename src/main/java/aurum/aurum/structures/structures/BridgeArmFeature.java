package aurum.aurum.structures.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BridgeArmFeature extends Feature<NoneFeatureConfiguration> {
    record Seg(ResourceLocation tpl, Rotation rot, BlockPos localJig) {}
    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("aurum", p); }

    static Seg seg(Direction d, int j) {
        if (j == 0) return switch (d) {  // tapones (los 4 NBT distintos que ya tienes)
            case NORTH -> new Seg(rl("puente_fortaleza_zzona_medio"),   Rotation.NONE, new BlockPos(24, 45, 47));
            case SOUTH -> new Seg(rl("puente_fortaleza_z_zona_medio"),  Rotation.NONE, new BlockPos(24, 45, 0));
            case EAST  -> new Seg(rl("puente_fortaleza_zzona_medio_e"), Rotation.NONE, new BlockPos(0, 45, 23));
            default    -> new Seg(rl("puente_fortaleza_z_zona_medio_o"), Rotation.COUNTERCLOCKWISE_90, new BlockPos(24, 45, 47));
        };
        ResourceLocation m = rl("puente_fortaleza_medio");
        return switch (d) {
            case NORTH -> new Seg(m, Rotation.NONE, new BlockPos(24, 45, 47));
            case SOUTH -> new Seg(m, Rotation.NONE, new BlockPos(24, 45, 0));
            case EAST  -> new Seg(m, Rotation.COUNTERCLOCKWISE_90, new BlockPos(24, 45, 0));
            default    -> new Seg(m, Rotation.COUNTERCLOCKWISE_90, new BlockPos(24, 45, 47));
        };
    }

    public BridgeArmFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel sl = level.getLevel();
        ChunkPos cp = new ChunkPos(ctx.origin());
        BoundingBox chunkBox = new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ());
        boolean any = false;
        for (BridgeNetwork.Plan plan : BridgeNetwork.plansNear(sl, cp))
            for (BridgeNetwork.Arm arm : plan.arms())
                for (int j = 0; j <= arm.medios(); j++) {
                    Seg seg = seg(arm.dir(), j);
                    StructureTemplate t = sl.getStructureManager().getOrCreate(seg.tpl());
                    StructurePlaceSettings s = new StructurePlaceSettings().setRotation(seg.rot())
                            .setBoundingBox(chunkBox).setKnownShape(true).setIgnoreEntities(true)
                            .addProcessor(JigsawReplacementProcessor.INSTANCE)
                            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
                    BlockPos jig = BridgeNetwork.near(plan.origin().getX(), plan.origin().getZ(),
                            plan.boxY() + BridgeNetwork.DECK, arm.dir(), j);
                    BlockPos anchor = jig.subtract(StructureTemplate.transform(seg.localJig(), Mirror.NONE, seg.rot(), BlockPos.ZERO));
                    if (!t.getBoundingBox(s, anchor).intersects(chunkBox)) continue;
                    t.placeInWorld(level, anchor, anchor, s, ctx.random(), Block.UPDATE_CLIENTS);
                    any = true;
                }
        return any;
    }
}