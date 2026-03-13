package aurum.aurum.client.gui.SoulModificationTableMenu;
import aurum.aurum.init.GUI.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public class SoulModificationTableMenu extends AbstractSoulMenu {
    public SoulModificationTableMenu(int pContainerId, Inventory pPlayerInventory) {
        super(ModMenuType.SOUL_MODIFICATION_TABLE.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory);
    }

    public SoulModificationTableMenu(int pContainerId, Inventory pPlayerInventory, Container pFurnaceContainer, ContainerData pFurnaceData) {
        super(ModMenuType.SOUL_MODIFICATION_TABLE.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory, pFurnaceContainer, pFurnaceData);
    }
}