package aurum.aurum.block.engineering.ArmorTable;

import aurum.aurum.client.gui.ArmorTable.ArmorTableMenu;
import aurum.aurum.client.gui.ExtractorBlock.ExtractorMenu;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ArmorTableBlockEntity extends AbstractArmorTableBlockEntity {
    public ArmorTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ARMOR_TABLE_BLOCK.get(), pPos, pBlockState, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.armor_table_block");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new ArmorTableMenu(pId, pPlayer, this, this.dataAccess);
    }
}
