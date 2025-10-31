package aurum.aurum.client.gui.DarkEnergyTable;

import aurum.aurum.block.engineering.DarkEnergyTable.DarkEnergyTableBlockEntity;
import aurum.aurum.block.engineering.DarkEnergyTable.PurifierSlot;
import aurum.aurum.block.engineering.DarkEnergyTable.WeaponSlot;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyWeapon;
import aurum.aurum.init.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public abstract class AbstractDarkEnergyTableMenu extends AbstractContainerMenu {
    private static final int INGREDIENT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int SLOT_COUNT = 2;
    private static final int DATA_COUNT = 8;

    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    private static final int FLOAT_SCALING_FACTOR = 1000; // Factor de escala


    protected AbstractDarkEnergyTableMenu(
            MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory
    ) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    protected AbstractDarkEnergyTableMenu(
            MenuType<?> pMenuType,
            RecipeType<? extends AbstractCookingRecipe> pRecipeType,
            int pContainerId,
            Inventory pPlayerInventory,
            Container pContainer,
            ContainerData pData
    ) {
        super(pMenuType, pContainerId);
        this.recipeType = pRecipeType;
        checkContainerSize(pContainer, SLOT_COUNT);
        checkContainerDataCount(pData, DATA_COUNT);
        this.container = pContainer;
        this.data = pData;
        this.level = pPlayerInventory.player.level();
        this.addSlot(new PurifierSlot(pPlayerInventory.player, pContainer, DarkEnergyTableBlockEntity.PURIFIER_SLOT, 81, 37)); // Purifier Slot
        this.addSlot(new WeaponSlot(pPlayerInventory.player, pContainer, DarkEnergyTableBlockEntity.WEAPON_SLOT, 145, 37)); // Weapon Slot



        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(pPlayerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(pPlayerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlots(pData);
    }

    /**
     * Determines whether supplied player can use this container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player inventory and the other inventory(s).
     */
    // En tu DarkEnergyTableMenu.java
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Definir los slots de destino
            final int purifierSlot = 0; // Slot del purificador
            final int weaponSlot = 1;   // Slot del arma
            final int playerInventoryStart = 2;
            final int playerInventoryEnd = 38; // 36 slots de inventario + 2 del container

            // Si el click viene del inventario del jugador
            if (index >= playerInventoryStart && index <= playerInventoryEnd) {
                // Intentar mover al slot de purificador
                if (itemstack1.is(ModItems.PURIFIER.get())) {
                    if (!this.moveItemStackTo(itemstack1, purifierSlot, purifierSlot + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Intentar mover al slot de arma
                else if (itemstack1.getItem() instanceof IEnergyWeapon) {
                    if (!this.moveItemStackTo(itemstack1, weaponSlot, weaponSlot + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Si es del hotbar, mover al inventario principal
                else if (index >= playerInventoryStart + 27 && index <= playerInventoryEnd) {
                    if (!this.moveItemStackTo(itemstack1, playerInventoryStart, playerInventoryStart + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Si es del inventario principal, mover al hotbar
                else if (index >= playerInventoryStart && index < playerInventoryStart + 27) {
                    if (!this.moveItemStackTo(itemstack1, playerInventoryStart + 27, playerInventoryEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            // Si el click viene del container, mover al inventario del jugador
            else if (!this.moveItemStackTo(itemstack1, playerInventoryStart, playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    public float getBurnProgress() {
        int i = this.data.get(2);
        int j = this.data.get(3);
        return j != 0 && i != 0 ? Mth.clamp((float)i / (float)j, 0.0F, 1.0F) : 0.0F;
    }

    public float getEnergyProgress() {
        int currentEnergy = this.data.get(6);
        int maxEnergy = this.data.get(7);
        return Mth.clamp((float) currentEnergy / (float) maxEnergy, 0.0F, 1.0F);
    }

    public float getDarkEnergyProgress() {
        int currentEnergy = getDarkEnergyStored();
        int maxEnergy = getDarkEnergyCapacity();
        return Mth.clamp((float) currentEnergy / (float) maxEnergy, 0.0F, 1.0F);
    }


    public float getCleanEnergyProgress() {
        int currentEnergy = getCleanEnergyStored();
        int maxEnergy = getMaxCleanEnergyStored();
        return Mth.clamp((float) currentEnergy / (float) maxEnergy, 0.0F, 1.0F);
    }

    public float getCurrentEnergy() {
        return (float) this.data.get(6);
    }

    public float getMaxEnergy() {
        return (float) this.data.get(7);
    }

    public float getCurrentDarkEnergy() {
        return (float) this.data.get(0);
    }

    public int getDarkEnergyCapacity() {
        return this.data.get(1);
    }

    public float getCurrentCleanEnergy() {
        return (float) this.data.get(2);
    }

    public int getCleanEnergyCapacity() {
        return this.data.get(3);
    }



    public float getLitProgress() {
        int i = this.data.get(1);
        if (i == 0) {
            i = 200;
        }

        return Mth.clamp((float)this.data.get(0) / (float)i, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public int getDarkEnergyStored() {
        return data.get(0);
    }

    public int getMaxDarkEnergyStored() {
        return data.get(1);
    }

    public int getCleanEnergyStored() {
        return data.get(2);
    }

    public int getMaxCleanEnergyStored() {
        return data.get(3);
    }

}
