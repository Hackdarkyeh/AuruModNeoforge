package aurum.aurum.block.engineering.ExtractorBlock.Slots;

import aurum.aurum.client.gui.ExtractorBlock.AbstractExtractorMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExtractorRangeSlot extends Slot {
    private final AbstractExtractorMenu menu;

    public ExtractorRangeSlot(AbstractExtractorMenu pFurnaceMenu, Container pFurnaceContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pFurnaceContainer, pSlot, pXPosition, pYPosition);
        this.menu = pFurnaceMenu;
    }

    /**
     * Verifica si el stack está permitido para ser colocado en este slot.
     * Se utiliza para slots de armadura, así como para combustible de hornos.
     */
    @Override
    public boolean mayPlace(ItemStack pStack) {
        return this.menu.isRangeExtractor(pStack);
    }



    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}
