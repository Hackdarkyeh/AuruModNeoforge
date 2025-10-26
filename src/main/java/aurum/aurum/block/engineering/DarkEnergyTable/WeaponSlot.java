package aurum.aurum.block.engineering.DarkEnergyTable;

import aurum.aurum.energy.IDarkEnergyWeapon;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WeaponSlot extends Slot {
    public WeaponSlot(Player pPlayer, Container pContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pContainer, pSlot, pXPosition, pYPosition);
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        return pStack.getItem() instanceof IDarkEnergyWeapon;
    }

    @Override
    public ItemStack remove(int pAmount) {
        return super.remove(pAmount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
    }

    @Override
    public boolean mayPickup(Player pPlayer) {
        // Bloquear la extracción si el jugador tiene menos de 30 niveles
        return pPlayer.experienceLevel >= 30;
    }


    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {
        super.onQuickCraft(pStack, pAmount);
    }
}

