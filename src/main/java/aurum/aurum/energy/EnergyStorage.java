package aurum.aurum.energy;

import net.minecraft.nbt.CompoundTag;

public class EnergyStorage {
    private int energy;
    private int maxEnergy;

    public EnergyStorage(int maxEnergy) {
        this.maxEnergy = maxEnergy;
        this.energy = 0;
    }

    // Añadir energía
    public void addEnergy(int amount) {
        this.energy = Math.min(this.energy + amount, this.maxEnergy);
    }

    // Remover energía
    public void removeEnergy(int amount) {
        this.energy = Math.max(this.energy - amount, 0);
    }

    // Obtener la energía actual
    public int getEnergy() {
        return this.energy;
    }

    // Obtener la energía máxima
    public int getMaxEnergy() {
        return this.maxEnergy;
    }

    // Serializar datos (para guardarlos)
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Energy", this.energy);
        return tag;
    }

    // Deserializar datos (para cargarlos)
    public void load(CompoundTag tag) {
        this.energy = tag.getInt("Energy");
    }
}

