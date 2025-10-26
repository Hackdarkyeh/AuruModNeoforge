package aurum.aurum.energy;

public interface IEnergyDevice {
    float getEnergyStored();
    float getEnergyCapacity();
    void receiveEnergy(float amount);
    boolean canReceiveEnergy();

    // Prioridades más altas se atienden primero
    default int getEnergyPriority() {
        return 0; // Prioridad por defecto
    }

    // Tipo de dispositivo para lógica especial
    EnergyDeviceType getDeviceType();

    public enum EnergyDeviceType {
        MACHINE,    // Prioridad alta - necesita energía para funcionar
        STORAGE,    // Prioridad baja - solo almacena sobrantes
        GENERATOR   // No recibe energía
    }
}
