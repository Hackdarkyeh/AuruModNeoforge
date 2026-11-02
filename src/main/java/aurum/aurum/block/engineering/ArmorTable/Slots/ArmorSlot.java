package aurum.aurum.block.engineering.ArmorTable.Slots;

import aurum.aurum.client.gui.ArmorTable.AbstractArmorTableMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorSlot extends Slot {
    private final AbstractArmorTableMenu menu;

    public ArmorSlot(AbstractArmorTableMenu pFurnaceMenu, Container pFurnaceContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pFurnaceContainer, pSlot, pXPosition, pYPosition);
        this.menu = pFurnaceMenu;
    }

    /**
     * Slot de carga: acepta cualquier ítem que lleve energía (armaduras, armas, herramientas...).
     */
    @Override
    public boolean mayPlace(ItemStack pStack) {
        return this.menu.isChargeable(pStack);
    }



    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}