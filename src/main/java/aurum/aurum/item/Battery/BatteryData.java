package aurum.aurum.item.Battery;

import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor.EnergyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BatteryData(float energy, int tier, EnergyType energyType) {

    public static final Codec<BatteryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("energy").forGetter(BatteryData::energy),
            Codec.INT.fieldOf("tier").forGetter(BatteryData::tier),
            Codec.STRING.fieldOf("energyType").forGetter(d -> d.energyType().name())
    ).apply(i, (e, t, type) -> new BatteryData(e, t, EnergyType.valueOf(type))));

    public static final StreamCodec<ByteBuf, BatteryData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BatteryData::energy,
            ByteBufCodecs.VAR_INT, BatteryData::tier,
            ByteBufCodecs.STRING_UTF8, d -> d.energyType().name(),
            (e, t, type) -> new BatteryData(e, t, EnergyType.valueOf(type)));

    public static BatteryData defaultData() { return new BatteryData(0, 1, EnergyType.NONE); }

    public BatteryTier tierData() { return BatteryTier.of(tier); }
    public BatteryData withEnergy(float e, EnergyType t) { return new BatteryData(e, tier, t); }
    public BatteryData withTier(int newTier) { return new BatteryData(energy, newTier, energyType); }
}