package aurum.aurum.worldgen.Dimension.Features;

import aurum.aurum.init.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AurumVinesFeature extends Feature<NoneFeatureConfiguration> {

    public AurumVinesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int placed = 0;

        for (int i = 0; i < 200; i++) {
            BlockPos pos = origin.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
            if (level.isEmptyBlock(pos) && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN)) {
                int length = Mth.nextInt(random, 1, 8);
                growVine(level, pos, length);
                placed++;
            }
        }
        return placed > 0;
    }

    private void growVine(WorldGenLevel level, BlockPos pos, int length) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < length; i++) {
            if (!level.isEmptyBlock(mutable)) break;
            boolean isTip = (i == length - 1);
            BlockState state = isTip
                    ? ModBlocks.AURUM_VINES.get().defaultBlockState()
                    : ModBlocks.AURUM_VINES_PLANT.get().defaultBlockState();
            level.setBlock(mutable, state, 2);
            mutable.move(Direction.DOWN);
        }
    }
}
