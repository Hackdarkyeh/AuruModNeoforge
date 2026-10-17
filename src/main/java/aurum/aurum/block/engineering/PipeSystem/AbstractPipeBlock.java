package aurum.aurum.block.engineering.PipeSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Base común para todas las tuberías. Cada cara tiene una propiedad booleana que indica
 * si hay una conexión en esa dirección. Las subclases solo deciden a qué se conectan.
 */
public abstract class AbstractPipeBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Mismo orden que Direction.values(): DOWN, UP, NORTH, SOUTH, WEST, EAST. */
    private static final BooleanProperty[] PROPS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    private static final Direction[] DIRS = Direction.values();

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN);

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARM_SHAPES = {
            Block.box(5, 0, 5, 11, 5, 11),   // DOWN
            Block.box(5, 11, 5, 11, 16, 11), // UP
            Block.box(5, 5, 0, 11, 11, 5),   // NORTH
            Block.box(5, 5, 11, 11, 11, 16), // SOUTH
            Block.box(0, 5, 5, 5, 11, 11),   // WEST
            Block.box(11, 5, 5, 16, 11, 11)  // EAST
    };

    /** 64 combinaciones de conexiones, precalculadas una sola vez. */
    private static final VoxelShape[] SHAPES = buildShapes();

    private static VoxelShape[] buildShapes() {
        VoxelShape[] out = new VoxelShape[64];
        for (int i = 0; i < 64; i++) {
            VoxelShape shape = CORE;
            for (int j = 0; j < 6; j++) {
                if ((i & (1 << j)) != 0) {
                    shape = Shapes.or(shape, ARM_SHAPES[j]);
                }
            }
            out[i] = shape;
        }
        return out;
    }

    public AbstractPipeBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.STONE)
                .strength(1f, 10f)
                .noOcclusion()
                .isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    /** Cada tipo de tubería decide a qué bloques se conecta. */
    protected abstract boolean canConnectTo(BlockState neighborState, BlockGetter level, BlockPos neighborPos, Direction direction);

    private boolean shouldConnect(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        return canConnectTo(level.getBlockState(neighborPos), level, neighborPos, dir);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = context.getLevel().getFluidState(pos);

        BlockState state = this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        for (Direction dir : DIRS) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), shouldConnect(level, pos, dir));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                canConnectTo(neighborState, level, neighborPos, direction));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        for (int j = 0; j < 6; j++) {
            if (state.getValue(PROPS[j])) {
                index |= 1 << j;
            }
        }
        return SHAPES[index];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }
}