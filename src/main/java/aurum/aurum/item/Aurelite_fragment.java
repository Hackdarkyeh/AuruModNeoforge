package aurum.aurum.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Fragmento de Aurelita: trozo del lingote de Aurelita (Aurelite_ingot).
 * SIEMPRE lleva el tag de pureza (mismo formato que el lingote: "pureza" en CUSTOM_DATA).
 */
public class Aurelite_fragment extends Item {

    /** Pureza que lleva por defecto cualquier fragmento obtenido sin pasar por un lingote (creativo, /give, loot...). */
    public static final int DEFAULT_PURITY = 0;

    private static CustomData purityData(int pureza) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("pureza", pureza);
        return CustomData.of(tag);
    }

    public Aurelite_fragment() {
        super(new Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON)
                // El tag de pureza viene de serie en el ítem, así que todo ItemStack nuevo ya lo tiene
                .component(DataComponents.CUSTOM_DATA, purityData(DEFAULT_PURITY))
        );
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    // --- PUREZA ---

    /** Crea fragmentos con la pureza indicada. */
    public static ItemStack createWithPurity(Item item, int pureza, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_DATA, purityData(pureza));
        return stack;
    }

    /** Crea fragmentos heredando la pureza de un lingote (para recetas / romper el lingote). */
    public static ItemStack fromIngot(ItemStack ingot, Item fragmentItem, int count) {
        return createWithPurity(fragmentItem, Aurelite_ingot.getPurity(ingot), count);
    }

    public static int getPurity(ItemStack stack) {
        return Aurelite_ingot.getPurity(stack);
    }

    // --- TOOLTIP: la pureza se muestra siempre ---

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.aurum.purity", getPurity(stack)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}