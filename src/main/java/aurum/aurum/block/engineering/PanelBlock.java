package aurum.aurum.block.engineering;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class PanelBlock extends Block {
    // Propiedades adicionales: Luz encendida
    public static final BooleanProperty UPPER_LEFT_LIGHT = BooleanProperty.create("lights_on");
    public static final BooleanProperty UPPER_RIGHT_LIGHT = BooleanProperty.create("lights_on");
    public static final BooleanProperty LOWER_LEFT_LIGHT = BooleanProperty.create("lights_on");
    public static final BooleanProperty LOWER_RIGHT_LIGHT = BooleanProperty.create("lights_on");
    public static final BooleanProperty MIDDLE_LIGHT = BooleanProperty.create("lights_on");

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PanelBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.STONE)
                .strength(1f, 10f)
                .noOcclusion()
                // Controla la emisión de luz
                .lightLevel(state -> state.getValue(UPPER_LEFT_LIGHT) ? 10 : 0) // Emite luz si LIGHTS_ON es true
                .lightLevel(state -> state.getValue(UPPER_RIGHT_LIGHT) ? 10 : 0) // Emite luz si LIGHTS_ON es true
                .lightLevel(state -> state.getValue(LOWER_LEFT_LIGHT) ? 10 : 0) // Emite luz si LIGHTS_ON es true
                .lightLevel(state -> state.getValue(LOWER_RIGHT_LIGHT) ? 10 : 0) // Emite luz si LIGHTS_ON es true
                .lightLevel(state -> state.getValue(MIDDLE_LIGHT) ? 10 : 0) // Emite luz si LIGHTS_ON es true
        );

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UPPER_LEFT_LIGHT, false) //  Máquina incompleta
                .setValue(UPPER_RIGHT_LIGHT, false) // Máquina completa
                .setValue(LOWER_LEFT_LIGHT, false)  // No hay bloque de interés
                .setValue(LOWER_RIGHT_LIGHT, false) // Si hay bloque de interés
                .setValue(MIDDLE_LIGHT, false) // Extrayendo recursos
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // Definir las formas: base y panel
        VoxelShape base = box(2, 0, 2, 14, 6, 14); // Base del bloque
        VoxelShape panel = box(4, 6, 4, 12, 16, 12); // Parte superior del bloque (panel)

        // Combinar las formas
        return Shapes.or(base, panel);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPPER_LEFT_LIGHT, UPPER_RIGHT_LIGHT, LOWER_LEFT_LIGHT, LOWER_RIGHT_LIGHT, MIDDLE_LIGHT, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

        return this.defaultBlockState()
                .setValue(UPPER_LEFT_LIGHT, false) // Por defecto, las luces están apagadas
                .setValue(UPPER_RIGHT_LIGHT, false) // Por defecto, las luces están apagadas
                .setValue(LOWER_LEFT_LIGHT, false) // Por defecto, las luces están apagadas
                .setValue(LOWER_RIGHT_LIGHT, false) // Por defecto, las luces están apagadas
                .setValue(MIDDLE_LIGHT, false) // Por defecto, las luces están apagadas
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        // No rotación para este bloque específico
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
    }
}

