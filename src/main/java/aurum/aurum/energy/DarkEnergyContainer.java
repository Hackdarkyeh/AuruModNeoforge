package aurum.aurum.energy;

import net.minecraft.world.item.ItemStack;

public interface DarkEnergyContainer {
    // Métodos comunes para todos los objetos con energía oscura
    int getCurrentDarkEnergy(ItemStack stack);
    int getMaxDarkEnergy();
    void setDarkEnergy(ItemStack stack, int amount);
    default boolean canReceiveDarkEnergy() { return true; } // Opcional: bloques podrían no recibir energía
}
