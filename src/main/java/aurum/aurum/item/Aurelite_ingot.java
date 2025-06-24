package aurum.aurum.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class Aurelite_ingot extends Item {
    public Aurelite_ingot() {
        super(new Properties()
                .stacksTo(1) // No más de una acumulación por stack
                .rarity(Rarity.UNCOMMON)// Raridad del ítem
        );
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        // Este ítem no se puede encantar
        return false;
    }
}
