package aurum.aurum.block.engineering.EnergyStorageBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyStorageBlock extends Block implements EntityBlock {

    public EnergyStorageBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
