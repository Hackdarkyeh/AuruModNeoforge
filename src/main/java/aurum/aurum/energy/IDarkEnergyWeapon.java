package aurum.aurum.energy;

import net.minecraft.world.item.ItemStack;

public interface IDarkEnergyWeapon {
    int getCurrentDarkEnergy(ItemStack stack);
    int getMaxDarkEnergy(ItemStack stack);
    void setDarkEnergy(ItemStack stack, int amount);
    int addDarkEnergy(ItemStack stack, int amount);
    boolean canReceiveDarkEnergy(ItemStack stack);
    boolean isDarkEnergyWeapon(ItemStack stack);
}

