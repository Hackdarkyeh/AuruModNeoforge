package aurum.aurum.block.ArmorSystem.SoulModificationTable;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.ToIntFunction;

public class SoulModificationTableBlock extends AbstractSoulModificationTableBlock {

    public static final MapCodec<SoulModificationTableBlock> CODEC = simpleCodec(SoulModificationTableBlock::new);


    @Override
    public MapCodec<SoulModificationTableBlock> codec() {
        return CODEC;
    }

    public SoulModificationTableBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13)));
    }
    public SoulModificationTableBlock(Properties p_53627_) {
        super(Properties.of()
                .mapColor(MapColor.STONE)
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
        return new SoulModificationTableBlockEntity(pPos, pState);
    }

    /**
     * Called to open this furnace's container.
     *
     * @see #//use
     */
    @Override
    protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof SoulModificationTableBlockEntity) {
            pPlayer.openMenu((MenuProvider)blockentity);
            //pPlayer.awardStat(ModStats.INTERACT_WITH_ENERGY_GENERATOR);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level.getBlockEntity(pos) instanceof SoulModificationTableBlockEntity soulModificationTableBlockEntity) {
            if(player.isCrouching() && !level.isClientSide()) {

                BlockEntity blockentity = level.getBlockEntity(pos);
                if (blockentity instanceof SoulModificationTableBlockEntity) {
                    player.openMenu((MenuProvider)blockentity, pos);
                    //pPlayer.awardStat(ModStats.INTERACT_WITH_ENERGY_GENERATOR);
                }

                //((ServerPlayer) player).openMenu(new SimpleMenuProvider(soulModificationTableBlockEntity, Component.literal("Table")), pos);
                return ItemInteractionResult.SUCCESS;
            }

            if(soulModificationTableBlockEntity.inventory.getStackInSlot(0).isEmpty() && !stack.isEmpty()) {
                soulModificationTableBlockEntity.inventory.insertItem(0, stack.copy(), false);
                stack.shrink(1);
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
            } else if(stack.isEmpty()) {
                ItemStack stackOnPedestal = soulModificationTableBlockEntity.inventory.extractItem(0, 1, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, stackOnPedestal);
                soulModificationTableBlockEntity.clearContents();
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 1f);
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

}

