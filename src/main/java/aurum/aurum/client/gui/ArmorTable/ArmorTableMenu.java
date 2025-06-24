package aurum.aurum.client.gui.ArmorTable;
import aurum.aurum.init.GUI.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public class ArmorTableMenu extends AbstractArmorTableMenu {
    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory) {
        super(ModMenuType.ARMOR_TABLE_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory);
    }

    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory, Container pFurnaceContainer, ContainerData pFurnaceData) {
        super(ModMenuType.ARMOR_TABLE_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory, pFurnaceContainer, pFurnaceData);
    }
}