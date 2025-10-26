package aurum.aurum.block.engineering.DarkEnergyTable;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlock;
import aurum.aurum.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

public class DarkEnergyTableBlock extends AbstractDarkEnergyTableBlock {

    public static final MapCodec<DarkEnergyTableBlock> CODEC = simpleCodec(DarkEnergyTableBlock::new);

    @Override
    public MapCodec<DarkEnergyTableBlock> codec() {
        return CODEC;
    }

    public DarkEnergyTableBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13)));
    }

    public DarkEnergyTableBlock(Properties p_53627_) {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13)));
    }

    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return p_50763_ -> p_50763_.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new DarkEnergyTableBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createDarkEnergyTableBlockEntityTicker(pLevel, pBlockEntityType, ModBlockEntities.DARK_ENERGY_TABLE_BLOCK.get()); // Asumiendo que DARK_ENERGY_TABLE_BLOCK se registrará en ModBlockEntities
    }

    @Override
    protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
        if (pLevel.isClientSide) return; // Asegurar que solo corra en el servidor

        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof DarkEnergyTableBlockEntity) {
            // Usar la sobrecarga que toma el escritor (buffer writer)
            // Esto corrige el *NullPointerException* del cliente.
            pPlayer.openMenu((MenuProvider)blockentity, (buffer) -> {
                buffer.writeBlockPos(pPos);
            });
        }
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        // Implementar partículas de energía oscura si el bloque está activo
        // if (pState.getValue(LIT)) {
        //     ... añadir partículas moradas o de energía oscura
        // }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // Forma de la mesa, similar a la ArmorTable o personalizada
        return Shapes.or(box(1, 12, 2, 15, 14, 14), box(4, 0, 4, 12, 2, 12), box(3, 2, 3, 13, 4, 13), box(3, 4, 3, 13, 6, 13), box(3, 6, 3, 13, 8, 13), box(3, 8, 3, 13, 10, 13), box(3, 10, 3, 13, 12, 13));
    }
}

