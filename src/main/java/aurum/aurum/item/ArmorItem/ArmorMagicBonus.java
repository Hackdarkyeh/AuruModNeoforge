package aurum.aurum.item.ArmorItem;

import aurum.aurum.client.gui.Player.ArmorSkillsData;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Único sitio donde se calcula el nivel de un encantamiento mejorado por el pilar Magia.
 * El tooltip (y más adelante el efecto real) deben usar siempre este método para no desincronizarse.
 *
 * Regla actual: Magia 3-4 suma +1 nivel, Magia 5 suma +2, con tope en V.
 * Si más adelante quieres un trato distinto por encantamiento, se cambia solo aquí.
 */
public final class ArmorMagicBonus {
    private ArmorMagicBonus() {}

    /** Tope de nivel al que puede llegar un encantamiento mejorado. */
    public static final int MAX_BOOSTED_LEVEL = 5;

    /** Niveles enteros que suma el pilar Magia a cada encantamiento: 0-2 → +0, 3-4 → +1, 5 → +2. */
    public static int bonusLevels(int magicAmp) {
        if (magicAmp >= 5) return 2;
        if (magicAmp >= 3) return 1;
        return 0;
    }

    /**
     * Nivel resultante del encantamiento. Nunca baja del original.
     * Los encantamientos de un solo nivel (Reparación, Afinidad acuática...) no se tocan.
     */
    public static int boostedLevel(Holder<Enchantment> enchantment, int level, ArmorSkillsData skills) {
        if (enchantment.value().getMaxLevel() <= 1) return level;

        int extra = bonusLevels(skills.magicAmp());
        if (extra <= 0) return level;

        return Math.max(level, Math.min(level + extra, MAX_BOOSTED_LEVEL));
    }
}