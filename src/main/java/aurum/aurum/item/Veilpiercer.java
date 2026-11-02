package aurum.aurum.item;

import aurum.aurum.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class Veilpiercer extends Item {

    /**
     * Durabilidad máxima (provisional, cámbiala a tu gusto). Antes el item no tenía durabilidad, así que
     * "ponerlo a durabilidad 1" no habría hecho nada. Con .durability() el item ya tiene tamaño de pila 1.
     */
    public static final int MAX_DURABILITY = 100;

    public Veilpiercer() {
        super(new Item.Properties()
                .durability(MAX_DURABILITY)
                .rarity(Rarity.UNCOMMON)// Raridad del ítem
        );
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        // Este ítem no se puede encantar
        return false;
    }

    /**
     * Forma de restablecerlo: en el yunque con fragmentos de aurelite (el mismo item que se gana golpeando a la
     * divine_armor mientras canaliza). Es una propuesta: quítalo si prefieres otra vía (mesa de armadura, etc.).
     */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.AURELITE_FRAGMENT.get());
    }
}