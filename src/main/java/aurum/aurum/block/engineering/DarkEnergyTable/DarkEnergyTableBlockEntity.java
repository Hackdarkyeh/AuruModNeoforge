package aurum.aurum.block.engineering.DarkEnergyTable;

import aurum.aurum.client.gui.DarkEnergyTable.DarkEnergyTableMenu;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.item.crafting.RecipeType;

import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

public class DarkEnergyTableBlockEntity extends AbstractDarkEnergyTableBlockEntity {
    public DarkEnergyTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.DARK_ENERGY_TABLE_BLOCK.get(), pPos, pBlockState, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dark_energy_table");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new DarkEnergyTableMenu(pId, pPlayer, this, this.dataAccess);
    }
}
