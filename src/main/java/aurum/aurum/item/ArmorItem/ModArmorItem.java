package aurum.aurum.item.ArmorItem;

import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor;
import aurum.aurum.init.ModArmorMaterials;
import aurum.aurum.init.ModComponents;
import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModArmorItem extends ArmorItem implements IEnergyArmor {

    private static final Map<Holder<ArmorMaterial>, List<MobEffectInstance>> MATERIAL_TO_EFFECT_MAP =
            (new ImmutableMap.Builder<Holder<ArmorMaterial>, List<MobEffectInstance>>())
                    .put(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC,
                            List.of(new MobEffectInstance(MobEffects.JUMP, 200, 1, false, false),
                                    new MobEffectInstance(MobEffects.HEALTH_BOOST, 200, 4, false, false)))
                    .build();

    public ModArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1)
                .component(ModComponents.ARMOR_TIER.get(), new ArmorTierData(1, true))
                .component(ModComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(0,
                        EnergyConfig.AureliteArmor.MAX_ENERGY_CAPACITY, EnergyType.NONE)));
    }

    // --- Implementación de IEnergyArmor ---

    @Override
    public EnergyType getCurrentEnergyType(ItemStack stack) {
        ArmorEnergyData data = stack.get(ModComponents.ARMOR_ENERGY.get());
        return data != null ? data.energyType() : EnergyType.NONE;
    }

    @Override
    public void setEnergyType(ItemStack stack, EnergyType type) {
        ArmorEnergyData currentData = stack.get(ModComponents.ARMOR_ENERGY.get());
        if (currentData != null) {
            stack.set(ModComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(
                    currentData.currentEnergy(),
                    currentData.maxCapacity(),
                    type
            ));
        }
    }

    @Override
    public int getCurrentEnergy(ItemStack stack) {
        ArmorEnergyData data = stack.get(ModComponents.ARMOR_ENERGY.get());
        return data != null ? data.currentEnergy() : 0;
    }

    @Override
    public int getMaxEnergy(ItemStack stack) {
        ArmorEnergyData data = stack.get(ModComponents.ARMOR_ENERGY.get());
        return data != null ? data.maxCapacity() : EnergyConfig.AureliteArmor.MAX_ENERGY_CAPACITY;
    }

    @Override
    public void setEnergy(ItemStack stack, int amount) {
        ArmorEnergyData currentData = stack.get(ModComponents.ARMOR_ENERGY.get());
        if (currentData != null) {
            int clampedAmount = Math.min(Math.max(0, amount), currentData.maxCapacity());
            stack.set(ModComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(
                    clampedAmount,
                    currentData.maxCapacity(),
                    currentData.energyType()
            ));
        }
    }

    @Override
    public int addEnergy(ItemStack stack, int amount) {
        ArmorEnergyData currentData = stack.get(ModComponents.ARMOR_ENERGY.get());
        if (currentData != null) {
            int energyAdded = Math.min(amount, currentData.maxCapacity() - currentData.currentEnergy());
            int newEnergy = currentData.currentEnergy() + energyAdded;
            stack.set(ModComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(
                    newEnergy,
                    currentData.maxCapacity(),
                    currentData.energyType()
            ));
            return energyAdded;
        }
        return 0;
    }

    @Override
    public boolean canReceiveEnergy(ItemStack stack) {
        return getCurrentEnergy(stack) < getMaxEnergy(stack);
    }

    @Override
    public int getEnergyConsumptionPerTick(ItemStack stack) {
        return switch(getCurrentEnergyType(stack)) {
            case DARK_ENERGY -> (int)(EnergyConfig.AureliteArmor.DARK_ENERGY_CONSUMPTION_PER_TICK *
                    EnergyConfig.AureliteArmor.DARK_ENERGY_DRAIN_MULTIPLIER);
            case CLEAN_ENERGY -> (int)(EnergyConfig.AureliteArmor.CLEAN_ENERGY_CONSUMPTION_PER_TICK *
                    EnergyConfig.AureliteArmor.CLEAN_ENERGY_DRAIN_MULTIPLIER);
            default -> 0;
        };
    }

    // --- Métodos para cargar energía específica ---

    public boolean addDarkEnergy(ItemStack stack, int amount) {
        EnergyType currentType = getCurrentEnergyType(stack);
        if (currentType != EnergyType.NONE && currentType != EnergyType.DARK_ENERGY) {
            return false; // No mezclar energías
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentType == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.DARK_ENERGY);
        }
        return energyAdded > 0;
    }

    public boolean addCleanEnergy(ItemStack stack, int amount) {
        EnergyType currentType = getCurrentEnergyType(stack);
        if (currentType != EnergyType.NONE && currentType != EnergyType.CLEAN_ENERGY) {
            return false; // No mezclar energías
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentType == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.CLEAN_ENERGY);
        }
        return energyAdded > 0;
    }

    // --- Reparación con energía limpia ---

    public boolean repairWithCleanEnergy(ItemStack stack, int cleanEnergyAmount) {
        if (getCurrentEnergyType(stack) != EnergyType.CLEAN_ENERGY) {
            return false;
        }

        int repairCost = cleanEnergyAmount * EnergyConfig.AureliteArmor.ENERGY_REPAIR_AMOUNT;
        if (getCurrentEnergy(stack) >= repairCost) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            setEnergy(stack, getCurrentEnergy(stack) - repairCost);
            return true;
        }
        return false;
    }

    // --- Consumo de energía en tick ---

    public void consumeEnergyPerTick(ItemStack stack, Player player) {
        EnergyType energyType = getCurrentEnergyType(stack);
        if (energyType == EnergyType.NONE || getCurrentEnergy(stack) <= 0) {
            return;
        }

        int consumption = getEnergyConsumptionPerTick(stack);
        if (getCurrentEnergy(stack) >= consumption) {
            setEnergy(stack, getCurrentEnergy(stack) - consumption);

            // Aplicar efectos según el tipo de energía
            applyEnergyEffects(stack, player, energyType);
        } else {
            // Sin energía suficiente, se apaga
            setEnergyType(stack, EnergyType.NONE);
        }
    }

    private void applyEnergyEffects(ItemStack stack, Player player, EnergyType energyType) {
        switch(energyType) {
            case DARK_ENERGY:
                // Bonus de velocidad con energía oscura
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                break;
            case CLEAN_ENERGY:
                // Regeneración con energía limpia
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(EnergyConfig.AureliteArmor.CLEAN_ENERGY_REGENERATION);
                }
                break;
        }
    }

    // --- Método modificado inventoryTick ---

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && !level.isClientSide()) {
            // Consumir energía si está equipada
            if (isEquipped(player, stack)) {
                consumeEnergyPerTick(stack, player);
            }

            if (hasFullSuitOfArmorOn(player)) {
                evaluateArmorEffects(player);
            } else {
                Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(20);
            }
        }
    }

    private boolean isEquipped(Player player, ItemStack stack) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack == stack) {
                return true;
            }
        }
        return false;
    }

    // --- Métodos de tooltip modificados ---

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        ArmorExpData expData = stack.getOrDefault(ModComponents.ARMOR_EXP, ArmorExpData.defaultData());
        ArmorTierData tierData = stack.get(ModComponents.ARMOR_TIER.get());
        ArmorEnergyData energyData = stack.get(ModComponents.ARMOR_ENERGY.get());

        assert tierData != null;
        tooltip.add(Component.literal("")
                .append(Component.translatable("tooltip.aurum.tier").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" ")).append(tierData.getDisplayName()));

        // Información de energía
        if (energyData != null && energyData.currentEnergy() > 0) {
            String energyTypeName = switch(energyData.energyType()) {
                case DARK_ENERGY -> "Oscura";
                case CLEAN_ENERGY -> "Limpia";
                default -> "Sin energía";
            };

            tooltip.add(Component.literal("Energía " + energyTypeName + ": " +
                            energyData.currentEnergy() + " / " + energyData.maxCapacity())
                    .withStyle(getEnergyColor(energyData.energyType())));

            if (energyData.energyType() != EnergyType.NONE) {
                tooltip.add(Component.literal("¡Modo activo: Mayor consumo!").withStyle(ChatFormatting.YELLOW));
            }
        }

        // Añade la línea de nivel
        tooltip.add(Component.literal("")
                .append(Component.translatable("tooltip.armor.level").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" " + expData.currentLevel()).withStyle(getTierColor(expData.currentLevel()))));
    }

    private ChatFormatting getEnergyColor(EnergyType energyType) {
        return switch(energyType) {
            case DARK_ENERGY -> ChatFormatting.DARK_PURPLE;
            case CLEAN_ENERGY -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
    }

    // Métodos existentes sin cambios...
    private void evaluateArmorEffects(Player player) {
        for(Map.Entry<Holder<ArmorMaterial>, List<MobEffectInstance>> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
            Holder<ArmorMaterial> mapArmorMaterial = entry.getKey();
            List<MobEffectInstance> mapEffect = entry.getValue();

            if(hasPlayerCorrectArmorOn(mapArmorMaterial, player)) {
                Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(40);
            }
        }
    }

    private boolean hasPlayerCorrectArmorOn(Holder<ArmorMaterial> mapArmorMaterial, Player player) {
        for(ItemStack armorStack : player.getArmorSlots()) {
            if(!(armorStack.getItem() instanceof ArmorItem)) {
                return false;
            }
        }

        ArmorItem boots = ((ArmorItem) player.getInventory().getArmor(0).getItem());
        ArmorItem leggings = ((ArmorItem) player.getInventory().getArmor(1).getItem());
        ArmorItem chestplate = ((ArmorItem) player.getInventory().getArmor(2).getItem());
        ArmorItem helmet = ((ArmorItem) player.getInventory().getArmor(3).getItem());

        return boots.getMaterial() == mapArmorMaterial && leggings.getMaterial() == mapArmorMaterial
                && chestplate.getMaterial() == mapArmorMaterial && helmet.getMaterial() == mapArmorMaterial;
    }

    private boolean hasFullSuitOfArmorOn(Player player) {
        ItemStack boots = player.getInventory().getArmor(0);
        ItemStack leggings = player.getInventory().getArmor(1);
        ItemStack chestplate = player.getInventory().getArmor(2);
        ItemStack helmet = player.getInventory().getArmor(3);

        return !boots.isEmpty() && !leggings.isEmpty() && !chestplate.isEmpty() && !helmet.isEmpty();
    }

    private ChatFormatting getTierColor(int level) {
        return switch(level) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.GREEN;
            case 2 -> ChatFormatting.BLUE;
            case 3 -> ChatFormatting.GOLD;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    // --- Métodos para UI ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCurrentEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCurrentEnergy(stack) / (float) getMaxEnergy(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return switch(getCurrentEnergyType(stack)) {
            case DARK_ENERGY -> 0x6600CC; // Morado oscuro
            case CLEAN_ENERGY -> 0x33FF99; // Verde claro
            default -> 0x9933FF; // Morado por defecto
        };
    }
}