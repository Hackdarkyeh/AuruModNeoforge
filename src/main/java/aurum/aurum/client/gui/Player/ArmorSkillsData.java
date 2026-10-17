package aurum.aurum.client.gui.Player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArmorSkillsData(
        int absorption,  // Pilar 1: Capacidad de Absorción (0 - 10)
        int recovery,    // Pilar 2: Velocidad de Recuperación (0 - 10)
        int magicAmp,    // Pilar 3: Amplificación Mágica (0 - 5)
        int resonance    // Pilar 4: Resonancia de Impacto (0 - 5)
) {

    // --- LÍMITES MÁXIMOS POR PILAR ---
    public static final int MAX_ABSORPTION = 10;
    public static final int MAX_RECOVERY = 10;
    public static final int MAX_MAGIC_AMP = 5;
    public static final int MAX_RESONANCE = 5;

    // --- CODECS DE SERIALIZACIÓN (NBT y Red) ---
    public static final Codec<ArmorSkillsData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("absorption").forGetter(ArmorSkillsData::absorption),
                    Codec.INT.fieldOf("recovery").forGetter(ArmorSkillsData::recovery),
                    Codec.INT.fieldOf("magic_amp").forGetter(ArmorSkillsData::magicAmp),
                    Codec.INT.fieldOf("resonance").forGetter(ArmorSkillsData::resonance)
            ).apply(instance, ArmorSkillsData::new)
    );

    public static final StreamCodec<ByteBuf, ArmorSkillsData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorSkillsData::absorption,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::recovery,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::magicAmp,
            ByteBufCodecs.VAR_INT, ArmorSkillsData::resonance,
            ArmorSkillsData::new
    );

    // Validador de rangos para evitar valores negativos o que superen los máximos
    public ArmorSkillsData {
        absorption = Math.max(0, Math.min(MAX_ABSORPTION, absorption));
        recovery = Math.max(0, Math.min(MAX_RECOVERY, recovery));
        magicAmp = Math.max(0, Math.min(MAX_MAGIC_AMP, magicAmp));
        resonance = Math.max(0, Math.min(MAX_RESONANCE, resonance));
    }

    public static ArmorSkillsData defaultData() {
        return new ArmorSkillsData(0, 0, 0, 0);
    }

    // --- PUNTOS TOTALES GASTADOS ---
    public int getSpentPoints() {
        return absorption + recovery + magicAmp + resonance;
    }

    // --- CÁLCULOS DE BONIFICACIONES (MATEMÁTICAS DE LOS PILARES) ---

    /**
     * Pilar 1: +10% de capacidad máxima de escudo por nivel
     */
    public float getAbsorptionBonusMultiplier() {
        return this.absorption * 0.10f;
    }

    /**
     * Pilar 2: +15% de velocidad de recarga por nivel
     */
    public float getRecoveryBonusMultiplier() {
        return this.recovery * 0.15f;
    }

    /**
     * Pilar 3: Dinámico -> +10% base al nivel 1, y +5% extra por nivel adicional (+15%, +20%, +25%, +30%)
     */
    public float getMagicEnchantmentMultiplier() {
        if (this.magicAmp <= 0) return 0.0f;
        return 0.10f + (this.magicAmp - 1) * 0.05f;
    }

    /**
     * Pilar 4: -5% de desgaste del escudo por cada impacto recibido
     */
    public float getResonanceBonusMultiplier() {
        return this.resonance * 0.05f;
    }

    // --- MÉTODOS AUXILIARES PARA INCREMENTAR PILARES SEGURAMENTE ---

    public ArmorSkillsData withAbsorption(int newVal) {
        return new ArmorSkillsData(newVal, this.recovery, this.magicAmp, this.resonance);
    }

    public ArmorSkillsData withRecovery(int newVal) {
        return new ArmorSkillsData(this.absorption, newVal, this.magicAmp, this.resonance);
    }

    public ArmorSkillsData withMagicAmp(int newVal) {
        return new ArmorSkillsData(this.absorption, this.recovery, newVal, this.resonance);
    }

    public ArmorSkillsData withResonance(int newVal) {
        return new ArmorSkillsData(this.absorption, this.recovery, this.magicAmp, newVal);
    }
}