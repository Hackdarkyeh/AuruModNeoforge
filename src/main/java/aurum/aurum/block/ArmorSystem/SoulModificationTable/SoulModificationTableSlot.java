package aurum.aurum.block.ArmorSystem.SoulModificationTable;

import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;

public class SoulModificationTableSlot {
    private final Vec3i index;
    private ItemStack itemStack;

    public SoulModificationTableSlot(Vec3i index, ItemStack itemStack) {
        this.index = index;
        this.itemStack = itemStack;
    }

    public Vec3i getIndex() {
        return index;
    }

    public ItemStack getStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }


}
