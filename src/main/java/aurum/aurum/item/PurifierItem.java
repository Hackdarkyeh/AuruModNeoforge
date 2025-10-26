package aurum.aurum.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PurifierItem extends Item {
    public PurifierItem(Properties pProperties) {
        // Definimos durabilidad máxima, por ejemplo 500
        super(pProperties.durability(500));
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        // Solo brilla si aún tiene "energía" (no está completamente roto)
        // Cuanto menor sea el daño, más "energía" tiene
        return pStack.getDamageValue() < pStack.getMaxDamage();
    }
}

