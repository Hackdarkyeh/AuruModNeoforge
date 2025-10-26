package aurum.aurum.client.gui.DarkEnergyTable;

import aurum.aurum.block.engineering.DarkEnergyTable.DarkEnergyTableBlockEntity;
import aurum.aurum.block.engineering.DarkEnergyTable.PurifierSlot;
import aurum.aurum.block.engineering.DarkEnergyTable.WeaponSlot;
import aurum.aurum.block.engineering.EnergyGeneratorBlock.Slots.EnergyGeneratorFuelSlot;
import aurum.aurum.block.engineering.EnergyGeneratorBlock.Slots.EnergyGeneratorUpdaterSlot;
import aurum.aurum.init.ModBlocks;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public abstract class AbstractDarkEnergyTableMenu extends AbstractContainerMenu {
    public static final int INGREDIENT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int SLOT_COUNT = 2;
    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    private static final int FLOAT_SCALING_FACTOR = 1000; // Factor de escala


    protected AbstractDarkEnergyTableMenu(
            MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory
    ) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(2), new SimpleContainerData(8));
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
        checkContainerDataCount(pData, 8);
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
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            int inventoryStart = SLOT_COUNT; // Índice inicial del inventario
            int inventoryEnd = this.slots.size(); // Tamaño del inventario

            if (pIndex == RESULT_SLOT) { // Slot de salida
                if (!this.moveItemStackTo(itemstack1, inventoryStart, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (pIndex != FUEL_SLOT && pIndex != INGREDIENT_SLOT ) { // Slots normales
                if (this.canSmelt(itemstack1)) { // Si se puede fundir
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.isFuel(itemstack1)) { // Si es combustible
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }else if (pIndex >= inventoryStart && pIndex < 31) { // Slots del inventario
                    if (!this.moveItemStackTo(itemstack1, 31, inventoryEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 31 && pIndex < inventoryEnd && !this.moveItemStackTo(itemstack1, inventoryStart, 31, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, inventoryStart, inventoryEnd, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }

        return itemstack;
    }


    protected boolean canSmelt(ItemStack pStack) {
        return this.level.getRecipeManager().getRecipeFor((RecipeType<AbstractCookingRecipe>)this.recipeType, new SingleRecipeInput(pStack), this.level).isPresent();
    }

    public boolean isFuel(ItemStack pStack) {
        return pStack.getBurnTime(this.recipeType) > 0;
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

    public float getCurrenEnergy() {
        return (float) this.data.get(6);
    }

    public float getMaxEnergy() {
        return (float) this.data.get(7);
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






    public boolean isBeingProcessed() {
        return data.get(3) > 0;
    }

    public int getScaledProgress() {
        int progress = data.get(4);
        int maxProgress = data.get(5);
        int progressArrowSize = 24; // Esto es el ancho en píxeles de la flecha de progreso en la GUI

        if (maxProgress == 0 || progress == 0) {
            return 0;
        }

        return progress * progressArrowSize / maxProgress;
    }

    public int getDarkEnergyStored() {
        return data.get(0);
    }

    public int getMaxDarkEnergyStored() {
        return data.get(1);
    }

    public int getScaledDarkEnergy() {
        int energy = data.get(0);
        int maxEnergy = data.get(1);
        int energyBarSize = 50; // Altura de la barra de energía en la GUI

        if (maxEnergy == 0 || energy == 0) {
            return 0;
        }

        return energy * energyBarSize / maxEnergy;
    }

    public int getCleanEnergyStored() {
        return data.get(2);
    }

    public int getMaxCleanEnergyStored() {
        return data.get(3);
    }

    public int getScaledCleanEnergy() {
        int energy = data.get(2);
        int maxEnergy = data.get(3);
        int energyBarSize = 50; // Altura de la barra de energía en la GUI

        if (maxEnergy == 0 || energy == 0) {
            return 0;
        }

        return energy * energyBarSize / maxEnergy;
    }














}
