package aurum.aurum.item.ArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Núcleo de mejora de armadura: en un yunque, junto a una pieza autorizada,
 * le añade 1 punto de habilidad. La lógica está en ArmorUpgradePointAnvilHandler.
 */
public class ArmorUpgradePointItem extends Item {

    public ArmorUpgradePointItem() {
        super(new Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.aurum.armor_upgrade_point").withStyle(ChatFormatting.GRAY));
    }
}