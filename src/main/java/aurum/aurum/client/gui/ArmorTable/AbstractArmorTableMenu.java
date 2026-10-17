package aurum.aurum.client.gui.ArmorTable;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlockEntity;
import aurum.aurum.block.engineering.ArmorTable.Slots.*;
import aurum.aurum.init.ModItems;
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
import net.minecraft.world.level.Level;

public abstract class AbstractArmorTableMenu extends AbstractContainerMenu {
    public static final int ARMOR_IN = 0;
    public static final int AURELITE_IN = 1;
    public static final int ARMOR_OUT = 2; // Slot de previsualización / salida
    public static final int PROTECTOR = 3;
    public static final int RANGE_EXTRACTOR = 4;
    public static final int SLOT_COUNT = 5;

    public static final int DATA_COUNT = 10; // Actualizado a 10 para soportar EC y ED

    private final Container container;
    private final ContainerData data;
    protected final Level level;
    public final Player player;

    protected AbstractArmorTableMenu(MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory) {
        this(pMenuType, pRecipeType, pContainerId, pPlayerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    protected AbstractArmorTableMenu(MenuType<?> pMenuType, RecipeType<? extends AbstractCookingRecipe> pRecipeType, int pContainerId, Inventory pPlayerInventory, Container pContainer, ContainerData pData) {
        super(pMenuType, pContainerId);
        checkContainerSize(pContainer, SLOT_COUNT);
        checkContainerDataCount(pData, DATA_COUNT);
        this.container = pContainer;
        this.data = pData;
        this.level = pPlayerInventory.player.level();
        this.player = pPlayerInventory.player;

        // --- SLOTS DE LA MÁQUINA ---
        this.addSlot(new ArmorSlot(this, pContainer, ARMOR_IN, 14, 15));
        this.addSlot(new AureliteInputSlot(pContainer, AURELITE_IN, 14, 52));

        // Slot de previsualización del holograma
        this.addSlot(new ArmorUpgradeOutPutSlot(pPlayerInventory.player, this, pContainer, ARMOR_OUT, 75, 35));

        this.addSlot(new ArmorUpgradeSlot(this, pContainer, PROTECTOR, 42, 33));
        this.addSlot(new ArmorTableUpgradeSlot(this, pContainer, RANGE_EXTRACTOR, 109, 63));

        // Inventario del jugador
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

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int inventoryStart = SLOT_COUNT;
            int inventoryEnd = this.slots.size();

            if (pIndex < inventoryStart) {
                // Mover de la máquina al inventario
                if (!this.moveItemStackTo(itemstack1, inventoryStart, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Mover del inventario a la máquina
                if (this.isArmor(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, ARMOR_IN, ARMOR_IN + 1, false)) return ItemStack.EMPTY;
                } else if (itemstack1.is(ModItems.AURELITE_INGOT.get())) {
                    if (!this.moveItemStackTo(itemstack1, AURELITE_IN, AURELITE_IN + 1, false)) return ItemStack.EMPTY;
                } else if (this.isProtector(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, PROTECTOR, PROTECTOR + 1, false)) return ItemStack.EMPTY;
                } else if (this.isRangeExtractor(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, RANGE_EXTRACTOR, RANGE_EXTRACTOR + 1, false)) return ItemStack.EMPTY;
                } else if (pIndex >= inventoryStart && pIndex < inventoryStart + 27) {
                    if (!this.moveItemStackTo(itemstack1, inventoryStart + 27, inventoryEnd, false)) return ItemStack.EMPTY;
                } else if (pIndex >= inventoryStart + 27 && pIndex < inventoryEnd && !this.moveItemStackTo(itemstack1, inventoryStart, inventoryStart + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(pPlayer, itemstack1);
        }
        return itemstack;
    }

    // --- ENERGÍA LIMPIA (EC) ---
    public int getCurrenEnergy() {
        return (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.data.get(7) << 16) | (this.data.get(6) & 0xFFFF);
    }

    public float getEnergyProgress() {
        float current = getCurrenEnergy();
        float max = getMaxEnergy();
        if (max == 0) return 0;
        return Mth.clamp(current / max, 0.0F, 1.0F);
    }

    // --- ENERGÍA OSCURA (ED) ---
    public int getCurrenDarkEnergy() {
        return this.data.get(8);
    }

    public int getMaxDarkEnergy() {
        return this.data.get(9);
    }

    public float getDarkEnergyProgress() {
        float current = getCurrenDarkEnergy();
        float max = getMaxDarkEnergy();
        if (max == 0) return 0;
        return Mth.clamp(current / max, 0.0F, 1.0F);
    }

    // --- EXPERIENCIA ---
    public boolean hasInsufficientExp() {
        return this.player.experienceLevel < 30;
    }

    // --- VALIDACIONES DE ÍTEMS ---
    public boolean isArmor(ItemStack pStack) { return pStack.is(ModTags.Items.COMPATIBLE_ARMOR); }
    public boolean isProtector(ItemStack pStack) { return pStack.is(ModItems.EXTRACTOR_PROTECTOR.get().asItem()); }
    public boolean isRangeExtractor(ItemStack pStack) {
        return pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_1.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_2.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_3.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_4.get().asItem()) ||
                pStack.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_5.get().asItem());
    }

    public float getExtractorProgress() {
        return Mth.clamp(this.data.get(2) / (float) this.data.get(3), 0.0F, 1.0F);
    }

    @Override
    public void slotsChanged(Container pContainer) {
        super.slotsChanged(pContainer);
        if (this.container instanceof AbstractArmorTableBlockEntity tableEntity) {
            tableEntity.updateOutputSlot();
        }
    }
}