package aurum.aurum.block.engineering.EnergyGeneratorBlock;

import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.init.ModItems;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractEnergyGeneratorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    private static final int SLOTS_COUNT = 4;

    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;

    float totalCapacityGestion = 0f;
    public float energyStoredVisible = 0f;
    private int energyGeneratedPerTick = 1;
    private int energyTransferPerTick = 1; // Aunque ya no envíe a máquinas, define la velocidad a la que mete energía a la red

    private final Map<BlockPos, float[]> detectedMasterStorages = new HashMap<>();

    private static final int FLOAT_SCALING_FACTOR = 1000;

    private boolean forceRedetection = false;
    private int ticksSinceLastDetection = 0;

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0: return litDuration > Short.MAX_VALUE ? Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE) : litTime;
                case 1: return Math.min(litDuration, Short.MAX_VALUE);
                case 2: return cookingProgress;
                case 3: return cookingTotalTime;
                case 4: return (int)(energyStoredVisible * FLOAT_SCALING_FACTOR);
                case 5: return (int)(totalCapacityGestion * FLOAT_SCALING_FACTOR);
                default: return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0: litTime = p_58434_; break;
                case 1: litDuration = p_58434_; break;
                case 2: cookingProgress = p_58434_; break;
                case 3: cookingTotalTime = p_58434_; break;
                case 4: energyStoredVisible = p_58434_ / (float) FLOAT_SCALING_FACTOR; break;
                case 5: totalCapacityGestion = p_58434_ / (float) FLOAT_SCALING_FACTOR; break;
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    protected AbstractEnergyGeneratorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType) {
        super(pType, pPos, pBlockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<AbstractCookingRecipe>)pRecipeType);
        this.recipeType = pRecipeType;
    }

    public static void invalidateCache() { fuelCache = null; }

    @Deprecated
    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = fuelCache;
        if (map != null) return map;
        Map<Item, Integer> map1 = Maps.newLinkedHashMap();
        buildFuels((e, time) -> e.ifRight(tag -> add(map1, tag, time)).ifLeft(item -> add(map1, item, time)));
        fuelCache = map1;
        return map1;
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, ItemLike item, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.left(item.asItem()), time);
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, TagKey<Item> tag, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.right(tag), time);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static void buildFuels(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> map1) {
        add(map1, Items.LAVA_BUCKET, 10000);
        add(map1, Blocks.COAL_BLOCK, 8000);
        add(map1, Items.BLAZE_ROD, 1200);
        add(map1, Items.COAL, 800);
        add(map1, Items.CHARCOAL, 800);
        add(map1, ItemTags.LOGS, 150);
        add(map1, ItemTags.PLANKS, 150);
        add(map1, ItemTags.WOODEN_SLABS, 75);
    }

    private static boolean isNeverAFurnaceFuel(Item pItem) {
        return pItem.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    private static void add(Map<Item, Integer> pMap, TagKey<Item> pItemTag, int pBurnTime) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(pItemTag)) {
            if (!isNeverAFurnaceFuel(holder.value())) pMap.put(holder.value(), pBurnTime);
        }
    }

    private static void add(Map<Item, Integer> pMap, ItemLike pItem, int pBurnTime) {
        Item item = pItem.asItem();
        if (!isNeverAFurnaceFuel(item)) pMap.put(item, pBurnTime);
    }

    private boolean isLit() { return this.litTime > 0; }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.litTime = pTag.getInt("BurnTime");
        this.cookingProgress = pTag.getInt("CookTime");
        this.cookingTotalTime = pTag.getInt("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));

        if (pTag.contains("EnergyStored")) this.energyStoredVisible = pTag.getFloat("EnergyStored");
        if (pTag.contains("TotalCapacity")) this.totalCapacityGestion = pTag.getFloat("TotalCapacity");

        CompoundTag compoundtag = pTag.getCompound("RecipesUsed");
        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(s), compoundtag.getInt(s));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt("BurnTime", this.litTime);
        pTag.putInt("CookTime", this.cookingProgress);
        pTag.putInt("CookTimeTotal", this.cookingTotalTime);
        pTag.putFloat("EnergyStored", this.energyStoredVisible);
        pTag.putFloat("TotalCapacity", this.totalCapacityGestion);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractEnergyGeneratorBlockEntity pBlockEntity) {
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;

        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
            pBlockEntity.generateEnergy();
        }

        pBlockEntity.ticksSinceLastDetection++;
        if (pBlockEntity.ticksSinceLastDetection >= 20 || pBlockEntity.forceRedetection) {
            pBlockEntity.detectAdjacentBlocksGradual();
            pBlockEntity.ticksSinceLastDetection = 0;
            pBlockEntity.forceRedetection = false;
        }

        pBlockEntity.synchronizeEnergy();

        ItemStack fuelStack = pBlockEntity.items.get(1);
        ItemStack inputStack = pBlockEntity.items.get(0);
        ItemStack updaterStack = pBlockEntity.items.get(3);

        boolean hasFuel = !fuelStack.isEmpty();
        boolean hasInput = !inputStack.isEmpty();

        // Configuración de Updaters
        if (!updaterStack.isEmpty()) {
            if (updaterStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER1.get()) { pBlockEntity.energyGeneratedPerTick = 2; pBlockEntity.energyTransferPerTick = 1; }
            else if (updaterStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER2.get()) { pBlockEntity.energyGeneratedPerTick = 3; pBlockEntity.energyTransferPerTick = 2; }
            else if (updaterStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER3.get()) { pBlockEntity.energyGeneratedPerTick = 4; pBlockEntity.energyTransferPerTick = 2; }
            else if (updaterStack.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER4.get()) { pBlockEntity.energyGeneratedPerTick = 5; pBlockEntity.energyTransferPerTick = 3; }
        } else {
            pBlockEntity.energyGeneratedPerTick = 1;
            pBlockEntity.energyTransferPerTick = 1;
        }

        RecipeHolder<?> recipeHolder = hasInput ? pBlockEntity.quickCheck.getRecipeFor(new SingleRecipeInput(inputStack), pLevel).orElse(null) : null;
        int maxStackSize = pBlockEntity.getMaxStackSize();

        boolean canSmeltItem = canBurn(pLevel.registryAccess(), recipeHolder, pBlockEntity.items, maxStackSize, pBlockEntity);
        boolean hasEnergySpace = !pBlockEntity.detectedMasterStorages.isEmpty() && (pBlockEntity.energyStoredVisible < pBlockEntity.totalCapacityGestion);

        // SE ENCIENDE SI HAY COMBUSTIBLE Y (HAY ESPACIO EN LA RED O HAY ALGO QUE FUNDIR)
        if (!pBlockEntity.isLit() && hasFuel && (canSmeltItem || hasEnergySpace)) {
            pBlockEntity.litTime = pBlockEntity.getBurnDuration(fuelStack);
            pBlockEntity.litDuration = pBlockEntity.litTime;

            if (pBlockEntity.isLit()) {
                stateChanged = true;

                // --- LÓGICA DE CONSUMO 100% SEGURA ---
                // Salvamos el posible remanente (ej. el cubo vacío si fuera un cubo de lava)
                ItemStack remainingItem = fuelStack.hasCraftingRemainingItem() ? fuelStack.getCraftingRemainingItem() : ItemStack.EMPTY;

                // Forzamos la reducción del stack sí o sí
                fuelStack.shrink(1);

                // Si la ranura de carbón se queda vacía y había un remanente, lo colocamos
                if (fuelStack.isEmpty() && !remainingItem.isEmpty()) {
                    pBlockEntity.items.set(1, remainingItem);
                }
            }
        }

        if (pBlockEntity.isLit()) {
            if (canSmeltItem) {
                pBlockEntity.cookingProgress++;
                if (pBlockEntity.cookingProgress >= pBlockEntity.cookingTotalTime) {
                    pBlockEntity.cookingProgress = 0;
                    pBlockEntity.cookingTotalTime = getTotalCookTime(pLevel, pBlockEntity);
                    if (burn(pLevel.registryAccess(), recipeHolder, pBlockEntity.items, maxStackSize, pBlockEntity)) {
                        pBlockEntity.setRecipeUsed(recipeHolder);
                    }
                    stateChanged = true;
                }
            } else {
                pBlockEntity.cookingProgress = 0;
            }
        } else if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
        }

        if (wasLit != pBlockEntity.isLit()) {
            stateChanged = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, pBlockEntity.isLit());
            pLevel.setBlock(pPos, pState, 3);
        }

        if (stateChanged) setChanged(pLevel, pPos, pState);
    }

    private static boolean canBurn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractEnergyGeneratorBlockEntity furnace) {
        if (!pInventory.get(0).isEmpty() && pRecipe != null) {
            ItemStack itemstack = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            if (itemstack.isEmpty()) return false;
            ItemStack itemstack1 = pInventory.get(2);
            if (itemstack1.isEmpty()) return true;
            if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) return false;
            return itemstack1.getCount() + itemstack.getCount() <= pMaxStackSize && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()
                    ? true : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize();
        }
        return false;
    }

    private static boolean burn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractEnergyGeneratorBlockEntity furnace) {
        if (pRecipe != null && canBurn(pRegistryAccess, pRecipe, pInventory, pMaxStackSize, furnace)) {
            ItemStack itemstack = pInventory.get(0);
            ItemStack itemstack1 = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            ItemStack itemstack2 = pInventory.get(2);
            if (itemstack2.isEmpty()) pInventory.set(2, itemstack1.copy());
            else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) itemstack2.grow(itemstack1.getCount());

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !pInventory.get(1).isEmpty() && pInventory.get(1).is(Items.BUCKET)) {
                pInventory.set(1, new ItemStack(Items.WATER_BUCKET));
            }
            itemstack.shrink(1);
            return true;
        }
        return false;
    }

    protected int getBurnDuration(ItemStack pFuel) {
        return pFuel.isEmpty() ? 0 : pFuel.getBurnTime(this.recipeType);
    }

    private static int getTotalCookTime(Level pLevel, AbstractEnergyGeneratorBlockEntity pBlockEntity) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(pBlockEntity.getItem(0));
        return pBlockEntity.quickCheck.getRecipeFor(singlerecipeinput, pLevel).map(p -> p.value().getCookingTime()).orElse(200);
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) { return pSide == Direction.DOWN ? SLOTS_FOR_DOWN : (pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES); }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) { return this.canPlaceItem(pIndex, pItemStack); }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) { return pDirection != Direction.DOWN || pIndex != 1 || pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET); }

    @Override
    public int getContainerSize() { return this.items.size(); }

    @Override
    protected NonNullList<ItemStack> getItems() { return this.items; }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) { this.items = pItems; }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (pIndex == 0 && !flag) {
            this.cookingTotalTime = getTotalCookTime(this.level, this);
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 2) return false;
        if (pIndex != 1) return true;
        return pStack.getBurnTime(this.recipeType) > 0 || (pStack.is(Items.BUCKET) && !this.items.get(1).is(Items.BUCKET));
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {
        if (pRecipe != null) this.recipesUsed.addTo(pRecipe.id(), 1);
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() { return null; }

    @Override
    public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {}

    public void awardUsedRecipesAndPopExperience(ServerPlayer pPlayer) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(pPlayer.serverLevel(), pPlayer.position());
        pPlayer.awardRecipes(list);
        for (RecipeHolder<?> recipeholder : list) {
            if (recipeholder != null) pPlayer.triggerRecipeCrafted(recipeholder, this.items);
        }
        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel pLevel, Vec3 pPopVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();
        for (Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            pLevel.getRecipeManager().byKey(entry.getKey()).ifPresent(p -> {
                list.add((RecipeHolder<?>)p);
                createExperience(pLevel, pPopVec, entry.getIntValue(), ((AbstractCookingRecipe)p.value()).getExperience());
            });
        }
        return list;
    }

    private static void createExperience(ServerLevel pLevel, Vec3 pPopVec, int pRecipeIndex, float pExperience) {
        int i = Mth.floor((float)pRecipeIndex * pExperience);
        float f = Mth.frac((float)pRecipeIndex * pExperience);
        if (f != 0.0F && Math.random() < (double)f) i++;
        ExperienceOrb.award(pLevel, pPopVec, i);
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) pHelper.accountStack(itemstack);
    }

    // AHORA EL ESCANEO ES 100% EXCLUSIVO PARA ENCONTRAR ALMACENES (BATERÍAS)
    private void detectAdjacentBlocksGradual() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> currentMasters = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        int maxSteps = 150;
        int steps = 0;

        while (!queue.isEmpty() && steps < maxSteps) {
            BlockPos currentPos = queue.poll();
            steps++;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos) || !level.isLoaded(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);
                visited.add(adjacentPos);

                if (adjacentState.getBlock() instanceof PipeBlock) {
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    BlockEntity be = level.getBlockEntity(adjacentPos);
                    if (be instanceof EnergyStorageBlockEntity storage) {
                        BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                        if (level.isLoaded(masterPos) && level.getBlockEntity(masterPos) instanceof EnergyStorageBlockEntity masterStorage) {
                            currentMasters.add(masterPos);
                            if (!detectedMasterStorages.containsKey(masterPos)) {
                                masterStorage.syncWithGenerator(this);
                                detectedMasterStorages.put(masterPos, new float[]{masterStorage.getCapacity(), masterStorage.getEnergyStored()});
                            }
                        }
                    }
                }
            }
        }

        // Elimina los almacenes que ya no están en la red
        detectedMasterStorages.keySet().retainAll(currentMasters);

        recalculateTotalEnergyAndCapacity();
    }

    private void adjustGeneratorCapacity(float totalCapacityGestion) {
        if (energyStoredVisible > totalCapacityGestion) energyStoredVisible = totalCapacityGestion;
    }

    public void generateEnergy() {
        if (detectedMasterStorages.isEmpty()) return;

        for (BlockPos masterPos : detectedMasterStorages.keySet()) {
            BlockEntity storageEntity = level.getBlockEntity(masterPos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                if (storage.getRemainingCapacity() >= energyGeneratedPerTick) {
                    float energyActuallyAdded = storage.addEnergy(energyGeneratedPerTick, false);
                    if (energyActuallyAdded > 0) {
                        detectedMasterStorages.get(masterPos)[1] = storage.getEnergyStored();
                        syncEnergyWithStorages();
                        return; // Distribuimos solo al primer almacén con espacio, parando aquí
                    }
                }
            }
        }
    }

    public void synchronizeEnergy() {
        if (!detectedMasterStorages.isEmpty()) {
            syncEnergyWithStorages();
        }
    }

    private void syncEnergyWithStorages() {
        if(detectedMasterStorages.isEmpty()) return;
        float totalRealEnergy = 0;
        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            BlockEntity storageEntity = level.getBlockEntity(entry.getKey());
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                float realEnergy = storage.getEnergyStored();
                totalRealEnergy += realEnergy;
                entry.getValue()[1] = realEnergy;
            }
        }
        this.energyStoredVisible = totalRealEnergy;
    }

    public boolean hasEnergy() { return energyStoredVisible > 0; }

    public void onStorageNetworkUpdated(float storageEnergy, float storageCapacity, int storageCount, BlockPos masterPos) {
        if (level != null && !level.isClientSide) {
            if (storageCapacity == 0 && storageEnergy == 0) {
                detectedMasterStorages.remove(masterPos);
            } else {
                BlockEntity storageEntity = level.getBlockEntity(masterPos);
                if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                    detectedMasterStorages.put(masterPos, new float[] {storage.getCapacity(), storage.getEnergyStored()});
                }
            }
            recalculateTotalEnergyAndCapacity();
        }
    }

    private void recalculateTotalEnergyAndCapacity() {
        if (detectedMasterStorages.isEmpty()) {
            this.totalCapacityGestion = 0f;
            this.energyStoredVisible = 0f;
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return;
        }

        float totalEnergy = 0;
        float totalCapacity = 0;

        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            BlockEntity storageEntity = level.getBlockEntity(entry.getKey());
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                float realEnergy = storage.getEnergyStored();
                float realCapacity = storage.getCapacity();
                totalCapacity += realCapacity;
                totalEnergy += realEnergy;
                entry.getValue()[0] = realCapacity;
                entry.getValue()[1] = realEnergy;
            }
        }

        this.totalCapacityGestion = totalCapacity;
        this.energyStoredVisible = totalEnergy;
        adjustGeneratorCapacity(totalCapacityGestion);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void forceDetection() {
        this.forceRedetection = true;
        this.ticksSinceLastDetection = 20; // Fuerza a entrar en el if la próxima vez
    }
}