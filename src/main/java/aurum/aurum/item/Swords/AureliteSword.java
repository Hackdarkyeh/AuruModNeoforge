// En AureliteSword.java - versión simplificada
package aurum.aurum.item.Swords;

import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyWeapon;
import aurum.aurum.init.ModComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class AureliteSword extends SwordItem implements IEnergyWeapon {

    public record EnergyData(int currentEnergy, int maxCapacity, EnergyType energyType) {
        public static final Codec<EnergyData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("currentEnergy").forGetter(EnergyData::currentEnergy),
                        Codec.INT.fieldOf("maxCapacity").forGetter(EnergyData::maxCapacity),
                        Codec.STRING.fieldOf("energyType").forGetter(data -> data.energyType.name())
                ).apply(instance, (energy, capacity, type) ->
                        new EnergyData(energy, capacity, EnergyType.valueOf(type))
                )
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, EnergyData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, EnergyData::currentEnergy,
                        ByteBufCodecs.INT, EnergyData::maxCapacity,
                        ByteBufCodecs.STRING_UTF8, data -> data.energyType.name(),
                        (energy, capacity, type) -> new EnergyData(energy, capacity, EnergyType.valueOf(type))
                );
    }

    public AureliteSword(Tier pTier, Properties pProperties) {
        super(pTier, pProperties.component(ModComponents.SWORD_ENERGY_DATA.get(),
                new EnergyData(0, EnergyConfig.AureliteSword.MAX_ENERGY_CAPACITY, EnergyType.NONE)));
    }

    // Método para obtener los datos de energía
    public static EnergyData getEnergyData(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SWORD_ENERGY_DATA.get(),
                new EnergyData(0, EnergyConfig.AureliteSword.MAX_ENERGY_CAPACITY, EnergyType.NONE));
    }

    // Método para establecer los datos de energía
    public static void setEnergyData(ItemStack stack, EnergyData data) {
        stack.set(ModComponents.SWORD_ENERGY_DATA.get(), data);
    }

    // --- Implementación de IEnergyWeapon ---
    @Override
    public EnergyType getCurrentEnergyType(ItemStack stack) {
        return getEnergyData(stack).energyType();
    }

    @Override
    public void setEnergyType(ItemStack stack, EnergyType type) {
        EnergyData currentData = getEnergyData(stack);
        setEnergyData(stack, new EnergyData(
                currentData.currentEnergy(),
                currentData.maxCapacity(),
                type
        ));
    }

    @Override
    public int getCurrentEnergy(ItemStack stack) {
        return getEnergyData(stack).currentEnergy();
    }

    @Override
    public int getMaxEnergy(ItemStack stack) {
        return getEnergyData(stack).maxCapacity();
    }

    @Override
    public void setEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        int clampedAmount = Math.min(Math.max(0, amount), currentData.maxCapacity());
        setEnergyData(stack, new EnergyData(
                clampedAmount,
                currentData.maxCapacity(),
                currentData.energyType()
        ));
    }

    @Override
    public int addEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        int energyAdded = Math.min(amount, currentData.maxCapacity() - currentData.currentEnergy());
        int newEnergy = currentData.currentEnergy() + energyAdded;
        setEnergyData(stack, new EnergyData(
                newEnergy,
                currentData.maxCapacity(),
                currentData.energyType()
        ));
        return energyAdded;
    }

    @Override
    public boolean canReceiveEnergy(ItemStack stack) {
        return getCurrentEnergy(stack) < getMaxEnergy(stack);
    }

    @Override
    public boolean canUseDarkEnergy(ItemStack stack) {
        return true;
    }

    // --- Métodos para cargar energía específica ---
    public boolean addDarkEnergy(ItemStack stack, int amount) {
        return addDarkEnergy(stack, amount, null);
    }

    public boolean addDarkEnergy(ItemStack stack, int amount, @Nullable Player player) {
        if (player != null && (!canUseDarkEnergy(stack) || player.experienceLevel < EnergyConfig.AureliteSword.MIN_PLAYER_LEVEL_DARK_ENERGY)) {
            return false;
        }

        EnergyData currentData = getEnergyData(stack);
        if (currentData.energyType() != EnergyType.NONE && currentData.energyType() != EnergyType.DARK_ENERGY) {
            return false;
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentData.energyType() == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.DARK_ENERGY);
        }
        return energyAdded > 0;
    }

    public boolean addCleanEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        if (currentData.energyType() != EnergyType.NONE && currentData.energyType() != EnergyType.CLEAN_ENERGY) {
            return false;
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentData.energyType() == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.CLEAN_ENERGY);
        }
        return energyAdded > 0;
    }

    // --- Consumo de energía en uso ---
    public boolean consumeEnergyForAttack(ItemStack stack) {
        EnergyData data = getEnergyData(stack);
        if (data.energyType() == EnergyType.NONE || data.currentEnergy() <= 0) {
            return true; // Puede atacar sin energía
        }

        int consumption = switch(data.energyType()) {
            case DARK_ENERGY -> (int)(EnergyConfig.AureliteSword.DARK_ENERGY_CONSUMPTION_PER_HIT *
                    EnergyConfig.AureliteSword.DARK_ENERGY_DRAIN_MULTIPLIER);
            case CLEAN_ENERGY -> (int)(EnergyConfig.AureliteSword.CLEAN_ENERGY_CONSUMPTION_PER_HIT *
                    EnergyConfig.AureliteSword.CLEAN_ENERGY_DRAIN_MULTIPLIER);
            default -> 0;
        };

        if (data.currentEnergy() >= consumption) {
            setEnergy(stack, data.currentEnergy() - consumption);
            return true;
        } else {
            setEnergyType(stack, EnergyType.NONE);
            return false;
        }
    }

    // --- Métodos sobreescritos para UI ---
    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return getCurrentEnergy(pStack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round(13.0F * getCurrentEnergy(pStack) / (float) getMaxEnergy(pStack));
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return switch(getCurrentEnergyType(pStack)) {
            case DARK_ENERGY -> 0x6600CC;
            case CLEAN_ENERGY -> 0x33FF99;
            default -> 0x9933FF;
        };
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);

        EnergyData data = getEnergyData(pStack);
        String energyTypeName = switch(data.energyType()) {
            case DARK_ENERGY -> "Oscura";
            case CLEAN_ENERGY -> "Limpia";
            default -> "Sin energía";
        };

        pTooltipComponents.add(Component.literal("Energía " + energyTypeName + ": " +
                data.currentEnergy() + " / " + data.maxCapacity()));

        // Mostrar daño actual
        float currentDamage = switch(data.energyType()) {
            case DARK_ENERGY -> EnergyConfig.AureliteSword.DARK_ENERGY_DAMAGE;
            case CLEAN_ENERGY -> EnergyConfig.AureliteSword.CLEAN_ENERGY_DAMAGE;
            default -> EnergyConfig.AureliteSword.BASE_DAMAGE;
        };

        pTooltipComponents.add(Component.literal("Daño: " + currentDamage)
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        if (data.energyType() != EnergyType.NONE) {
            pTooltipComponents.add(Component.literal("¡Modo activo: Mayor consumo!")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        EnergyData data = getEnergyData(stack);

        if (pPlayer.isCrouching() && data.currentEnergy() >= 50 && data.energyType() != EnergyType.NONE) {
            if (consumeEnergyForAttack(stack)) {
                if (!pLevel.isClientSide) {
                    pLevel.explode(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                            data.energyType() == EnergyType.DARK_ENERGY ? 2.0f : 1.5f,
                            Level.ExplosionInteraction.NONE);
                }
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}