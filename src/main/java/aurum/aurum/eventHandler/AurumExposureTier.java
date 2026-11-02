package aurum.aurum.eventHandler;

/**
 * Tramos de exposición a Aurum (fracción de AurumExposureHandler.MAX_EXPOSURE).
 * Los campos de daño y barrera se usan en los pasos siguientes (efectos y barrera).
 */
public enum AurumExposureTier {
    //          desde   daño+   barrera-  intervalo de daño periódico en ticks (0 = no hay)
    NONE("Sin efectos", 0.00f, 0.00f, 0.00f, 0),
    LOW("Leve", 0.15f, 0.10f, 0.00f, 0),
    MEDIUM("Medio", 0.45f, 0.20f, 0.15f, 0),
    HIGH("Alto", 0.75f, 0.35f, 0.25f, 200),      // 1 de daño cada 10 s
    CRITICAL("Crítico", 0.95f, 0.50f, 0.35f, 100); // 1 de daño cada 5 s

    public final String label;
    /** Fracción de la escala a partir de la cual empieza este tramo. */
    public final float lowerFraction;
    /** Aumento del daño de tipo Aurum recibido (0.10 = +10 %). El +20 % del tramo medio es un valor mío. */
    public final float damageBonus;
    /** Fracción de la capacidad de la Barrera de Corrosión que se pierde (0.15 = 15 %). */
    public final float barrierLoss;
    /** Ticks entre cada punto de daño periódico (0 = sin daño periódico). */
    public final int damageIntervalTicks;

    AurumExposureTier(String label, float lowerFraction, float damageBonus, float barrierLoss, int damageIntervalTicks) {
        this.label = label;
        this.lowerFraction = lowerFraction;
        this.damageBonus = damageBonus;
        this.barrierLoss = barrierLoss;
        this.damageIntervalTicks = damageIntervalTicks;
    }

    public static AurumExposureTier fromExposure(float exposure) {
        float fraction = exposure / AurumExposureHandler.MAX_EXPOSURE;
        AurumExposureTier result = NONE;
        for (AurumExposureTier tier : values()) {
            if (fraction >= tier.lowerFraction) {
                result = tier;
            }
        }
        return result;
    }
}