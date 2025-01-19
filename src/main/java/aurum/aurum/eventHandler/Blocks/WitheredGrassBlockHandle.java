package aurum.aurum.eventHandler.Blocks;

import aurum.aurum.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class WitheredGrassBlockHandle {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getBlockState(BlockPos.containing(x, y + 1, z)).canOcclude()) {
            world.setBlock(BlockPos.containing(x, y, z), ModBlocks.WITHERED_DIRT_BLOCK.get().defaultBlockState(), 3);
        }
    }
}
