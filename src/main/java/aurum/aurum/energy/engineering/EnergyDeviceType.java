package aurum.aurum.energy.engineering;

public enum EnergyDeviceType {
    MACHINE,    // Prioridad alta - necesita energía para funcionar
    STORAGE,    // Prioridad baja - solo almacena sobrantes
    GENERATOR   // No recibe energía
}
