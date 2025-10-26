package aurum.aurum.block.engineering.DarkEnergyTable;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.energy.IEnergyConsumer;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractDarkEnergyTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible, IEnergyConsumer {
    // Definición de slots
    public static final int PURIFIER_SLOT = 0;          // Slot para el purificador
    public static final int WEAPON_SLOT = 1;            // Slot para el arma a recargar
    private static final int SLOTS_COUNT = 2; // Total de slots
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);

    // Almacenamiento de energía
    private final EnergyStorage energyStorage = new EnergyStorage(10000, 100, 100, 100); // Capacidad, tasa de transferencia
    private final EnergyStorage darkEnergyStorage = new EnergyStorage(10000, 100, 100, 100); // Capacidad, tasa de transferencia
    private final EnergyStorage cleanEnergyStorage = new EnergyStorage(10000, 100, 100, 100);
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private float litTime;
    float litDuration;
    int extractingProgress;
    int extractingTotalTime;

    int SCALING_FACTOR = 1000; // Factor de escala para energía almacenada

    private float energyStored = getEnergyStored(); // Energía almacenada actualmente
    private int processingProgress = 0;
    private int maxProcessingTime = 200; // Tiempo en ticks para procesar un purificador

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) darkEnergyStorage.getEnergyStored();
                case 1 -> (int) darkEnergyStorage.getCapacity();
                case 2 -> (int) cleanEnergyStorage.getEnergyStored();
                case 3 -> (int) cleanEnergyStorage.getCapacity();
                case 4 -> processingProgress;
                case 5 -> maxProcessingTime;
                case 6 -> (int) energyStorage.getEnergyStored();
                case 7 -> (int) energyStorage.getCapacity();
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
                case 6 -> energyStorage.setStoredEnergy(value);
                case 7 -> energyStorage.setMaxEnergyStored(value);
            }
        }

        @Override
        public int getCount() {
            return 8; // Número de valores que se sincronizan
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractDarkEnergyTableBlockEntity(
            BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType
    ) {
        super(pType, pPos, pBlockState);
        this.recipeType = pRecipeType;
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.litTime = pTag.getInt("BurnTime");
        this.extractingProgress = pTag.getInt("ExtractorTime");
        this.extractingTotalTime = pTag.getInt("CookTimeTotal");
        this.energyStored = pTag.getFloat("EnergyStorage");
        this.litDuration = this.getExtractingDuration();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putFloat("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        pTag.putFloat("EnergyStorage", this.energyStorage.getEnergyStored());
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractDarkEnergyTableBlockEntity pBlockEntity) {
        BlockPos coordsEnergyGenerator = pBlockEntity.findSingleGeneratorInNetwork();
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;
        if (pLevel.hasNearbyAlivePlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 5.0D)) {
            if (pLevel.getNearestPlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 5, false).experienceLevel == 30) {
            }else{
            }
        }


        // Reducir tiempo de combustión si está encendido
        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        // Actualizar el estado visual si cambió
        if (wasLit != pBlockEntity.isLit()) {
            stateChanged = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, pBlockEntity.isLit());
            pLevel.setBlock(pPos, pState, 3);
        }

        // Marcar el bloque como cambiado si hubo actualizaciones
        if (stateChanged) {
            setChanged(pLevel, pPos, pState);
        }
    }

    protected float getExtractingDuration() {
        return this.energyStored;
    }



    @Override
    public float getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public float getEnergyCapacity() {
        return energyStorage.getCapacity();
    }

    @Override
    public void receiveEnergy(float amount) {

    }

    @Override
    public boolean canReceiveEnergy() {
        return true;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public float getEnergyDemand() {
        return 0;
    }

    public void setEnergyStored(float energy) {
        this.energyStorage.addEnergy(energy, false);
    }

    /**
     * Returns {@code true} if automation can insert the given item in the given slot from the given side.
     */
    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    /**
     * Returns {@code true} if automation can extract the given item in the given slot from the given side.
     */
    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return pDirection == Direction.DOWN && pIndex == 1 ? pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
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
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }


    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (pIndex == 0 && !flag) {
            this.extractingProgress = 0;
            this.setChanged();
        }
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 2) {
            return false;
        } else if (pIndex != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return pStack.getBurnTime(this.recipeType) > 0 || pStack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {
        if (pRecipe != null) {
            ResourceLocation resourcelocation = pRecipe.id();
            this.recipesUsed.addTo(resourcelocation, 1);
        }
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }












    private BlockPos findSingleGeneratorInNetwork() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            assert level != null;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos)) continue;

                BlockEntity adjacentStateEntity = level.getBlockEntity(adjacentPos);
                BlockState adjacentState = level.getBlockState(adjacentPos);
                if (adjacentStateEntity instanceof EnergyGeneratorBlockEntity) {
                    // Generador encontrado adyacente
                    return adjacentPos;
                }

                if (adjacentState.getBlock() instanceof PipeBlock) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);

                    // Comprobar generadores adyacentes a esta tubería
                    for (Direction adjDirection : Direction.values()) {
                        BlockPos sidePos = adjacentPos.relative(adjDirection);
                        if (visited.contains(sidePos)) continue;

                        BlockEntity sideState = level.getBlockEntity(sidePos);
                        if (sideState instanceof EnergyGeneratorBlockEntity) {
                            // Generador encontrado adyacente a una tubería
                            return sidePos;
                        }
                    }
                }
            }
        }

        return null; // No se encontró generador en la red
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel pLevel, Vec3 pPopVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            pLevel.getRecipeManager().byKey(entry.getKey()).ifPresent(p_300839_ -> {
                list.add((RecipeHolder<?>)p_300839_);
                createExperience(pLevel, pPopVec, entry.getIntValue(), ((AbstractCookingRecipe)p_300839_.value()).getExperience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel pLevel, Vec3 pPopVec, int pRecipeIndex, float pExperience) {
        int i = Mth.floor((float)pRecipeIndex * pExperience);
        float f = Mth.frac((float)pRecipeIndex * pExperience);
        if (f != 0.0F && Math.random() < (double)f) {
            i++;
        }

        ExperienceOrb.award(pLevel, pPopVec, i);
    }

}

