package aurum.aurum.item.EnergyGeneratorTier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class EnergyGeneratorUpdaterTier4 extends Item {

    public EnergyGeneratorUpdaterTier4() {
        super(new Item.Properties()
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

