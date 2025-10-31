package aurum.aurum.item.ArmorItem;

import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArmorEnergyData(
        int currentEnergy,
        int maxCapacity,
        IEnergyArmor.EnergyType energyType
) {
    public static final Codec<ArmorEnergyData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("currentEnergy").forGetter(ArmorEnergyData::currentEnergy),
                    Codec.INT.fieldOf("maxCapacity").forGetter(ArmorEnergyData::maxCapacity),
                    Codec.STRING.fieldOf("energyType").forGetter(data -> data.energyType.name())
            ).apply(instance, (energy, capacity, type) ->
                    new ArmorEnergyData(energy, capacity, IEnergyArmor.EnergyType.valueOf(type))
            )
    );

    public static final StreamCodec<ByteBuf, ArmorEnergyData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorEnergyData::currentEnergy,
            ByteBufCodecs.VAR_INT, ArmorEnergyData::maxCapacity,
            ByteBufCodecs.STRING_UTF8, data -> data.energyType.name(),
            (energy, capacity, type) -> new ArmorEnergyData(energy, capacity, IEnergyArmor.EnergyType.valueOf(type))
    );

    public static ArmorEnergyData defaultData() {
        return new ArmorEnergyData(0, 5000, IEnergyArmor.EnergyType.NONE);
    }
}
