package aurum.aurum.item.ArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorExpansions extends Item {
    private final String name;
    private final int weight;

    public ArmorExpansions(int peso, String name) {
        super(new Properties());
        this.weight = peso;
        this.name = name;
    }

    public String getName() { return name; }
    public int getWeight() { return weight; }

    public ResourceLocation getId() {
        return BuiltInRegistries.ITEM.getKey(this);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        String expansionKey = "ability.aurum.expansion.weight";

        pTooltipComponents.add(Component.translatable(expansionKey, weight).withStyle(ChatFormatting.GRAY));

    }
}
