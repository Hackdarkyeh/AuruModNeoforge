package aurum.aurum.eventHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ModGameBusClientEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.ENCHANTED_BOOK)) return;

        ItemEnchantments stored = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (Holder<Enchantment> holder : stored.keySet()) {
            holder.unwrapKey().ifPresent(key -> {
                if (key.location().getNamespace().equals(MODID)) {
                    event.getToolTip().add(
                            Component.translatable("enchantment.aurum." + key.location().getPath() + ".desc")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        }
    }
}