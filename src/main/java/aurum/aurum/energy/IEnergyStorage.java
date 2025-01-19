package aurum.aurum.energy;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.nbt.CompoundTag;

public interface IEnergyStorage {

    // Devuelve la cantidad de energía almacenada
    int getEnergyStored();

    // Devuelve la capacidad máxima de almacenamiento de energía
    int getMaxEnergyStored();

    // Añade energía al almacenamiento, con una opción para simular la operación
    int addEnergy(int energy, boolean simulate);

    // Consume energía del almacenamiento, con una opción para simular la operación
    int consumeEnergy(int energy, boolean simulate);

    // Devuelve la capacidad restante de almacenamiento
    int getRemainingCapacity();


    // Devuelve si el almacenamiento puede recibir energía
    boolean canReceive();

    // Devuelve si el almacenamiento puede extraer energía
    boolean canExtract();

    // Guarda la información de energía en una etiqueta NBT
    CompoundTag saveEnergyToTag(CompoundTag tag);

    // Carga la información de energía desde una etiqueta NBT
    void loadEnergyFromTag(CompoundTag tag);

    int receiveEnergy(int maxReceive, boolean simulate);

    void setStoredEnergy(int energyStored);
}



