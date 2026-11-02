package aurum.aurum.block.engineering.ArmorTable.Slots;

import aurum.aurum.init.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot del purificador de la ArmorTable. Solo admite el purificador y exige 30 niveles para sacarlo. */
public class PurifierInputSlot extends Slot {

    public PurifierInputSlot(Container pContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pContainer, pSlot, pXPosition, pYPosition);
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        return pStack.is(ModItems.PURIFIER.get());
    }

    @Override
    public boolean mayPickup(Player pPlayer) {
        // Bloquear la extracción si el jugador tiene menos de 30 niveles
        return pPlayer.experienceLevel >= 30;
    }
}