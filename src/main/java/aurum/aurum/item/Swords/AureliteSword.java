package aurum.aurum.item.Swords;

import aurum.aurum.energy.IDarkEnergyWeapon;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AureliteSword extends SwordItem implements IDarkEnergyWeapon {

    // Record para almacenar los datos de energía directamente en el ItemStack
    public record DarkEnergyData(int currentEnergy, int maxCapacity) {
        public static final Codec<DarkEnergyData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("currentEnergy").forGetter(DarkEnergyData::currentEnergy),
                        Codec.INT.fieldOf("maxCapacity").forGetter(DarkEnergyData::maxCapacity)
                ).apply(instance, DarkEnergyData::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, DarkEnergyData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, DarkEnergyData::currentEnergy,
                        ByteBufCodecs.INT, DarkEnergyData::maxCapacity,
                        DarkEnergyData::new
                );
    }

    // DataComponentType registrado para esta espada
    public static final DataComponentType<DarkEnergyData> DARK_ENERGY =
            DataComponentType.<DarkEnergyData>builder()
                    .persistent(DarkEnergyData.CODEC)
                    .networkSynchronized(DarkEnergyData.STREAM_CODEC)
                    .build();

    private final int maxEnergy;

    public AureliteSword(Tier pTier, Properties pProperties, int maxEnergy) {
        super(pTier, pProperties.component(DARK_ENERGY, new DarkEnergyData(0, maxEnergy)));
        this.maxEnergy = maxEnergy;
    }

    // Método para obtener los datos de energía de un ItemStack
    public static DarkEnergyData getEnergyData(ItemStack stack) {
        return stack.getOrDefault(DARK_ENERGY, new DarkEnergyData(0, ((AureliteSword)stack.getItem()).maxEnergy));
    }

    // Método para establecer los datos de energía
    public static void setEnergyData(ItemStack stack, DarkEnergyData data) {
        stack.set(DARK_ENERGY, data);
    }

    // --- Implementación de IDarkEnergyWeapon ---

    @Override
    public int getCurrentDarkEnergy(ItemStack stack) {
        return getEnergyData(stack).currentEnergy();
    }

    @Override
    public int getMaxDarkEnergy(ItemStack stack) {
        return getEnergyData(stack).maxCapacity();
    }

    @Override
    public void setDarkEnergy(ItemStack stack, int amount) {
        DarkEnergyData currentData = getEnergyData(stack);
        setEnergyData(stack, new DarkEnergyData(amount, currentData.maxCapacity()));
    }

    @Override
    public int addDarkEnergy(ItemStack stack, int amount) {
        DarkEnergyData currentData = getEnergyData(stack);
        int energyAdded = Math.min(amount, currentData.maxCapacity() - currentData.currentEnergy());
        int newEnergy = currentData.currentEnergy() + energyAdded;
        setDarkEnergy(stack, newEnergy);
        return energyAdded;
    }

    @Override
    public boolean canReceiveDarkEnergy(ItemStack stack) {
        return getCurrentDarkEnergy(stack) < getMaxDarkEnergy(stack);
    }

    @Override
    public boolean isDarkEnergyWeapon(ItemStack stack) {
        return true;
    }

    // --- Funcionalidad Adicional de la Espada ---

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        // Muestra la barra de durabilidad/energía solo si tiene energía
        return getCurrentDarkEnergy(pStack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        // Calcula el ancho de la barra basado en la energía actual
        return Math.round(13.0F * getCurrentDarkEnergy(pStack) / (float) getMaxDarkEnergy(pStack));
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        // Color morado para la barra de energía
        return 0x9933FF;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(Component.literal("Energía: " + getCurrentDarkEnergy(pStack) + " / " + getMaxDarkEnergy(pStack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        if (pPlayer.isCrouching() && consumeEnergy(stack, 50)) {
            if (!pLevel.isClientSide) {
                pLevel.explode(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), 1.5f, Level.ExplosionInteraction.NONE);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    // Método para consumir energía
    public boolean consumeEnergy(ItemStack stack, int amount) {
        int currentEnergy = getCurrentDarkEnergy(stack);
        if (currentEnergy >= amount) {
            setDarkEnergy(stack, currentEnergy - amount);
            return true;
        }
        return false;
    }
}

