package aurum.aurum.energy.ArmorAndWeapons;

import net.minecraft.world.item.ItemStack;

public interface IEnergyArmor {
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

    // Consumo de energía por tick cuando está activa
    int getEnergyConsumptionPerTick(ItemStack stack);
}
