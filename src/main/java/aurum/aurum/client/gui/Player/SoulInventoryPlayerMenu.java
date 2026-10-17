package aurum.aurum.client.gui.Player;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.Red.SyncSoulContainerPayload;
import aurum.aurum.eventHandler.SoulAbilityHandler;
import aurum.aurum.init.GUI.ModMenuType;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.SoulExpansions;
import net.minecraft.server.level.ServerPlayer; // <-- IMPORTANTE
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SoulInventoryPlayerMenu extends AbstractContainerMenu {
    private final Container soulItemContainer = new SimpleContainer(1);
    private final Player player;

    public SoulInventoryPlayerMenu(int containerId, Inventory playerInv) {
        super(ModMenuType.SOUL_MODIFICATION_PLAYER_MENU.get(), containerId);
        this.player = playerInv.player;

        // El slot del alma (Slot 0)
        this.addSlot(new Slot(this.soulItemContainer, 0, 80, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SoulExpansions;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!player.level().isClientSide) {
                    ItemStack stack = getItem();
                    if (stack.isEmpty()) {
                        // Slot vacío -> limpiar alma
                        actualizarAlmaDelJugador(ItemStack.EMPTY); // Usamos el método centralizado
                        SoulAbilityHandler.removeAllModifiers(player);
                    } else {
                        // ---------- CÓDIGO NUEVO CORREGIDO (maxHonor añadido) ----------
                        if (!stack.has(ModDataComponents.SOUL_COMPONENT.get())) {
                            if (stack.getItem() instanceof SoulExpansions expansion) {
                                // Pasamos los 4 parámetros requeridos tras tu última actualización de SoulData
                                stack.set(ModDataComponents.SOUL_COMPONENT.get(),
                                        SoulData.createEmpty(expansion.getIdTier(), expansion.getHonor(), expansion.getMaxWight(), expansion.getMaxHonor()));
                            }
                        }

                        actualizarAlmaDelJugador(stack);
                        if (SoulAbilityHandler.hasAuthorizedArmor(player)) {
                            SoulAbilityHandler.applyAllAbilities(player);
                        }
                    }
                }
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (!player.level().isClientSide()) {
                    actualizarAlmaDelJugador(ItemStack.EMPTY); // Sincroniza que se retiró
                    SoulAbilityHandler.removeAllModifiers(player);
                }
            }
        });

        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
        cargarAlmaActual();
    }

    // Centralizamos aquí la actualización y la sincronización de red
    private void actualizarAlmaDelJugador(ItemStack stack) {
        // 1. Crear el nuevo contenedor wrapper
        SoulData.PlayerSoulContainer nuevoContenedor = new SoulData.PlayerSoulContainer(stack.copy());

        // 2. Guardar en los datos del servidor
        player.setData(ModAttachments.ACTIVE_SOUL, nuevoContenedor);

        // 3. NUEVO: Si estamos en el servidor, disparamos el paquete de red al cliente
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new SyncSoulContainerPayload(nuevoContenedor));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    public ItemStack getSoulItemStack() {
        ItemStack fromSlot = soulItemContainer.getItem(0);
        if (!fromSlot.isEmpty()) return fromSlot;
        return player.getData(ModAttachments.ACTIVE_SOUL).soulStack();
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public void handleSlotClick(int slotIndex, ItemStack expansionStack) {
        // Lógica de validación
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36 && !this.moveItemStackTo(itemstack1, 0, 27, false)) {
                return ItemStack.EMPTY;
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

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    private void cargarAlmaActual() {
        SoulData.PlayerSoulContainer container = player.getData(ModAttachments.ACTIVE_SOUL);
        ItemStack savedStack = container.soulStack();

        if (!savedStack.isEmpty()) {
            this.soulItemContainer.setItem(0, savedStack.copy());
        }
    }
}