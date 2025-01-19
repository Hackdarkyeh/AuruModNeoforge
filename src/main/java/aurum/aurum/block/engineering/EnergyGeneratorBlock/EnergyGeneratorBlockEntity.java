package aurum.aurum.block.engineering.EnergyGeneratorBlock;

import aurum.aurum.client.gui.EnergyGeneratorMenu;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyGeneratorBlockEntity extends AbstractEnergyGeneratorBlockEntity {
    public EnergyGeneratorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ENERGY_GENERATOR_BLOCK_ENTITY.get(), pPos, pBlockState, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.energy_generator_block");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new EnergyGeneratorMenu(pId, pPlayer, this, this.dataAccess);
    }
}
