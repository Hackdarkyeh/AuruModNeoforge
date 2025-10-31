package aurum.aurum.energy.engineering;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;

public class EnergyStorage implements IEnergyStorage {
    private float energyStored;
    private float maxCapacity;
    private final int transfer;
    private final int maxTransfer;
    private final int maxReceive;

    public EnergyStorage(int maxEnergy, int transfer, int maxTransfer, int maxReceive) {
        this.maxCapacity = maxEnergy;
        this.transfer = transfer;
        this.maxTransfer = maxTransfer;
        this.maxReceive = maxReceive;
        this.energyStored = 0;
    }

    public void setMaxEnergyStored(float maxEnergyStored) {
        this.maxCapacity = maxEnergyStored;
    }

    @Override
    public float addEnergy(float amount, boolean simulate) {
        // Calcula la cantidad máxima de energía que se puede añadir
        float energyToAdd = Math.min(amount, this.maxCapacity - this.energyStored);

        // Si no es una simulación, actualiza el almacenamiento de energía
        if (!simulate) {
            this.energyStored += energyToAdd;
        }

        // Devuelve la cantidad de energía que se pudo añadir (o se añadiría en caso de simulación)
        return energyToAdd;
    }

    @Override
    public float consumeEnergy(float energy, boolean simulate) {
        // Calcula la cantidad máxima de energía que se puede extraer
        float energyExtracted = Math.min(energyStored, Math.min(maxCapacity, energy));

        if (!simulate) {
            // Actualiza el almacenamiento de energía si no es una simulación
            this.energyStored -= energyExtracted;
        }

        // Devuelve la cantidad de energía que fue extraída (o se extraería en caso de simulación)
        return energyExtracted;
    }

    @Override
    public float getEnergyStored() {
        return energyStored;
    }

    @Override
    public float getCapacity() {
        return maxCapacity;
    }

    @Override
    public boolean canReceive() {
        return energyStored < maxCapacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public CompoundTag saveEnergyToTag(CompoundTag tag) {
        // Si el tag es null, creamos uno nuevo
        if (tag == null) {
            tag = new CompoundTag();
        }

        // Guardamos todos los valores importantes
        tag.putFloat("Energy", this.energyStored);
        tag.putFloat("MaxEnergy", this.maxCapacity);
        tag.putInt("Transfer", this.transfer);
        tag.putInt("MaxTransfer", this.maxTransfer);
        tag.putInt("MaxReceive", this.maxReceive);

        return tag;
    }

    @Override
    public void loadEnergyFromTag(CompoundTag tag) {
        if (tag == null) return;

        // Cargamos los valores guardados
        this.energyStored = tag.getFloat("Energy");
        this.maxCapacity = tag.getFloat("MaxEnergy");

        // Estos valores son final, pero los cargamos por si acaso
        // (en caso de que se use reflexión o similar)
        try {
            Field transferField = this.getClass().getDeclaredField("transfer");
            transferField.setAccessible(true);
            transferField.setInt(this, tag.getInt("Transfer"));

            Field maxTransferField = this.getClass().getDeclaredField("maxTransfer");
            maxTransferField.setAccessible(true);
            maxTransferField.setInt(this, tag.getInt("MaxTransfer"));

            Field maxReceiveField = this.getClass().getDeclaredField("maxReceive");
            maxReceiveField.setAccessible(true);
            maxReceiveField.setInt(this, tag.getInt("MaxReceive"));
        } catch (Exception e) {
            System.err.println("Failed to load final fields from NBT: " + e.getMessage());
        }
    }

    @Override
    public float receiveEnergy(float maxReceive, boolean simulate) {
        // Calcular la cantidad de energía que puede ser recibida
        float energyAvailableToReceive = Math.min(this.getCapacity() - this.getEnergyStored(), maxReceive);

        // Si es una simulación, solo devolvemos la cantidad que se podría recibir
        if (simulate) {
            return energyAvailableToReceive;
        }

        // Si no es una simulación, efectivamente se recibe la energía
        this.setStoredEnergy(this.getEnergyStored() + energyAvailableToReceive);

        return energyAvailableToReceive;
    }

    @Override
    public void setStoredEnergy(float energyStored) {
        this.energyStored = energyStored;
    }

    @Override
    public float getRemainingCapacity() {
        return maxCapacity - energyStored;
    }
}
