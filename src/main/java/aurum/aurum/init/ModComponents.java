package aurum.aurum.init;

import aurum.aurum.item.ArmorItem.ArmorEnergyData;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.item.ArmorItem.ArmorTierData;
import aurum.aurum.item.Swords.AureliteSword;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static aurum.aurum.Aurum.MODID;

public class ModComponents {
    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents
            (Registries.DATA_COMPONENT_TYPE, MODID);

    public static final Codec<ArmorTierData> TIER_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(0, 2).fieldOf("tierLevel").forGetter(ArmorTierData::tierLevel), // value1
                    Codec.BOOL.fieldOf("isUpgradable").forGetter(ArmorTierData::isUpgradable)      // value2
            ).apply(instance, ArmorTierData::new)
    );

    public static final StreamCodec<ByteBuf, ArmorTierData> TIER_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorTierData::tierLevel,      // value1
            ByteBufCodecs.BOOL, ArmorTierData::isUpgradable,      // value2
            ArmorTierData::new
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorTierData>> ARMOR_TIER =
            REGISTRAR.registerComponentType("armor_tier", builder -> builder
                    .persistent(TIER_CODEC)
                    .networkSynchronized(TIER_STREAM_CODEC)
            );

    public static void register(IEventBus bus) {
        REGISTRAR.register(bus);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorExpData>> ARMOR_EXP =
            REGISTRAR.registerComponentType("armor_exp", builder -> builder
                    .persistent(ArmorExpData.CODEC)
                    .networkSynchronized(ArmorExpData.STREAM_CODEC)
            );
    // En ModComponents.java
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorEnergyData>> ARMOR_ENERGY =
            REGISTRAR.register("armor_energy",
                    () -> DataComponentType.<ArmorEnergyData>builder()
                            .persistent(ArmorEnergyData.CODEC)
                            .networkSynchronized(ArmorEnergyData.STREAM_CODEC)
                            .build());


    // En ModComponents.java
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AureliteSword.EnergyData>> SWORD_ENERGY_DATA =
            REGISTRAR.register("sword_energy",
                    () -> DataComponentType.<AureliteSword.EnergyData>builder()
                            .persistent(AureliteSword.EnergyData.CODEC)
                            .networkSynchronized(AureliteSword.EnergyData.STREAM_CODEC)
                            .build());
}
