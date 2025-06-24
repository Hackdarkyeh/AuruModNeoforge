package aurum.aurum.item.ArmorItem;

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

public class ModArmorItem extends ArmorItem {
    private static final Map<Holder<ArmorMaterial>, List<MobEffectInstance>> MATERIAL_TO_EFFECT_MAP =
            (new ImmutableMap.Builder<Holder<ArmorMaterial>, List<MobEffectInstance>>())
                    .put(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC,
                            List.of(new MobEffectInstance(MobEffects.JUMP, 200, 1, false, false),
                                    new MobEffectInstance(MobEffects.HEALTH_BOOST, 200, 4, false, false)))
                    .build();

    public ModArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1).component(ModComponents.ARMOR_TIER.get(),
                new ArmorTierData(1, true)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if(entity instanceof Player player && !level.isClientSide() && hasFullSuitOfArmorOn(player)) {
            evaluateArmorEffects(player);
        }else if (entity instanceof Player player && !level.isClientSide()){
            Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(20);
        }
    }

    private void evaluateArmorEffects(Player player) {
        for(Map.Entry<Holder<ArmorMaterial>, List<MobEffectInstance>> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
            Holder<ArmorMaterial> mapArmorMaterial = entry.getKey();
            List<MobEffectInstance> mapEffect = entry.getValue();

            if(hasPlayerCorrectArmorOn(mapArmorMaterial, player)) {
                //addEffectToPlayer(player, mapEffect);
                Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(40);
            }
        }
    }

    private void addEffectToPlayer(Player player, List<MobEffectInstance> mapEffect) {
        boolean hasPlayerEffect = mapEffect.stream().allMatch(effect -> player.hasEffect(effect.getEffect()));

        if(!hasPlayerEffect) {
            for (MobEffectInstance effect : mapEffect) {
                player.addEffect(new MobEffectInstance(effect.getEffect(),
                        effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag); // Mantén los tooltips originales

        ArmorExpData expData = stack.getOrDefault(ModComponents.ARMOR_EXP, ArmorExpData.defaultData());
        ArmorTierData tierData = stack.get(ModComponents.ARMOR_TIER.get());

        assert tierData != null;
        tooltip.add(Component.literal("")
                .append(Component.translatable("tooltip.aurum.tier").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" ")).append(tierData.getDisplayName()));

        // Añade la línea de nivel (aparecerá encima de los encantamientos)
        tooltip.add(Component.literal("")
                .append(Component.translatable("tooltip.armor.level").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" " + expData.currentLevel()).withStyle(getTierColor(expData.currentLevel()))));
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
}
