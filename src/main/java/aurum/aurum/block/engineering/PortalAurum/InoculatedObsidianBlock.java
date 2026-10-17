package aurum.aurum.block.engineering.PortalAurum;

import aurum.aurum.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

public class InoculatedObsidianBlock extends AbstractInoculatedObsidianBlock {

    public static final MapCodec<InoculatedObsidianBlock> CODEC = simpleCodec(InoculatedObsidianBlock::new);

    @Override
    public MapCodec<InoculatedObsidianBlock> codec() {
        return CODEC;
    }

    public InoculatedObsidianBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(50.0F, 1200.0F)
                .lightLevel(litBlockEmission(13)));
    }

    public InoculatedObsidianBlock(Properties properties) {
        super(properties);
    }

    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return state -> state.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        // Se mantiene para manejar la lógica interna del bloque (ej. ticks del portal)
        return new InoculatedObsidianBlockEntity(pPos, pState);
    }


// ... dentro de la clase de tu bloque físico (AbstractInoculatedObsidianBlock) ...

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Solo queremos que la lógica pesada de la raid y validación corra en el servidor
        if (level.isClientSide()) {
            return null;
        }

        // Enganchamos el reloj del juego con tu método serverTick
        return createTickerHelper(blockEntityType, ModBlockEntities.INOCULATED_OBSIDIAN_BLOCK_ENTITY.get(),
                AbstractInoculatedObsidianBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        // Lógica de partículas opcional
    }
}