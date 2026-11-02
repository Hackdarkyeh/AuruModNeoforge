package aurum.aurum.entity.AurumVillager;

/**
 * Constantes de los contratos (alquiler) de aldeanos. Todo ajustable aquí.
 * Ejemplo base: 5 yanyitos = un día de juego = 24000 ticks.
 */
public final class AurumContractConfig {

    /** Un día de juego en ticks. */
    public static final int TICKS_PER_DAY = 24000;
    /** Yanyitos que cuesta un día de contrato (antes de recargos). */
    public static final int YANYITOS_PER_DAY = 5;
    /** Ticks de contrato que da cada yanyito. */
    public static final int TICKS_PER_YANYITO = TICKS_PER_DAY / YANYITOS_PER_DAY; // 4800 = 4 min

    /** Días máximos que se pueden acumular en un contrato. */
    public static final int MAX_CONTRACT_DAYS = 7;

    /** Máximo de contratados por nivel de alma (2 por nivel, tope 6, alma hasta tier 3). Aún sin aplicar. */
    public static final int MAX_HIRED_PER_SOUL_TIER = 2;
    public static final int MAX_HIRED_TOTAL = 6;

    private AurumContractConfig() {}

    /** Ticks de contrato que compran N yanyitos. */
    public static long ticksFor(int yanyitos) {
        return (long) yanyitos * TICKS_PER_YANYITO;
    }

    /**
     * Yanyitos que hay que pagar por un día de contrato, con el recargo por reputación del pueblo
     * (AurumVillageData.priceMultiplier: +25 % por muerte, tope x3). Siempre se redondea hacia arriba.
     */
    public static int dayPrice(double priceMultiplier) {
        return (int) Math.ceil(YANYITOS_PER_DAY * priceMultiplier);
    }

    /** Profesiones que se pueden contratar (el nini y el banquero no). */
    public static boolean canBeHired(AurumVillagerEntity.Profession profession) {
        return profession == AurumVillagerEntity.Profession.FIGHTER
                || profession == AurumVillagerEntity.Profession.MINER;
    }
}
