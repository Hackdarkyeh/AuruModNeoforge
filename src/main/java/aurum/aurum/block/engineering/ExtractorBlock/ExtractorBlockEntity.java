package aurum.aurum.block.engineering.ExtractorBlock;

import aurum.aurum.client.gui.ExtractorBlock.ExtractorMenu;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class ExtractorBlockEntity extends AbstractExtractorBlockEntity {
    public ExtractorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.EXTRACTOR_BLOCK_ENTITY.get(), pPos, pBlockState, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.extractor_block");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new ExtractorMenu(pId, pPlayer, this, this.dataAccess);
    }
}
