package aurum.aurum.client.gui.ExtractorBlock;

import aurum.aurum.block.engineering.ExtractorBlock.Slots.*;
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
import net.minecraft.world.level.Level;

public abstract class AbstractExtractorMenu extends AbstractContainerMenu {
    public static final int PIPE_STACK = 0;
    public static final int EXTRACTOR_PEAK = 1;
    public static final int MINERAL_OUTPUT = 2;
    public static final int PROTECTOR = 3;
    public static final int RANGE_EXTRACTOR = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 4;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    private final int FLOAT_SCALING_FACTOR = 1000;

    protected AbstractExtractorMenu(
            MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory
    ) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(7));
    }

    protected AbstractExtractorMenu(
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
        checkContainerDataCount(pData, 7);
        this.container = pContainer;
        this.data = pData;
        this.level = pPlayerInventory.player.level();
        this.addSlot(new ExtractorPipeSlot(this, pContainer, PIPE_STACK, 47, 17));
        this.addSlot(new ExtractorPeakSlot(this, pContainer, EXTRACTOR_PEAK, 47, 53));
        this.addSlot(new ExtractorMineralOutPutSlot(pPlayerInventory.player, pContainer, MINERAL_OUTPUT, 80, 58));
        this.addSlot(new ExtractorProtectorSlot(this, pContainer, PROTECTOR, 113, 17));
        this.addSlot(new ExtractorRangeSlot(this, pContainer, RANGE_EXTRACTOR, 113, 53));
        //this.addSlot(new EnergyGeneratorFuelSlot(this, pContainer, FUEL_SLOT, 86, 50));
        //this.addSlot(new FurnaceResultSlot(pPlayerInventory.player, pContainer, RESULT_SLOT, 148, 50));
        //this.addSlot(new EnergyGeneratorUpdaterSlot(this, pContainer,
          //      ENERGY_GENERATOR_UPDATER_SLOT, 152, 9));

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

            if (pIndex == MINERAL_OUTPUT) { // Slot de salida
                if (!this.moveItemStackTo(itemstack1, inventoryStart, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (pIndex != PIPE_STACK && pIndex != EXTRACTOR_PEAK && pIndex != PROTECTOR && pIndex != RANGE_EXTRACTOR) { // Slots normales
                if (this.isPipe(itemstack1)) { // Si se puede fundir
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.isPeak(itemstack1)) { // Si es combustible
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if(this.isProtector(itemstack1)) { // Si es un actualizador de generador de energía
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                }else if(this.isRangeExtractor(itemstack1)) { // Si es un actualizador de generador de energía
                    if (!this.moveItemStackTo(itemstack1, 4, 5, false)) {
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



    public int getCurrenEnergy() {
        return this.data.get(4);
    }

    public int getMaxEnergy() {
        return this.data.get(5);
    }

    public boolean hasInsufficientExp(){
        return this.data.get(6) == 1;
    }

    public boolean isPipe(ItemStack pStack) {
        return pStack.is(ModBlocks.PIPE_BLOCK.get().asItem());
    }

    public boolean isPeak(ItemStack pStack) {
        return pStack.is(ModItems.EXTRACTOR_PEAK_TIER1.get().asItem()) || pStack.is(ModItems.EXTRACTOR_PEAK_TIER2.get().asItem()) ||
                pStack.is(ModItems.EXTRACTOR_PEAK_TIER3.get().asItem() );
    }

    public boolean isProtector(ItemStack pStack) {
        return pStack.is(ModItems.EXTRACTOR_PROTECTOR.get().asItem());
    }

    public boolean isRangeExtractor(ItemStack pStack) {
        return pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_1.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_2.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_3.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_4.get().asItem())
                || pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_5.get().asItem());
    }

    public float getEnergyProgress() {
        int currentEnergy = this.data.get(4);
        int maxEnergy = this.data.get(5);
        return Mth.clamp((float) currentEnergy / (float) maxEnergy, 0.0F, 1.0F);
    }

    public float getExtractorProgress() {
        float currentTimeExtractorMineral = this.data.get(2);
        float totalTimeExtractorMineral = this.data.get(3);

        return Mth.clamp(currentTimeExtractorMineral/totalTimeExtractorMineral, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return this.data.get(0) / (float) FLOAT_SCALING_FACTOR > 0;
    }
}
