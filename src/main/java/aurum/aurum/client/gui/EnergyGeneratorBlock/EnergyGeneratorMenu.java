package aurum.aurum.client.gui.EnergyGeneratorBlock;
import aurum.aurum.init.GUI.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public class EnergyGeneratorMenu extends AbstractEnergyGeneratorMenu {
    public EnergyGeneratorMenu(int pContainerId, Inventory pPlayerInventory) {
        super(ModMenuType.ENERGY_GENERATOR_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory);
    }

    public EnergyGeneratorMenu(int pContainerId, Inventory pPlayerInventory, Container pFurnaceContainer, ContainerData pFurnaceData) {
        super(ModMenuType.ENERGY_GENERATOR_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory, pFurnaceContainer, pFurnaceData);
    }
}