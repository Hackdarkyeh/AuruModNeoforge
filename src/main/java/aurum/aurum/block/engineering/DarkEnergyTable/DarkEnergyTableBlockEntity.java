package aurum.aurum.block.engineering.DarkEnergyTable;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlockEntity;
import aurum.aurum.client.gui.DarkEnergyTable.DarkEnergyTableMenu;
import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.energy.IDarkEnergyWeapon;
import aurum.aurum.energy.IEnergyStorage;
import aurum.aurum.init.GUI.ModMenuType;
import aurum.aurum.init.ModBlockEntities;
import aurum.aurum.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class DarkEnergyTableBlockEntity extends AbstractDarkEnergyTableBlockEntity {
    /*
    // Definición de slots
    public static final int PURIFIER_SLOT = 0;          // Slot para el purificador
    public static final int WEAPON_SLOT = 1;            // Slot para el arma a recargar
    private static final int SLOTS_COUNT = 2; // Total de slots

    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);

    // Almacenamiento de energía
    private final EnergyStorage darkEnergyStorage = new EnergyStorage(10000, 100, 100, 100); // Capacidad, tasa de transferencia
    private final EnergyStorage cleanEnergyStorage = new EnergyStorage(10000, 100, 100, 100);

    // Datos para la GUI (progreso, energía, etc.)
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) darkEnergyStorage.getEnergyStored();
                case 1 -> (int) darkEnergyStorage.getMaxEnergyStored();
                case 2 -> (int) cleanEnergyStorage.getEnergyStored();
                case 3 -> (int) cleanEnergyStorage.getMaxEnergyStored();
                case 4 -> processingProgress;
                case 5 -> maxProcessingTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> darkEnergyStorage.setStoredEnergy(value);
                case 1 -> darkEnergyStorage.setMaxEnergyStored(value);
                case 2 -> cleanEnergyStorage.setStoredEnergy(value);
                case 3 -> cleanEnergyStorage.setMaxEnergyStored(value);
                case 4 -> processingProgress = value;
                case 5 -> maxProcessingTime = value;
            }
        }

        @Override
        public int getCount() {
            return 6; // Número de valores que se sincronizan
        }
    };

    private int processingProgress = 0;
    private int maxProcessingTime = 200; // Tiempo en ticks para procesar un purificador
    */
    public DarkEnergyTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.DARK_ENERGY_TABLE_BLOCK.get(), pPos, pBlockState, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dark_energy_table");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new DarkEnergyTableMenu(pId, pPlayer, this, this.dataAccess);
    }
    /*
    @Override
    public int getContainerSize() {
        return SLOTS_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        darkEnergyStorage.loadEnergyFromTag(pTag.getCompound("DarkEnergyStorage"));
        cleanEnergyStorage.loadEnergyFromTag(pTag.getCompound("CleanEnergyStorage"));
        processingProgress = pTag.getInt("ProcessingProgress");
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        pTag.put("DarkEnergyStorage", darkEnergyStorage.saveEnergyToTag(new CompoundTag()));
        pTag.put("CleanEnergyStorage", cleanEnergyStorage.saveEnergyToTag(new CompoundTag()));
        pTag.putInt("ProcessingProgress", processingProgress);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, DarkEnergyTableBlockEntity pBlockEntity) {
        if (pLevel.isClientSide) {
            return;
        }

        // Lógica de procesamiento de energía oscura a limpia
        boolean changed = false;
        if (pBlockEntity.canProcessPurifier()) {
            pBlockEntity.processingProgress++;
            if (pBlockEntity.processingProgress >= pBlockEntity.maxProcessingTime) {
                pBlockEntity.processPurifier();
                pBlockEntity.processingProgress = 0;
                changed = true;
            }
        } else {
            pBlockEntity.processingProgress = 0;
        }

        // Lógica de recarga de armas
        if (pBlockEntity.canRechargeWeapon()) {
            pBlockEntity.rechargeWeapon();
            changed = true;
        }

        if (changed) {
            setChanged(pLevel, pPos, pState);
        }
    }



    private boolean canProcessPurifier() {
        ItemStack purifierStack = this.items.get(PURIFIER_SLOT);
        // Asumiendo que ModItems.PURIFIER es el item purificador
        return !purifierStack.isEmpty() && purifierStack.is(ModItems.PURIFIER.get()) &&
                darkEnergyStorage.getEnergyStored() >= getEnergyCostForPurifier() &&
                cleanEnergyStorage.getRemainingCapacity() >= getCleanEnergyGenerated();
    }

    private void processPurifier() {
        ItemStack purifierStack = this.items.get(PURIFIER_SLOT);
        darkEnergyStorage.consumeEnergy(getEnergyCostForPurifier(), false);
        cleanEnergyStorage.addEnergy(getCleanEnergyGenerated(), false);
        purifierStack.shrink(1);
        this.items.set(PURIFIER_SLOT, purifierStack);
    }

    private float getEnergyCostForPurifier() {
        return 500; // Costo de energía oscura por purificador
    }

    private float getCleanEnergyGenerated() {
        return 1000; // Energía limpia generada por purificador
    }

    private boolean canRechargeWeapon() {
        ItemStack weaponStack = this.items.get(WEAPON_SLOT);
        if (weaponStack.isEmpty() || !(weaponStack.getItem() instanceof IDarkEnergyWeapon)) {
            return false;
        }
        IDarkEnergyWeapon weapon = (IDarkEnergyWeapon) weaponStack.getItem();
        return weapon.canReceiveDarkEnergy(weaponStack) &&
                weapon.getCurrentDarkEnergy(weaponStack) < weapon.getMaxDarkEnergy(weaponStack) &&
                cleanEnergyStorage.getEnergyStored() > 0; // Necesita energía limpia para recargar
    }

    private void rechargeWeapon() {
        ItemStack weaponStack = this.items.get(WEAPON_SLOT);
        IDarkEnergyWeapon weapon = (IDarkEnergyWeapon) weaponStack.getItem();

        float energyToTransfer = Math.min(cleanEnergyStorage.getEnergyStored(), 100); // Tasa de recarga
        int energyAccepted = (int) weapon.addDarkEnergy(weaponStack, (int) energyToTransfer); // Asumiendo que addDarkEnergy devuelve la cantidad aceptada

        if (energyAccepted > 0) {
            cleanEnergyStorage.consumeEnergy(energyAccepted, false);
        }
    }

    // Implementación de WorldlyContainer para automatización
    private static final int[] SLOTS_FOR_SIDES = new int[]{PURIFIER_SLOT, WEAPON_SLOT};

    /*@Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else if (pSide == Direction.UP) {
            return SLOTS_FOR_UP;
        } else {
            return SLOTS_FOR_SIDES;
        }
    }*/


    /*
    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }
    /*
    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        if (pDirection == Direction.DOWN && pIndex == CLEAN_ENERGY_OUTPUT_SLOT) {
            // Solo se puede extraer energía limpia si el ítem es un contenedor de energía limpia
            return pStack.getItem() instanceof DarkEnergyItems && ((DarkEnergyItems)pStack.getItem()).getEnergyData(pStack).currentEnergy() > 0;
        }
        return true; // Para otros slots, permitir extracción normal
    }*/
    /*
    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        return switch (pIndex) {
            //case DARK_ENERGY_INPUT_SLOT -> pStack.getItem() instanceof DarkEnergyItems; // Solo ítems de energía oscura
            case PURIFIER_SLOT -> pStack.is(ModItems.PURIFIER.get()); // Solo el ítem purificador
            case WEAPON_SLOT -> pStack.getItem() instanceof IDarkEnergyWeapon; // Solo armas de energía oscura
            default -> false;
        };
    }

    public static int getSlotsCount() {
        return SLOTS_COUNT;
    }
    */
}
