package aurum.aurum.block;

import aurum.aurum.init.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AurumVinesBlock extends GrowingPlantHeadBlock implements BonemealableBlock {

    public static final MapCodec<AurumVinesBlock> CODEC = simpleCodec(AurumVinesBlock::new);

    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    public AurumVinesBlock(BlockBehaviour.Properties properties) {
        super(properties, Direction.DOWN, SHAPE, false, 0.1);
    }

    @Override
    public MapCodec<AurumVinesBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return Mth.nextInt(random, 1, 4);
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int growAmount = Mth.nextInt(random, 1, 4);
        BlockPos.MutableBlockPos mutable = pos.mutable();

        for (int i = 0; i < growAmount; i++) {
            BlockPos nextPos = mutable.relative(this.growthDirection);
            if (!level.isInWorldBounds(nextPos) || !level.getBlockState(nextPos).isAir()) {
                break;
            }
            // El bloque actual (cabeza) pasa a ser cuerpo
            level.setBlock(mutable, this.getBodyBlock().defaultBlockState(), 3);
            // La nueva cabeza se coloca un bloque más abajo
            level.setBlock(nextPos, this.defaultBlockState(), 3);
            mutable.move(this.growthDirection);
        }
    }

    @Override
    protected GrowingPlantBodyBlock getBodyBlock() {
        return (GrowingPlantBodyBlock) ModBlocks.AURUM_VINES_PLANT.get();
    }
}
