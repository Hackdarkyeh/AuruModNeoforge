package aurum.aurum.client.gui.EnergyGeneratorBlock;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.Slots.EnergyGeneratorFuelSlot;
import aurum.aurum.block.engineering.EnergyGeneratorBlock.Slots.EnergyGeneratorUpdaterSlot;
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

public abstract class AbstractEnergyGeneratorMenu extends AbstractContainerMenu {
    public static final int INGREDIENT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int ENERGY_GENERATOR_UPDATER_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 4;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    protected AbstractEnergyGeneratorMenu(
            MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory
    ) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(4), new SimpleContainerData(7));
    }

    protected AbstractEnergyGeneratorMenu(
            MenuType<?> pMenuType,
            RecipeType<? extends AbstractCookingRecipe> pRecipeType,
            int pContainerId,
            Inventory pPlayerInventory,
            Container pContainer,
            ContainerData pData
    ) {
        super(pMenuType, pContainerId);
        this.recipeType = pRecipeType;
        checkContainerSize(pContainer, 4);
        checkContainerDataCount(pData, 6);
        this.container = pContainer;
        this.data = pData;
        this.level = pPlayerInventory.player.level();
        this.addSlot(new Slot(pContainer, INGREDIENT_SLOT, 86, 14));
        this.addSlot(new EnergyGeneratorFuelSlot(this, pContainer, FUEL_SLOT, 86, 50));
        this.addSlot(new FurnaceResultSlot(pPlayerInventory.player, pContainer, RESULT_SLOT, 148, 50));
        this.addSlot(new EnergyGeneratorUpdaterSlot(this, pContainer,
                ENERGY_GENERATOR_UPDATER_SLOT, 152, 9));



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
            } else if (pIndex != FUEL_SLOT && pIndex != INGREDIENT_SLOT && pIndex != ENERGY_GENERATOR_UPDATER_SLOT ) { // Slots normales
                if (this.canSmelt(itemstack1)) { // Si se puede fundir
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.isFuel(itemstack1)) { // Si es combustible
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if(this.isEnergyGeneratorUpdater(itemstack1)) { // Si es un actualizador de generador de energía
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
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

    public boolean isEnergyGeneratorUpdater(ItemStack pStack) {
        return pStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER1.get() ||
                pStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER2.get() ||
                pStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER3.get() ||
                pStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER4.get();
    }

    public float getBurnProgress() {
        int i = this.data.get(2);
        int j = this.data.get(3);
        return j != 0 && i != 0 ? Mth.clamp((float)i / (float)j, 0.0F, 1.0F) : 0.0F;
    }

    public float getEnergyProgress() {
        int currentEnergy = this.data.get(4);
        int maxEnergy = this.data.get(5);
        return Mth.clamp((float) currentEnergy / (float) maxEnergy, 0.0F, 1.0F);
    }

    public int getCurrenEnergy() {
        return this.data.get(4);
    }

    public int getMaxEnergy() {
        return this.data.get(5);
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
}
