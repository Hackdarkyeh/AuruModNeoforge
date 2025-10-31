package aurum.aurum.energy.engineering;

import net.minecraft.nbt.CompoundTag;

public interface IEnergyStorage {

    // Devuelve la cantidad de energía almacenada
    float getEnergyStored();

    // Devuelve la capacidad máxima de almacenamiento de energía
    float getCapacity();

    // Añade energía al almacenamiento, con una opción para simular la operación
    float addEnergy(float energy, boolean simulate);

    // Consume energía del almacenamiento, con una opción para simular la operación
    float consumeEnergy(float energy, boolean simulate);

    // Devuelve la capacidad restante de almacenamiento
    float getRemainingCapacity();


    // Devuelve si el almacenamiento puede recibir energía
    boolean canReceive();

    // Devuelve si el almacenamiento puede extraer energía
    boolean canExtract();

    // Guarda la información de energía en una etiqueta NBT
    CompoundTag saveEnergyToTag(CompoundTag tag);

    // Carga la información de energía desde una etiqueta NBT
    void loadEnergyFromTag(CompoundTag tag);

    float receiveEnergy(float maxReceive, boolean simulate);

    void setStoredEnergy(float energyStored);


}





