package aurum.aurum.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DarkEnergyItems extends Item {
    // Record para almacenar los datos de energía
    public record DarkEnergyData(int currentEnergy, int maxCapacity) {
        // Codec para persistencia (disco/NBT)
        public static final Codec<DarkEnergyData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("currentEnergy").forGetter(DarkEnergyData::currentEnergy),
                        Codec.INT.fieldOf("maxCapacity").forGetter(DarkEnergyData::maxCapacity)
                ).apply(instance, DarkEnergyData::new)
        );

        // StreamCodec para sincronización en red
        public static final StreamCodec<RegistryFriendlyByteBuf, DarkEnergyData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, DarkEnergyData::currentEnergy,
                        ByteBufCodecs.INT, DarkEnergyData::maxCapacity,
                        DarkEnergyData::new
                );
    }

    // DataComponentType registrado
    public static final DataComponentType<DarkEnergyData> DARK_ENERGY =
            DataComponentType.<DarkEnergyData>builder()
                    .persistent(DarkEnergyData.CODEC)
                    .networkSynchronized(DarkEnergyData.STREAM_CODEC)
                    .build();

    private final int baseCapacity;

    public DarkEnergyItems(Properties properties, int baseCapacity) {
        super(properties);
        this.baseCapacity = baseCapacity;
    }

    // Método para obtener los datos de energía de un ItemStack
    public static DarkEnergyData getEnergyData(ItemStack stack) {
        return stack.getOrDefault(DARK_ENERGY, new DarkEnergyData(0, ((DarkEnergyItems)stack.getItem()).baseCapacity));
    }

    // Método para establecer los datos de energía
    public static void setEnergyData(ItemStack stack, DarkEnergyData data) {
        stack.set(DARK_ENERGY, data);
    }

    // Método para añadir energía
    public static void addEnergy(ItemStack stack, int amount) {
        DarkEnergyData current = getEnergyData(stack);
        int newEnergy = Math.min(current.currentEnergy() + amount, current.maxCapacity());
        setEnergyData(stack, new DarkEnergyData(newEnergy, current.maxCapacity()));
    }

    // Método para consumir energía
    public static boolean consumeEnergy(ItemStack stack, int amount) {
        DarkEnergyData current = getEnergyData(stack);
        if (current.currentEnergy() >= amount) {
            setEnergyData(stack, new DarkEnergyData(current.currentEnergy() - amount, current.maxCapacity()));
            return true;
        }
        return false;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        // Al fabricar/obtener el item, se inicializa con 0 energía
        initializeEnergy(stack, false);
        super.onCraftedBy(stack, level, player);
    }



    private void initializeEnergy(ItemStack stack, boolean isCreative) {
        int initialEnergy = isCreative ? getBaseCapacity() : 0;
        setEnergyData(stack, new DarkEnergyData(initialEnergy, getBaseCapacity()));
    }

    public int getBaseCapacity() {
        return this.baseCapacity;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        // Mostrar barra solo si tiene energía
        return getEnergyData(stack).currentEnergy() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        DarkEnergyData data = getEnergyData(stack);
        return Math.round(13.0F * data.currentEnergy() / data.maxCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Color morado para la barra de energía oscura
        return 0x9933FF;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (getEnergyData(stack).currentEnergy() > 0) {
            // Solo consume energía si tiene
            consumeEnergy(stack, 1);
        }
        // Nunca se rompe por falta de energía
        return true;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false; // Deshabilita reparación normal
    }



    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // Método para cargar energía
    public static void chargeWeapon(ItemStack stack, int amount) {
        DarkEnergyData data = getEnergyData(stack);
        int newEnergy = Math.min(data.currentEnergy() + amount, data.maxCapacity());
        setEnergyData(stack, new DarkEnergyData(newEnergy, data.maxCapacity()));
    }
}