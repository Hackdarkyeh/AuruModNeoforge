package aurum.aurum.item.ArmorItem;

import aurum.aurum.Aurum;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * Una pieza del mod (Aurelite o armadura autorizada) SIN energía deja de proteger: se le quitan los
 * atributos de armadura, dureza y resistencia al retroceso. Se decide pieza a pieza.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorProtectionHandler {

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        // Solo piezas del mod, y solo si se han quedado sin energía
        if (!AurumArmor.is(stack) || AurumArmor.hasProtectionPower(stack)) {
            return;
        }

        event.removeIf(entry -> {
            var attribute = entry.attribute().value();
            return attribute == Attributes.ARMOR.value()
                    || attribute == Attributes.ARMOR_TOUGHNESS.value()
                    || attribute == Attributes.KNOCKBACK_RESISTANCE.value();
        });
    }
}