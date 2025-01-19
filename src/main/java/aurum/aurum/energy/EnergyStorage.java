package aurum.aurum.energy;

import net.minecraft.nbt.CompoundTag;

public class EnergyStorage implements IEnergyStorage {
    private int energyStored;
    private final int maxEnergy;
    private final int transfer;
    private final int maxTransfer;
    private final int maxReceive;

    public EnergyStorage(int maxEnergy, int transfer, int maxTransfer, int maxReceive) {
        this.maxEnergy = maxEnergy;
        this.transfer = transfer;
        this.maxTransfer = maxTransfer;
        this.maxReceive = maxReceive;
        this.energyStored = 0;
    }

    @Override
    public int addEnergy(int amount, boolean simulate) {
        // Calcula la cantidad máxima de energía que se puede añadir
        int energyToAdd = Math.min(amount, this.maxEnergy - this.energyStored);

        // Si no es una simulación, actualiza el almacenamiento de energía
        if (!simulate) {
            this.energyStored += energyToAdd;
        }

        // Devuelve la cantidad de energía que se pudo añadir (o se añadiría en caso de simulación)
        return energyToAdd;
    }


    // Método para consumir energía
    @Override
    public int consumeEnergy(int energy, boolean simulate) {
        // Calcula la cantidad máxima de energía que se puede extraer
        int energyExtracted = Math.min(energyStored, Math.min(maxEnergy, energy));

        if (!simulate) {
            // Actualiza el almacenamiento de energía si no es una simulación
            this.energyStored -= energyExtracted;
        }

        // Devuelve la cantidad de energía que fue extraída (o se extraería en caso de simulación)
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return energyStored;
    }

    @Override
    public int getMaxEnergyStored() {
        return maxEnergy;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public CompoundTag saveEnergyToTag(CompoundTag tag) {
        return null;
    }

    @Override
    public void loadEnergyFromTag(CompoundTag tag) {

    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // Calcular la cantidad de energía que puede ser recibida
        int energyAvailableToReceive = Math.min(this.getMaxEnergyStored() - this.getEnergyStored(), maxReceive);

        // Si es una simulación, solo devolvemos la cantidad que se podría recibir
        if (simulate) {
            return energyAvailableToReceive;
        }

        // Si no es una simulación, efectivamente se recibe la energía
        this.setStoredEnergy(this.getEnergyStored() + energyAvailableToReceive);

        return energyAvailableToReceive;
    }

    @Override
    public void setStoredEnergy(int energyStored) {
        this.energyStored = energyStored;
    }

    @Override
    public int getRemainingCapacity() {
        return maxEnergy - energyStored;
    }
}
