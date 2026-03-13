package aurum.aurum.client.gui.SoulModificationTableMenu;

import aurum.aurum.item.ArmorItem.ArmorExpansions;
import aurum.aurum.item.SoulExpansions;
import aurum.aurum.tagsProvider.ModTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSoulMenu extends AbstractContainerMenu {
    public static final int SOUL_CORE_SLOT = 0;
    // ⚠️ CORREGIDO: 1 Slot de Núcleo + 8 Slots de Habilidad = 9 Slots totales.
    public static final int SLOT_COUNT = 9;
    private static final int DATA_COUNT = 0; // 0: Peso usado, 1: Alma máxima

    private final Container container;
    private final ContainerData data;
    protected final Player player;

    protected AbstractSoulMenu(
            MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory
    ) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    protected AbstractSoulMenu(
            MenuType<?> pMenuType,
            RecipeType<? extends AbstractCookingRecipe> pRecipeType,
            int pContainerId,
            Inventory pPlayerInventory,
            Container pContainer,
            ContainerData pData
    ) {
        super(pMenuType, pContainerId);
        checkContainerSize(pContainer, SLOT_COUNT);
        checkContainerDataCount(pData, DATA_COUNT);
        this.container = pContainer;
        this.data = pData;
        this.player = pPlayerInventory.player;
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);

        // 🔹 1. Slot del núcleo de alma (central)
        this.addSlot(new Slot(container, SOUL_CORE_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isSoulCore(stack); // Solo permite núcleos de alma
            }
        });

        // 🔹 2. Slots de Expansión (Habilidades) - 8 slots (índices del 1 al 8)
        for (int i = 1; i < SLOT_COUNT; i++) {
            final int slotIndex = i; // Slot ID del contenedor (1 a 8)
            this.addSlot(new Slot(container, slotIndex, getSlotX(slotIndex), getSlotY(slotIndex)) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return isAbility(stack); // Solo permite habilidades
                }
            });
        }

        this.addDataSlots(data);
    }
    // ... (El resto del código se mantiene igual)

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    // 🔹 Método para verificar si un item es una habilidad válida
    public boolean isAbility(ItemStack stack) {
        return stack.is(ModTags.Items.ABILITY_ITEMS); // Solo permite objetos que sean habilidades
    }

    public boolean isSoulCore(ItemStack stack) {
        return stack.is(ModTags.Items.SOUL_ITEMS);
    }
    // 🔹 Método para obtener el peso de una habilidad (personalizable)
    private int getAbilityWeight(ItemStack stack) {
        if (isAbility(stack)) {
            // Asegúrate que el item realmente sea de tipo ArmorExpansions si pasas esta línea.
            return ((ArmorExpansions) stack.getItem()).getWeight();
        }
        return 0;
    }

    // 🔹 Devuelve el peso actual
    public int getCurrentWeight() {
        return data.get(0);
    }

    // 🔹 Devuelve la capacidad máxima de alma
    public int getMaxSoulCapacity() {
        ItemStack soulCore = container.getItem(SOUL_CORE_SLOT);
        if (!soulCore.isEmpty() && isSoulCore(soulCore)) {
            // Asegúrate que el item realmente sea de tipo SoulExpansions.
            return ((SoulExpansions) soulCore.getItem()).getMaxWight();
        }
        return 0;
    }

    // 🔹 Devuelve el progreso del peso en porcentaje
    public float getWeightProgress() {
        // Asegúrate de no dividir por cero
        int maxCapacity = getMaxSoulCapacity();
        if (maxCapacity == 0) return 0.0F;
        return Mth.clamp((float) getCurrentWeight() / (float) maxCapacity, 0.0F, 1.0F);
    }

    // 🔹 Métodos para posicionar los slots de habilidades en forma de estrella
    // Estos índices (1-8) corresponden a los slots de Habilidad
    private int getSlotX(int index) {
        // ⚠️ AJUSTADO: Se asume que el índice 1 es el primer slot de habilidad.
        // Los arreglos deben tener 9 entradas (para slots 0 a 8) o 8 entradas (para slots 1 a 8).
        // Si quieres usar el índice del slot (1-8), debes ignorar el índice 0 del arreglo.
        // Ejemplo: Posiciones para slots 1 a 8 (circular/estrella)
        int[] positionsX = {0, 50, 70, 100, 130, 150, 130, 100, 70};
        return positionsX[index];
    }

    private int getSlotY(int index) {
        // Ejemplo: Posiciones para slots 1 a 8 (circular/estrella)
        int[] positionsY = {0, 30, 10, 5, 10, 30, 50, 60, 50};
        return positionsY[index];
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        // Implementación básica del Shift-Click
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            // Rango de slots del menú: 0 a 8 (9 slots)
            int menuStart = 0;
            int menuEnd = SLOT_COUNT; // 9

            // Rango de slots del inventario del jugador: 9 a 44 (36 slots)
            int playerInventoryStart = menuEnd; // 9
            int playerHotbarEnd = menuEnd + 36; // 45

            if (index < menuEnd) {
                // Item de la mesa a Inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, playerInventoryStart, playerHotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Item del Inventario del jugador a la mesa

                // Slot 0 es SOUL_CORE
                if (isSoulCore(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, SOUL_CORE_SLOT, SOUL_CORE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Slots 1-8 son ABILITY_SLOTS
                else if (isAbility(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, SOUL_CORE_SLOT + 1, menuEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Otro item: Mover dentro del inventario del jugador (ej. a la Hotbar)
                else if (index < playerHotbarEnd - 9) {
                    if (!this.moveItemStackTo(stackInSlot, playerHotbarEnd - 9, playerHotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Item en la Hotbar: Mover al inventario principal
                else if (index < playerHotbarEnd) {
                    if (!this.moveItemStackTo(stackInSlot, playerInventoryStart, playerHotbarEnd - 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }
        return originalStack;
    }

}