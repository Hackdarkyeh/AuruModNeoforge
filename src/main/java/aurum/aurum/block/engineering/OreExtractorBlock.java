package aurum.aurum.block.engineering;

import aurum.aurum.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import static aurum.aurum.init.ModBlocks.PIPE_BLOCK;

public class OreExtractorBlock extends Block {
    private final int energyPerPipe = 100;
    private final int energyPerExtraction = 200;

    public OreExtractorBlock() {
        super(BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops());
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // Solo ejecuta esto en el lado del servidor
        if (!level.isClientSide) {
            // Busca el bloque de almacenamiento de energía cercano (por ejemplo, justo al lado)
            IEnergyStorage energyStorage = findAdjacentEnergyStorage(level, pos);
            ResourceStorageBlock resourceStorage = findAdjacentResourceStorage(level, pos);

            if (energyStorage != null && resourceStorage != null) {
                // Comprueba si hay un mineral debajo antes de colocar la tubería
                if (isMineralBelow(level, pos)) {
                    // Intenta consumir energía para colocar la tubería
                    if (energyStorage.consumeEnergy(energyPerPipe, false) > 0) {
                        // Coloca la tubería hacia abajo y verifica si llegó al mineral
                        boolean reachedMineral = PipePlacer.placePipesDownwards(level, pos, PIPE_BLOCK.get().defaultBlockState());

                        // Consume energía para extraer el mineral solo si la tubería ha llegado al mineral
                        if (reachedMineral && energyStorage.consumeEnergy(energyPerExtraction, false) > 0) {
                            // Simula la extracción de un mineral (por ejemplo, un diamante)
                            ItemStack extractedMineral = new ItemStack(Items.DIAMOND, 1);
                            resourceStorage.addResource(extractedMineral);
                        }
                    }
                }
            }
        }
    }

    // Comprueba si hay un bloque de mineral directamente debajo del extractor
    private boolean isMineralBelow(Level level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        // Aquí puedes añadir más minerales si lo deseas
        return belowState.getBlock() == Blocks.DIAMOND_ORE || belowState.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE;
    }

    // Encuentra un bloque de almacenamiento de energía adyacente
    private IEnergyStorage findAdjacentEnergyStorage(Level level, BlockPos pos) {
        // Busca en las 6 direcciones alrededor del bloque
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState state = level.getBlockState(adjacentPos);
            if (state.getBlock() instanceof IEnergyStorage) {
                return (IEnergyStorage) state.getBlock();
            }
        }
        return null;
    }

    // Encuentra un bloque de almacenamiento de recursos adyacente
    private ResourceStorageBlock findAdjacentResourceStorage(Level level, BlockPos pos) {
        // Busca en las 6 direcciones alrededor del bloque
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState state = level.getBlockState(adjacentPos);
            if (state.getBlock() instanceof ResourceStorageBlock) {
                return (ResourceStorageBlock) state.getBlock();
            }
        }
        return null;
    }
}
