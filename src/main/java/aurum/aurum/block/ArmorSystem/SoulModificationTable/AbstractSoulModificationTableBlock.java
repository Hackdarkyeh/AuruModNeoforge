package aurum.aurum.block.ArmorSystem.SoulModificationTable;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

// Clase abstracta para la Mesa de Modificación de Alma
public abstract class AbstractSoulModificationTableBlock extends BaseEntityBlock {
    // Propiedad de dirección (NORTE, SUR, ESTE, OESTE)
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Propiedad de encendido/apagado
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // Constructor protegido, define propiedades por defecto
    protected AbstractSoulModificationTableBlock(Properties pProperties) {
        super(pProperties);
        // Estado por defecto: mirando al norte y apagado
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, Boolean.valueOf(false)));
    }

    // Constructor alternativo (no se usa normalmente, revisar si es necesario)
    public AbstractSoulModificationTableBlock(BlockEntityType<SoulModificationTableBlockEntity> soulModificationTableBlockEntityBlockEntityType, BlockPos blockPos, BlockState blockState) {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13)));
    }

    // Función para determinar la emisión de luz según el estado LIT
    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return p_50763_ -> p_50763_.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
    }

    // Método abstracto para el codec (serialización)
    @Override
    protected abstract MapCodec<? extends AbstractSoulModificationTableBlock> codec();

    // Método abstracto para abrir el contenedor de la mesa
    protected abstract void openContainer(Level pLevel, BlockPos pPos, Player pPlayer);

    // Devuelve el estado del bloque al colocarlo, orientado según el jugador
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    // Lógica al eliminar el bloque
    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof AbstractSoulModificationTableBlockEntity) {
                if (pLevel instanceof ServerLevel) {
                    // Suelta el contenido y otorga experiencia
                    Containers.dropContents(pLevel, pPos, (AbstractSoulModificationTableBlockEntity)blockentity);
                    ((AbstractSoulModificationTableBlockEntity)blockentity).getRecipesToAwardAndPopExperience((ServerLevel)pLevel, Vec3.atCenterOf(pPos));
                }

                super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
                pLevel.updateNeighbourForOutputSignal(pPos, this);
            } else {
                super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            }
        }
    }

    // Indica si el bloque tiene señal de salida analógica (para comparadores)
    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    // Devuelve la señal analógica del bloque
    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
    }

    // Define la forma de renderizado del bloque
    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    // Rotación del bloque
    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    // Espejado del bloque
    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    // Define las propiedades del estado del bloque
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, LIT);
    }

    // Ticker para la entidad del bloque (actualización en servidor)
    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createFurnaceTicker(
            Level pLevel, BlockEntityType<T> pServerType, BlockEntityType<? extends AbstractSoulModificationTableBlockEntity> pClientType
    ) {
        return pLevel.isClientSide ? null : createTickerHelper(pServerType, pClientType, AbstractSoulModificationTableBlockEntity::serverTick);
    }
}
