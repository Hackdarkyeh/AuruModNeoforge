package aurum.aurum.client.gui.Player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArmorSkillsData(
        int absorption,   // Pilar 1: Capacidad de Absorción (0 - 10)
        int recovery,     // Pilar 2: Velocidad de Recuperación (0 - 10)
        int magicAmp,     // Pilar 3: Amplificación Mágica (0 - 5)
        int resonance,    // Pilar 4: Resonancia de Impacto (0 - 5)
        int bonusPoints   // Puntos extra (los del item armor_upgrade_point), se suman a los del tier
) {

    // --- LÍMITES MÁXIMOS POR PILAR ---
    public static final int MAX_ABSORPTION = 10;
    public static final int MAX_RECOVERY = 10;
    public static final int MAX_MAGIC_AMP = 5;
    public static final int MAX_RESONANCE = 5;

    /** Tier máximo: los puntos nativos nunca pasan de aquí (1 punto por tier). */
    public static final int MAX_TIER = 3;

    /** Tier de una pieza a partir de su nivel de XP (1 tier cada 10 niveles, con tope en MAX_TIER). */
    public static int tierFromLevel(int level) {
        return Math.min(MAX_TIER, level / 10);
    }

    /** Suma de todos los pilares: más puntos que esto no se pueden gastar nunca. */
    public static final int MAX_TOTAL_POINTS = MAX_ABSORPTION + MAX_RECOVERY + MAX_MAGIC_AMP + MAX_RESONANCE;

    // --- CODECS DE SERIALIZACIÓN (NBT y Red) ---
    public static final Codec<ArmorSkillsData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("absorption").forGetter(ArmorSkillsData::absorption),
                    Codec.INT.fieldOf("recovery").forGetter(ArmorSkillsData::recovery),
                    Codec.INT.fieldOf("magic_amp").forGetter(ArmorSkillsData::magicAmp),
                    Codec.INT.fieldOf("resonance").forGetter(ArmorSkillsData::resonance),
                    // optional: las armaduras ya guardadas (sin este campo) siguen cargando con 0
                    Codec.INT.optionalFieldOf("bonus_points", 0).forGetter(ArmorSkillsData::bonusPoints)
            ).apply(instance, ArmorSkillsData::new)
    );

    public static final StreamCodec<ByteBuf, ArmorSkillsData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorSkillsData::absorption,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::recovery,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::magicAmp,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::resonance,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::bonusPoints,
            ArmorSkillsData::new
    );

    // Validador de rangos para evitar valores negativos o que superen los máximos
    public ArmorSkillsData {
        absorption = Math.max(0, Math.min(MAX_ABSORPTION, absorption));
        recovery = Math.max(0, Math.min(MAX_RECOVERY, recovery));
        magicAmp = Math.max(0, Math.min(MAX_MAGIC_AMP, magicAmp));
        resonance = Math.max(0, Math.min(MAX_RESONANCE, resonance));
        bonusPoints = Math.max(0, bonusPoints);
    }

    /** Constructor de 4 argumentos para no romper el código que ya lo usa (bonusPoints = 0). */
    public ArmorSkillsData(int absorption, int recovery, int magicAmp, int resonance) {
        this(absorption, recovery, magicAmp, resonance, 0);
    }

    public static ArmorSkillsData defaultData() {
        return new ArmorSkillsData(0, 0, 0, 0, 0);
    }

    // --- PUNTOS ---
    public int getSpentPoints() {
        return absorption + recovery + magicAmp + resonance;
    }

    /** Puntos que le quedan por gastar: los del tier + los extra - los ya gastados. */
    public int getAvailablePoints(int tier) {
        return tier + bonusPoints - getSpentPoints();
    }

    // --- CÁLCULOS DE BONIFICACIONES (MATEMÁTICAS DE LOS PILARES) ---

    /** Pilar 1: +10% de capacidad máxima de escudo por nivel */
    public float getAbsorptionBonusMultiplier() {
        return this.absorption * 0.10f;
    }

    /** Pilar 2: +15% de velocidad de recarga por nivel */
    public float getRecoveryBonusMultiplier() {
        return this.recovery * 0.15f;
    }

    /** Pilar 3: Dinámico -> +10% base al nivel 1, y +5% extra por nivel adicional (+15%, +20%, +25%, +30%) */
    public float getMagicEnchantmentMultiplier() {
        if (this.magicAmp <= 0) return 0.0f;
        return 0.10f + (this.magicAmp - 1) * 0.05f;
    }

    /** Pilar 4: -5% de desgaste del escudo por cada impacto recibido */
    public float getResonanceBonusMultiplier() {
        return this.resonance * 0.05f;
    }

    // --- MÉTODOS AUXILIARES (todos conservan bonusPoints) ---

    public ArmorSkillsData withAbsorption(int newVal) {
        return new ArmorSkillsData(newVal, this.recovery, this.magicAmp, this.resonance, this.bonusPoints);
    }

    public ArmorSkillsData withRecovery(int newVal) {
        return new ArmorSkillsData(this.absorption, newVal, this.magicAmp, this.resonance, this.bonusPoints);
    }

    public ArmorSkillsData withMagicAmp(int newVal) {
        return new ArmorSkillsData(this.absorption, this.recovery, newVal, this.resonance, this.bonusPoints);
    }

    public ArmorSkillsData withResonance(int newVal) {
        return new ArmorSkillsData(this.absorption, this.recovery, this.magicAmp, newVal, this.bonusPoints);
    }

    public ArmorSkillsData withBonusPoints(int newVal) {
        return new ArmorSkillsData(this.absorption, this.recovery, this.magicAmp, this.resonance, newVal);
    }
}