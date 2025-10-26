package aurum.aurum.energy;

// Para máquinas que consumen energía para funcionar
public interface IEnergyConsumer {
    float getEnergyStored();
    float getEnergyCapacity();
    void receiveEnergy(float amount);
    boolean canReceiveEnergy();
    boolean isActive(); // Si está realizando trabajo
    float getEnergyDemand(); // Cuánta energía necesita para trabajar
}
