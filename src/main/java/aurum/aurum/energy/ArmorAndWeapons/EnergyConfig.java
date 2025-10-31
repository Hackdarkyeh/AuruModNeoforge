package aurum.aurum.energy.ArmorAndWeapons;


public class EnergyConfig {

    // CONFIGURACIÓN PARA AURELITE SWORD
    public static class AureliteSword {
        public static final int MAX_ENERGY_CAPACITY = 1000000;
        public static final int BASE_DAMAGE = 1;
        public static final int DARK_ENERGY_DAMAGE = 20; // -25% daño
        public static final int CLEAN_ENERGY_DAMAGE = 14; // -12.5% daño
        public static final int DARK_ENERGY_CONSUMPTION_PER_HIT = 50;
        public static final int CLEAN_ENERGY_CONSUMPTION_PER_HIT = 25;
        public static final int ENERGY_REPAIR_AMOUNT = 10; // Energía limpia para reparar 1 durabilidad
        public static final float DARK_ENERGY_DRAIN_MULTIPLIER = 1.5f; // +50% consumo
        public static final float CLEAN_ENERGY_DRAIN_MULTIPLIER = 1.2f; // +20% consumo

        // Requisitos para usar energía oscura
        public static final int MIN_PLAYER_LEVEL_DARK_ENERGY = 100;
        public static final float MIN_DARK_ENERGY_PURITY = 0.8f; // 80% pureza mínima
    }

    // CONFIGURACIÓN PARA ARMADURA AURELITE
    public static class AureliteArmor {
        public static final int MAX_ENERGY_CAPACITY = 5000;
        public static final int BASE_DEFENSE = 20;
        public static final int DARK_ENERGY_DEFENSE = 16; // -20% defensa
        public static final int CLEAN_ENERGY_DEFENSE = 18; // -10% defensa
        public static final int DARK_ENERGY_CONSUMPTION_PER_TICK = 2;
        public static final int CLEAN_ENERGY_CONSUMPTION_PER_TICK = 1;
        public static final int ENERGY_REPAIR_AMOUNT = 50; // Energía limpia para reparar 1 durabilidad
        public static final float DARK_ENERGY_DRAIN_MULTIPLIER = 1.4f; // +40% consumo
        public static final float CLEAN_ENERGY_DRAIN_MULTIPLIER = 1.15f; // +15% consumo

        // Bonus/malus por tipo de energía
        public static final float DARK_ENERGY_MOVEMENT_SPEED = 0.1f; // +10% velocidad
        public static final float CLEAN_ENERGY_REGENERATION = 0.01f; // Regeneración pasiva
    }

    // CONFIGURACIÓN GENERAL
    public static class General {
        public static final int ENERGY_TRANSFER_RATE = 1; // Energía por tick que se puede transferir
        public static final int MIN_ENERGY_FOR_OPERATION = 1; // Energía mínima para funcionar
    }
}
