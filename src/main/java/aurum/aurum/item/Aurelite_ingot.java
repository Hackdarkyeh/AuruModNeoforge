package aurum.aurum.item; // Cambia esto por tu paquete real

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public class Aurelite_ingot extends Item {
    public Aurelite_ingot() {
        super(new Properties()
                .stacksTo(1) // No más de una acumulación por stack
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON) // Raridad del ítem
        );
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        // Este ítem no se puede encantar
        return false;
    }

    // --- MÉTODOS DE AYUDA PARA LA PUREZA ---

    /**
     * Escribe el nivel de pureza en un ItemStack nuevo.
     */
    public static ItemStack createWithPurity(Item item, int pureza) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt("pureza", pureza); // Guardamos el valor

        // En 1.21.1, guardamos el NBT dentro del componente CUSTOM_DATA
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /**
     * Lee el nivel de pureza de un ItemStack.
     */
    public static int getPurity(ItemStack stack) {
        // Obtenemos los datos customizados, o vacío si no tiene ninguno
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        // Devuelve el valor guardado, o 0 si no se ha establecido
        return tag.contains("pureza") ? tag.getInt("pureza") : 0;
    }

    // --- MOSTRAR EN LA DESCRIPCIÓN ---

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int pureza = getPurity(stack);

        // Usamos ChatFormatting.GOLD para que salga en color dorado
        tooltipComponents.add(Component.translatable("tooltip.aurum.purity", pureza).withStyle(ChatFormatting.GOLD));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}