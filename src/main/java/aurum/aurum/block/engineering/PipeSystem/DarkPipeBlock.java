package aurum.aurum.block.engineering.PipeSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class DarkPipeBlock extends AbstractPipeBlock {

    public DarkPipeBlock() {
        super();
    }

    @Override
    protected boolean canConnectTo(BlockState neighborState, BlockGetter level, BlockPos neighborPos, Direction direction) {
        // Se conecta a otras tuberías oscuras.
        // Para conectar también a máquinas, añade:
        //
        return neighborState.getBlock() instanceof DarkPipeBlock || level.getBlockEntity(neighborPos) instanceof PipeNetworkUtils.IEnergyDevice;
    }
}