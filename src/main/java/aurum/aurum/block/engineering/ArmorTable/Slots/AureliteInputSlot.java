package aurum.aurum.block.engineering.ArmorTable.Slots;

import aurum.aurum.init.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AureliteInputSlot extends Slot {

    public AureliteInputSlot(Container pContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pContainer, pSlot, pXPosition, pYPosition);
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        // Solo permite colocar lingotes de Aurelite en esta ranura
        return pStack.is(ModItems.AURELITE_INGOT.get());
    }
}