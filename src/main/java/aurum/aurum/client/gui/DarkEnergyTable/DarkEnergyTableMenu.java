package aurum.aurum.client.gui.DarkEnergyTable;

import aurum.aurum.init.GUI.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public class DarkEnergyTableMenu extends AbstractDarkEnergyTableMenu {

    public DarkEnergyTableMenu(int pContainerId, Inventory pPlayerInventory) {
        super(ModMenuType.DARK_ENERGY_TABLE.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory);
    }

    public DarkEnergyTableMenu(int pContainerId, Inventory pPlayerInventory, Container pFurnaceContainer, ContainerData data) {
        super(ModMenuType.DARK_ENERGY_TABLE.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory, pFurnaceContainer, data);
    }
}

