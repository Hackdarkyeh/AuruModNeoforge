package aurum.aurum.block.engineering.ArmorTable.Slots;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlockEntity;
import aurum.aurum.client.gui.ArmorTable.AbstractArmorTableMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorUpgradeOutPutSlot extends Slot {
    private final Player player;
    private final AbstractArmorTableMenu menu;

    public ArmorUpgradeOutPutSlot(Player player, AbstractArmorTableMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.player = player;
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false; // El jugador no puede meter ítems manualmente aquí
    }

    @Override
    public boolean mayPickup(Player player) {
        // Solo permite recoger si el jugador tiene 30 o más niveles de XP
        return player.experienceLevel >= 30 && this.hasItem();
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        // 1. Restar 30 niveles de experiencia
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-30);
        }

        // 2. Consumir 1 lingote de Aurelita de AURELITE_IN (Slot 1)
        ItemStack aureliteStack = this.container.getItem(AbstractArmorTableMenu.AURELITE_IN);
        if (!aureliteStack.isEmpty()) {
            aureliteStack.shrink(1);
        }

        // 3. Consumir la armadura original de ARMOR_IN (Slot 0)
        this.container.setItem(AbstractArmorTableMenu.ARMOR_IN, ItemStack.EMPTY);

        // 4. Consumir Energía Oscura si la entidad está presente
        if (this.container instanceof AbstractArmorTableBlockEntity tableEntity) {
            tableEntity.darkEnergyStored = Math.max(0, tableEntity.darkEnergyStored - 1000f);
        }

        super.onTake(player, stack);

        // 5. Reevaluar la ranura de salida
        if (this.container instanceof AbstractArmorTableBlockEntity tableEntity) {
            tableEntity.updateOutputSlot();
        }
    }
}