package aurum.aurum.client.gui.ExtractorBlock;
import aurum.aurum.init.GUI.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public class ExtractorMenu extends AbstractExtractorMenu {
    public ExtractorMenu(int pContainerId, Inventory pPlayerInventory) {
        super(ModMenuType.EXTRACTOR_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory);
    }

    public ExtractorMenu(int pContainerId, Inventory pPlayerInventory, Container pFurnaceContainer, ContainerData pFurnaceData) {
        super(ModMenuType.EXTRACTOR_MENU.get(), RecipeType.SMELTING, pContainerId, pPlayerInventory, pFurnaceContainer, pFurnaceData);
    }
}