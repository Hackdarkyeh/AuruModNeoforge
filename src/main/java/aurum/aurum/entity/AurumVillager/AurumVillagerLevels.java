package aurum.aurum.entity.AurumVillager;

import aurum.aurum.init.ModAttachments;

/**
 * Sistema de experiencia común a todos los aldeanos de Aurum.
 * Los niveles y la experiencia necesaria son los de vanilla (1 a 5; 10, 70, 150 y 250 para subir).
 * Qué mejora cada nivel depende de la profesión (ofertas, tarifas, alquiler...) y lo decide cada una.
 */
public final class AurumVillagerLevels {

    public static final int MAX_LEVEL = 5;
    /** Experiencia necesaria para pasar del nivel N al N+1 (índice = N). */
    private static final int[] XP_TO_NEXT_LEVEL = {0, 10, 70, 150, 250};

    private AurumVillagerLevels() {}

    public static int level(AurumVillagerEntity villager) {
        return villager.getData(ModAttachments.VILLAGER_LEVEL).level();
    }

    public static int xp(AurumVillagerEntity villager) {
        return villager.getData(ModAttachments.VILLAGER_LEVEL).xp();
    }

    /** Experiencia total que hace falta para pasar del nivel dado al siguiente (0 si ya está en el máximo). */
    public static int xpForNextLevel(int level) {
        return level >= MAX_LEVEL ? 0 : XP_TO_NEXT_LEVEL[Math.max(1, level)];
    }

    /** Suma experiencia y sube de nivel si toca. Devuelve cuántos niveles ha subido (normalmente 0 o 1). */
    public static int addXp(AurumVillagerEntity villager, int amount) {
        if (amount <= 0) {
            return 0;
        }
        AurumVillagerLevel current = villager.getData(ModAttachments.VILLAGER_LEVEL);
        int level = current.level();
        int xp = current.xp() + amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= XP_TO_NEXT_LEVEL[level]) {
            level++;
            gained++;
        }
        villager.setData(ModAttachments.VILLAGER_LEVEL, new AurumVillagerLevel(level, xp));
        return gained;
    }

    /** Para pruebas: fija nivel y experiencia (la experiencia se pone en el mínimo de ese nivel). */
    public static void setLevel(AurumVillagerEntity villager, int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        int xp = clamped == 1 ? 0 : XP_TO_NEXT_LEVEL[clamped - 1];
        villager.setData(ModAttachments.VILLAGER_LEVEL, new AurumVillagerLevel(clamped, xp));
    }
}
