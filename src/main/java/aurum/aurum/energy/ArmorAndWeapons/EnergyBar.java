package aurum.aurum.energy.ArmorAndWeapons;

/** Regla común de la barra de energía de armas y armaduras: solo se ve cuando falta energía. */
public final class EnergyBar {
    private EnergyBar() {}

    /** Visible si falta energía (vacía incluida); con la energía al máximo se oculta. */
    public static boolean isVisible(int current, int max) {
        return max > 0 && current < max;
    }

    public static int width(int current, int max) {
        return max <= 0 ? 0 : Math.round(13.0F * current / (float) max);
    }
}