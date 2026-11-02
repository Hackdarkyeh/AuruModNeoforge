package aurum.aurum.eventHandler.ArmorEvents;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.item.ArmorItem.ArmorTierData;
import aurum.aurum.item.ArmorItem.AurumArmor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/** Tier, Energía y Nivel en el tooltip de cualquier pieza del mod (Aurelite o armadura autorizada). */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ArmorTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!AurumArmor.is(stack)) return;

        ArmorExpData exp = stack.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());
        List<Component> top = new ArrayList<>();

        top.add(Component.literal("")
                .append(Component.translatable("tooltip.aurum.tier").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(new ArmorTierData(exp.currentLevel() / 10 + 1, true).getDisplayName()));

        if (stack.has(ModDataComponents.ARMOR_ENERGY.get())) {
            String typeName = switch (AurumArmor.getType(stack)) {
                case DARK_ENERGY -> " Oscura";
                case CLEAN_ENERGY -> " Limpia";
                default -> "";
            };
            ChatFormatting color = switch (AurumArmor.getType(stack)) {
                case DARK_ENERGY -> ChatFormatting.DARK_PURPLE;
                case CLEAN_ENERGY -> ChatFormatting.AQUA;
                default -> ChatFormatting.GRAY;
            };
            top.add(Component.literal("Energía" + typeName + ": " + AurumArmor.getEnergy(stack)
                    + " / " + AurumArmor.getMaxEnergy(stack)).withStyle(color));
        }

        ChatFormatting levelColor = switch (exp.currentLevel() / 10) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.GREEN;
            case 2 -> ChatFormatting.BLUE;
            case 3 -> ChatFormatting.GOLD;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
        top.add(Component.literal("")
                .append(Component.translatable("tooltip.armor.level").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" " + exp.currentLevel()).withStyle(levelColor)));

        // Justo después del nombre del ítem
        List<Component> tip = event.getToolTip();
        tip.addAll(Math.min(1, tip.size()), top);
    }
}