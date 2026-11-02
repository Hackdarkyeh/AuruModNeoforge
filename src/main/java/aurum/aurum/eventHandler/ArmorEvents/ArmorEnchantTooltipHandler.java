package aurum.aurum.eventHandler.ArmorEvents;

import aurum.aurum.Aurum;
import aurum.aurum.client.gui.Player.ArmorSkillsData;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.AurumArmor;
import aurum.aurum.item.ArmorItem.ArmorMagicBonus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Tooltip: "Protección ~~IV~~ V" (nivel original tachado + nivel mejorado por Magia en dorado). */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ArmorEnchantTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!AurumArmor.is(stack)) return;

        ArmorSkillsData skills = stack.get(ModDataComponents.ARMOR_SKILLS.get());
        if (skills == null || skills.magicAmp() <= 0) return;

        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        for (var entry : enchantments.entrySet()) {
            var holder = entry.getKey();
            int level = entry.getIntValue();
            int boosted = ArmorMagicBonus.boostedLevel(holder, level, skills);
            if (boosted <= level) continue;

            // Línea que añade vanilla para este encantamiento
            String vanillaLine = Enchantment.getFullname(holder, level).getString();

            for (int i = 0; i < tooltip.size(); i++) {
                if (!tooltip.get(i).getString().equals(vanillaLine)) continue;

                MutableComponent line = holder.value().description().copy()
                        .withStyle(holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY);
                line.append(CommonComponents.SPACE)
                        .append(Component.translatable("enchantment.level." + level)
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH))
                        .append(CommonComponents.SPACE)
                        .append(Component.translatable("enchantment.level." + boosted)
                                .withStyle(ChatFormatting.GOLD));

                tooltip.set(i, line);
                break;
            }
        }
    }
}