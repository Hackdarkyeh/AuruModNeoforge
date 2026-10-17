package aurum.aurum.block.engineering.EnergyStorageBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

public class EnergyStorageBlock extends Block implements EntityBlock {

    // true = hay otro EnergyStorageBlock en esa dirección (solo afecta al modelo)
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN);

    public EnergyStorageBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), level.getBlockState(pos.relative(dir)).is(this));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), neighborState.is(this));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() == this ? true : super.skipRendering(state, adjacentBlockState, side);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state); // Devuelve una nueva instancia de tu BlockEntity
    }

    @Override
    public void onPlace(BlockState newState, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(newState, level, pos, oldState, isMoving);

        if (level.isClientSide || newState.is(oldState.getBlock())) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EnergyStorageBlockEntity newBlock)) {
            return;
        }

        // Buscar un bloque maestro adyacente válido
        EnergyStorageBlockEntity adjacentMaster = findAdjacentMaster(level, pos);

        if (adjacentMaster != null) {
            // Unirse a la red existente
            joinExistingNetwork(newBlock, adjacentMaster, pos);
        } else {
            // Crear nueva red
            initializeNewNetwork(newBlock, pos);
        }
    }

    private EnergyStorageBlockEntity findAdjacentMaster(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            // Verificar que la posición es válida y el chunk está cargado
            if (!level.isLoaded(neighborPos)) {
                continue;
            }

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof EnergyStorageBlockEntity neighbor) {
                // Usar getBlockPos() en lugar de worldPosition
                BlockPos masterPos = neighbor.isMaster ? neighbor.getBlockPos() : neighbor.masterPos;
                if (masterPos != null && level.isLoaded(masterPos)) {
                    BlockEntity masterBe = level.getBlockEntity(masterPos);
                    if (masterBe instanceof EnergyStorageBlockEntity master) {
                        return master;
                    }
                }
            }
        }
        return null;
    }

    private void joinExistingNetwork(EnergyStorageBlockEntity newBlock, EnergyStorageBlockEntity master, BlockPos newPos) {
        // Configurar el nuevo bloque como esclavo
        newBlock.isMaster = false;
        newBlock.masterPos = master.getBlockPos();
        newBlock.mergedBlocks.clear();
        newBlock.energyStorage.setStoredEnergy(0);

        // Añadir a la red del maestro
        master.mergedBlocks.add(newPos);

        // Recalcular la capacidad total
        int additionalCapacity = 10000; // Capacidad por bloque adicional
        float newCapacity = master.energyStorage.getCapacity() + additionalCapacity;


        master.energyStorage.setMaxEnergyStored(newCapacity);

        // Actualizar toda la red
        master.updateNetwork();
    }

    private void initializeNewNetwork(EnergyStorageBlockEntity newBlock, BlockPos pos) {
        // Configurar como nueva red independiente
        newBlock.isMaster = true;
        newBlock.masterPos = pos;
        newBlock.mergedBlocks.clear();
        newBlock.mergedBlocks.add(pos);
        newBlock.energyStorage.setMaxEnergyStored(10000); // Capacidad base
        newBlock.energyStorage.setStoredEnergy(0); // Energía inicial cero
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyStorageBlockEntity) {
                ((EnergyStorageBlockEntity) be).onBlockRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof EnergyStorageBlockEntity) {
                ((EnergyStorageBlockEntity) be).tick(lvl, pos, st, (EnergyStorageBlockEntity) be);
            }
        };
    }
}