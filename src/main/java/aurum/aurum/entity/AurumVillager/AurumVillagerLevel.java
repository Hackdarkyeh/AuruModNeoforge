package aurum.aurum.entity.AurumVillager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Nivel y experiencia de un aldeano de Aurum (todos los aldeanos tienen uno). Va como attachment del aldeano. */
public record AurumVillagerLevel(int level, int xp) {

    public static final AurumVillagerLevel DEFAULT = new AurumVillagerLevel(1, 0);

    public static final Codec<AurumVillagerLevel> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("level").forGetter(AurumVillagerLevel::level),
            Codec.INT.fieldOf("xp").forGetter(AurumVillagerLevel::xp)
    ).apply(inst, AurumVillagerLevel::new));
}
