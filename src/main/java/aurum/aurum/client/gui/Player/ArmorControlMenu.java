package aurum.aurum.client.gui.Player;

import aurum.aurum.init.GUI.ModMenuType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorControlMenu extends AbstractContainerMenu {
    public final Player player;
    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ArmorControlMenu(int containerId, Inventory playerInv) {
        super(ModMenuType.ARMOR_MODIFICATION_PLAYER_MENU.get(), containerId);
        this.player = playerInv.player;

        // 1. Añadimos los 4 slots de Armadura del jugador (ID 39 a 36 del inventario interno)
        for (int k = 0; k < 4; ++k) {
            final EquipmentSlot equipmentslot = SLOT_IDS[k];
            int slotIndex = 39 - k; // Corregido: 39 (Cabeza) a 36 (Pies)

            this.addSlot(new Slot(playerInv, slotIndex, 8, 8 + k * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(equipmentslot, player);
                }

                @Override
                public boolean mayPickup(Player playerIn) {
                    return super.mayPickup(playerIn);
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    ResourceLocation emptyIcon = switch (equipmentslot) {
                        case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                        case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                        case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                        case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
                        default -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                    };
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
                }
            });
        }

        // 2. Inventario principal del jugador (Slots 4 a 30)
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInv, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }

        // 3. Hotbar (Slots 31 a 39 en el ContainerMenu)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Si el ítem está en las casillas de Armadura (0 a 3) -> Mover al inventario (4 a 40)
            if (index < 4) {
                if (!this.moveItemStackTo(itemstack1, 4, 40, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Si está en el inventario -> Intentar colocar en la casilla de armadura adecuada
                boolean movedToArmor = false;
                for (int k = 0; k < 4; k++) {
                    EquipmentSlot targetSlot = SLOT_IDS[k];
                    if (itemstack1.canEquip(targetSlot, player)) {
                        Slot armorSlot = this.slots.get(k);
                        if (!armorSlot.hasItem() && this.moveItemStackTo(itemstack1, k, k + 1, false)) {
                            movedToArmor = true;
                            break;
                        }
                    }
                }

                // Si no era pieza equipable de armadura o las casillas estaban llenas
                if (!movedToArmor) {
                    if (index >= 4 && index < 31) {
                        if (!this.moveItemStackTo(itemstack1, 31, 40, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 31 && index < 40) {
                        if (!this.moveItemStackTo(itemstack1, 4, 31, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}