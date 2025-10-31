package aurum.aurum.energy.ArmorAndWeapons;

import net.minecraft.world.item.ItemStack;

public interface IEnergyWeapon {
    // Tipos de energía
    enum EnergyType {
        NONE,
        DARK_ENERGY,
        CLEAN_ENERGY
    }

    EnergyType getCurrentEnergyType(ItemStack stack);
    void setEnergyType(ItemStack stack, EnergyType type);

    int getCurrentEnergy(ItemStack stack);
    int getMaxEnergy(ItemStack stack);
    void setEnergy(ItemStack stack, int amount);
    int addEnergy(ItemStack stack, int amount);
    boolean canReceiveEnergy(ItemStack stack);

    // Métodos específicos para cada tipo de energía
    default boolean canUseDarkEnergy(ItemStack stack) {
        return true; // Sobrescribir en implementaciones específicas
    }

    default boolean canUseCleanEnergy(ItemStack stack) {
        return true; // Sobrescribir en implementaciones específicas
    }
}
