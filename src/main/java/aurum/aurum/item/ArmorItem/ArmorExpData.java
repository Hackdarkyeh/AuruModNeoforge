package aurum.aurum.item.ArmorItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArmorExpData(
        int currentExp,    // EXP actual
        int currentLevel,  // Nivel actual
        int expToNextLevel // EXP requerido para siguiente nivel
) {
    // Codec para guardar en disco
    public static final Codec<ArmorExpData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("currentExp").forGetter(ArmorExpData::currentExp),
                    Codec.INT.fieldOf("currentLevel").forGetter(ArmorExpData::currentLevel),
                    Codec.INT.fieldOf("expToNextLevel").forGetter(ArmorExpData::expToNextLevel)
            ).apply(instance, ArmorExpData::new)
    );

    // StreamCodec para sincronización en red
    public static final StreamCodec<ByteBuf, ArmorExpData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorExpData::currentExp,
            ByteBufCodecs.VAR_INT, ArmorExpData::currentLevel,
            ByteBufCodecs.VAR_INT, ArmorExpData::expToNextLevel,
            ArmorExpData::new
    );

    // Nueva instancia por defecto
    public static ArmorExpData defaultData() {
        return new ArmorExpData(0, 0, 100); // 100 EXP para primer nivel
    }



    // Añadir EXP y calcular si sube de nivel
    public ArmorExpData addExp(int amount, ArmorTierData tierData) {
        // Ajustar EXP según tier (tiers más altos requieren más EXP)
        double tierFactor = 1.0 + tierData.tierLevel() * 0.5; // +50% más por tier
        int adjustedAmount = (int)(amount / tierFactor);

        int newExp = currentExp + adjustedAmount;
        int newLevel = currentLevel;
        int newExpToNext = expToNextLevel;

        while (newExp >= newExpToNext) {
            newExp -= newExpToNext;
            newLevel++;
            newExpToNext = (int)(newExpToNext * (1.5 + tierData.tierLevel() * 0.1)); // +10% más por tier
        }

        return new ArmorExpData(newExp, newLevel, newExpToNext);
    }
}
