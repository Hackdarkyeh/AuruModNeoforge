package aurum.aurum.block.engineering.EnergyGeneratorBlock;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlockEntity;
import aurum.aurum.block.engineering.DarkEnergyTable.AbstractDarkEnergyTableBlockEntity;
import aurum.aurum.block.engineering.DarkEnergyTable.DarkEnergyTableBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.AbstractExtractorBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.ExtractorBlock;
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

    float totalCapacityGestion;
    public float energyStoredVisible = 0;
    private int energyGeneratedPerTick = 1;
    private int energyTransferPerTick = 1;

    // ✅ CORREGIDO: Usar maestros en lugar de storages individuales
    private final Map<BlockPos, float[]> detectedMasterStorages = new HashMap<>();
    Queue<Map.Entry<BlockPos, Float>> storageQueue = new LinkedList<>();

    private final Map<BlockPos, AbstractExtractorBlockEntity> detectedExtractors = new HashMap<>();
    private final Set<BlockPos> connectedExtractors = new HashSet<>();

    private final Map<BlockPos, AbstractDarkEnergyTableBlockEntity> detectedDarkEnergyTable = new HashMap<>();
    private final Set<BlockPos> connectedDarkEnergyTable = new HashSet<>();

    private final Set<BlockPos> connectedGenerators = new HashSet<>();
    private static final int FLOAT_SCALING_FACTOR = 1000;

    private final Set<BlockPos> subscribedMasterStorages = new HashSet<>();

    // ✅ NUEVO: Variables para detección mejorada
    private boolean forceRedetection = false;
    private int ticksSinceLastDetection = 0;
    private static final int DETECTION_INTERVAL = 10;

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    if (litDuration > Short.MAX_VALUE) {
                        return net.minecraft.util.Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE);
                    }
                    return aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.litTime;
                case 1:
                    return Math.min(aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.litDuration, Short.MAX_VALUE);
                case 2:
                    return aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.cookingProgress;
                case 3:
                    return aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.cookingTotalTime;
                case 4:
                    return (int)AbstractEnergyGeneratorBlockEntity.this.energyStoredVisible * FLOAT_SCALING_FACTOR;
                case 5:
                    return (int)aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.totalCapacityGestion * FLOAT_SCALING_FACTOR;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.litTime = p_58434_;
                    break;
                case 1:
                    aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.litDuration = p_58434_;
                    break;
                case 2:
                    aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.cookingProgress = p_58434_;
                    break;
                case 3:
                    aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.cookingTotalTime = p_58434_;
                    break;
                case 4:
                    AbstractEnergyGeneratorBlockEntity.this.energyStoredVisible = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
                case 5:
                    AbstractEnergyGeneratorBlockEntity.this.totalCapacityGestion = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    protected AbstractEnergyGeneratorBlockEntity(
            BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType
    ) {
        super(pType, pPos, pBlockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<AbstractCookingRecipe>)pRecipeType);
        this.recipeType = pRecipeType;
    }

    public static void invalidateCache() {
        fuelCache = null;
    }

    /**
     * @deprecated Neo: get burn times by calling {@link net.neoforged.neoforge.common.extensions.IItemStackExtension#getBurnTime(RecipeType)}
     */
    @Deprecated
    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = fuelCache;
        if (map != null) {
            return map;
        } else {
            Map<Item, Integer> map1 = Maps.newLinkedHashMap();
            buildFuels((e, time) -> e.ifRight(tag -> add(map1, tag, time)).ifLeft(item -> add(map1, item, time)));
            fuelCache = map1;
            return map1;
        }
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, ItemLike item, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.left(item.asItem()), time);
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, TagKey<Item> tag, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.right(tag), time);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static void buildFuels(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> map1) {
        {
            add(map1, Items.LAVA_BUCKET, 10000);
            add(map1, Blocks.COAL_BLOCK, 8000);
            add(map1, Items.BLAZE_ROD, 1200);
            add(map1, Items.COAL, 800);
            add(map1, Items.CHARCOAL, 800);
            add(map1, ItemTags.LOGS, 150);
            add(map1, ItemTags.BAMBOO_BLOCKS, 150);
            add(map1, ItemTags.PLANKS, 150);
            add(map1, Blocks.BAMBOO_MOSAIC, 150);
            add(map1, ItemTags.WOODEN_STAIRS, 150);
            add(map1, Blocks.BAMBOO_MOSAIC_STAIRS, 150);
            add(map1, ItemTags.WOODEN_SLABS, 75);
            add(map1, Blocks.BAMBOO_MOSAIC_SLAB, 75);
            add(map1, ItemTags.WOODEN_TRAPDOORS, 150);
            add(map1, ItemTags.WOODEN_PRESSURE_PLATES, 150);
            add(map1, ItemTags.WOODEN_FENCES, 150);
            add(map1, ItemTags.FENCE_GATES, 150);
            add(map1, Blocks.NOTE_BLOCK, 150);
            add(map1, Blocks.BOOKSHELF, 150);
            add(map1, Blocks.CHISELED_BOOKSHELF, 150);
            add(map1, Blocks.LECTERN, 150);
            add(map1, Blocks.JUKEBOX, 150);
            add(map1, Blocks.CHEST, 150);
            add(map1, Blocks.TRAPPED_CHEST, 150);
            add(map1, Blocks.CRAFTING_TABLE, 150);
            add(map1, Blocks.DAYLIGHT_DETECTOR, 150);
            add(map1, ItemTags.BANNERS, 150);
            add(map1, Items.BOW, 150);
            add(map1, Items.FISHING_ROD, 150);
            add(map1, Blocks.LADDER, 150);
            add(map1, ItemTags.SIGNS, 100);
            add(map1, ItemTags.HANGING_SIGNS, 400);
            add(map1, Items.WOODEN_SHOVEL, 100);
            add(map1, Items.WOODEN_SWORD, 100);
            add(map1, Items.WOODEN_HOE, 100);
            add(map1, Items.WOODEN_AXE, 100);
            add(map1, Items.WOODEN_PICKAXE, 100);
            add(map1, ItemTags.WOODEN_DOORS, 100);
            add(map1, ItemTags.BOATS, 600);
            add(map1, ItemTags.WOOL, 50);
            add(map1, ItemTags.WOODEN_BUTTONS, 50);
            add(map1, Items.STICK, 50);
            add(map1, ItemTags.SAPLINGS, 50);
            add(map1, Items.BOWL, 50);
            add(map1, ItemTags.WOOL_CARPETS, 33);
            add(map1, Blocks.DRIED_KELP_BLOCK, 2000);
            add(map1, Items.CROSSBOW, 150);
            add(map1, Blocks.BAMBOO, 25);
            add(map1, Blocks.DEAD_BUSH, 50);
            add(map1, Blocks.SCAFFOLDING, 25);
            add(map1, Blocks.LOOM, 150);
            add(map1, Blocks.BARREL, 150);
            add(map1, Blocks.CARTOGRAPHY_TABLE, 150);
            add(map1, Blocks.FLETCHING_TABLE, 150);
            add(map1, Blocks.SMITHING_TABLE, 150);
            add(map1, Blocks.COMPOSTER, 150);
            add(map1, Blocks.AZALEA, 50);
            add(map1, Blocks.FLOWERING_AZALEA, 50);
            add(map1, Blocks.MANGROVE_ROOTS, 150);
        }
    }

    private static boolean isNeverAFurnaceFuel(Item pItem) {
        return pItem.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    private static void add(Map<Item, Integer> pMap, TagKey<Item> pItemTag, int pBurnTime) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(pItemTag)) {
            if (!isNeverAFurnaceFuel(holder.value())) {
                pMap.put(holder.value(), pBurnTime);
            }
        }
    }

    private static void add(Map<Item, Integer> pMap, ItemLike pItem, int pBurnTime) {
        Item item = pItem.asItem();
        if (isNeverAFurnaceFuel(item)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw (IllegalStateException) Util.pauseInIde(
                        new IllegalStateException(
                                "A developer tried to explicitly make fire resistant item " + item.getName(null).getString() + " a furnace fuel. That will not work!"
                        )
                );
            }
        } else {
            pMap.put(item, pBurnTime);
        }
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
        this.cookingProgress = pTag.getInt("CookTime");
        this.cookingTotalTime = pTag.getInt("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));
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
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractEnergyGeneratorBlockEntity pBlockEntity) {
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;

        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
            pBlockEntity.generateEnergy();
        }

        // ✅ DETECCIÓN MEJORADA
        pBlockEntity.ticksSinceLastDetection++;
        if (pBlockEntity.ticksSinceLastDetection >= DETECTION_INTERVAL || pBlockEntity.forceRedetection) {
            pBlockEntity.detectAdjacentBlocksGradual();
            pBlockEntity.ticksSinceLastDetection = 0;
            pBlockEntity.forceRedetection = false;
        }

        pBlockEntity.removeDisconnectedStorages();
        pBlockEntity.synchronizeEnergy();

        ItemStack fuelStack = pBlockEntity.items.get(1);
        ItemStack inputStack = pBlockEntity.items.get(0);
        ItemStack energy_generator_updater = pBlockEntity.items.get(3);

        boolean hasFuel = !fuelStack.isEmpty();
        boolean hasInput = !inputStack.isEmpty();
        boolean hasEnergyGeneratorUpdater = !energy_generator_updater.isEmpty();

        if (hasEnergyGeneratorUpdater){
            if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER1.get()){
                pBlockEntity.energyGeneratedPerTick = 2;
                pBlockEntity.energyTransferPerTick = 200;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER2.get()){
                pBlockEntity.energyGeneratedPerTick = 3;
                pBlockEntity.energyTransferPerTick = 300;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER3.get()){
                pBlockEntity.energyGeneratedPerTick = 4;
                pBlockEntity.energyTransferPerTick = 400;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER4.get()){
                pBlockEntity.energyGeneratedPerTick = 5;
                pBlockEntity.energyTransferPerTick = 50;
            }
        }else{
            pBlockEntity.energyGeneratedPerTick = 1;
            pBlockEntity.energyTransferPerTick = 100;
        }

        RecipeHolder<?> recipeHolder = hasInput
                ? pBlockEntity.quickCheck.getRecipeFor(new SingleRecipeInput(inputStack), pLevel).orElse(null)
                : null;

        int maxStackSize = pBlockEntity.getMaxStackSize();

        if (!pBlockEntity.isLit() && hasFuel && canBurn(pLevel.registryAccess(), recipeHolder, pBlockEntity.items, maxStackSize, pBlockEntity)) {
            pBlockEntity.litTime = pBlockEntity.getBurnDuration(fuelStack);
            pBlockEntity.litDuration = pBlockEntity.litTime;
            if (pBlockEntity.isLit()) {
                stateChanged = true;
                if (fuelStack.hasCraftingRemainingItem()) {
                    pBlockEntity.items.set(1, fuelStack.getCraftingRemainingItem());
                } else {
                    fuelStack.shrink(1);
                    if (fuelStack.isEmpty()) {
                        pBlockEntity.items.set(1, fuelStack.getCraftingRemainingItem());
                    }
                }
            }
        }

        if (pBlockEntity.isLit() && canBurn(pLevel.registryAccess(), recipeHolder, pBlockEntity.items, maxStackSize, pBlockEntity)) {
            pBlockEntity.cookingProgress++;
            if (pBlockEntity.cookingProgress == pBlockEntity.cookingTotalTime) {
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

        if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
        }

        if (wasLit != pBlockEntity.isLit()) {
            stateChanged = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, pBlockEntity.isLit());
            pLevel.setBlock(pPos, pState, 3);
        }

        if (stateChanged) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private static boolean canBurn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractEnergyGeneratorBlockEntity furnace) {
        if (!pInventory.get(0).isEmpty() && pRecipe != null) {
            ItemStack itemstack = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = pInventory.get(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                    return false;
                } else {
                    return itemstack1.getCount() + itemstack.getCount() <= pMaxStackSize && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()
                            ? true
                            : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractEnergyGeneratorBlockEntity furnace) {
        if (pRecipe != null && canBurn(pRegistryAccess, pRecipe, pInventory, pMaxStackSize, furnace)) {
            ItemStack itemstack = pInventory.get(0);
            ItemStack itemstack1 = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            ItemStack itemstack2 = pInventory.get(2);
            if (itemstack2.isEmpty()) {
                pInventory.set(2, itemstack1.copy());
            } else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !pInventory.get(1).isEmpty() && pInventory.get(1).is(Items.BUCKET)) {
                pInventory.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack pFuel) {
        if (pFuel.isEmpty()) {
            return 0;
        } else {
            return pFuel.getBurnTime(this.recipeType);
        }
    }

    private static int getTotalCookTime(Level pLevel, AbstractEnergyGeneratorBlockEntity pBlockEntity) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(pBlockEntity.getItem(0));
        return pBlockEntity.quickCheck.getRecipeFor(singlerecipeinput, pLevel).map(p_300840_ -> p_300840_.value().getCookingTime()).orElse(200);
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

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

    public void awardUsedRecipesAndPopExperience(ServerPlayer pPlayer) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(pPlayer.serverLevel(), pPlayer.position());
        pPlayer.awardRecipes(list);

        for (RecipeHolder<?> recipeholder : list) {
            if (recipeholder != null) {
                pPlayer.triggerRecipeCrafted(recipeholder, this.items);
            }
        }

        this.recipesUsed.clear();
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

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }

    // ✅ DETECCIÓN MEJORADA
    private void detectAdjacentBlocksGradual() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> currentlyDetectedMasters = new HashSet<>();

        storageQueue.clear();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos)) continue;

                assert level != null;
                if (!level.isLoaded(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);
                visited.add(adjacentPos);

                if (adjacentState.getBlock() instanceof PipeBlock) {
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    BlockEntity storageEntity = level.getBlockEntity(adjacentPos);
                    if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                        EnergyStorageBlockEntity masterStorage = storage;
                        BlockPos masterPos = adjacentPos;

                        if (!storage.isMaster()) {
                            masterPos = storage.getMasterPos();
                            if (level.isLoaded(masterPos)) {
                                BlockEntity masterEntity = level.getBlockEntity(masterPos);
                                if (masterEntity instanceof EnergyStorageBlockEntity) {
                                    masterStorage = (EnergyStorageBlockEntity) masterEntity;
                                }
                            }
                        }

                        if (isConnectedToGenerator(masterPos)) {
                            currentlyDetectedMasters.add(masterPos);

                            if (!detectedMasterStorages.containsKey(masterPos)) {
                                System.out.println("🔌 RECONEXIÓN - Maestro detectado: " + masterPos);
                                masterStorage.syncWithGenerator(this);

                                float masterEnergy = masterStorage.getEnergyStored();
                                float masterCapacity = masterStorage.getCapacity();

                                float[] masterData = new float[]{masterCapacity, masterEnergy};
                                detectedMasterStorages.put(masterPos, masterData);

                                storageQueue.add(Map.entry(adjacentPos, masterCapacity));
                            }
                        }
                    }
                } else if(adjacentState.getBlock() instanceof ExtractorBlock){
                    BlockEntity extractorEntity = level.getBlockEntity(adjacentPos);
                    if (extractorEntity instanceof AbstractExtractorBlockEntity extractor) {
                        if (!detectedExtractors.containsKey(adjacentPos)) {
                            detectedExtractors.put(adjacentPos, extractor);
                            System.out.println("Extractor detectado en: " + adjacentPos);
                        }
                    }
                } else if(adjacentState.getBlock() instanceof DarkEnergyTableBlock){
                    BlockEntity darkEnergyTableEntity = level.getBlockEntity(adjacentPos);
                    if (darkEnergyTableEntity instanceof AbstractDarkEnergyTableBlockEntity darkEnergyTable) {
                        if (!detectedDarkEnergyTable.containsKey(adjacentPos)) {
                            detectedDarkEnergyTable.put(adjacentPos, darkEnergyTable);
                            System.out.println("DarkEnergyTable detectado en: " + adjacentPos);
                        }
                    }
                } else {
                    // Verificar desconexiones
                    if (detectedMasterStorages.containsKey(adjacentPos)) {
                        deleteStorage(adjacentPos);
                    }
                    if (detectedExtractors.containsKey(adjacentPos)) {
                        removeExtractor(adjacentPos);
                    }
                    if (detectedDarkEnergyTable.containsKey(adjacentPos)) {
                        removeDarkEnergyTable(adjacentPos);
                    }
                }
            }
        }

        if (!storageQueue.isEmpty()) {
            updateGeneratorCapacity(storageQueue);
        }

        verifyMissingMasters(currentlyDetectedMasters);
    }

    private void verifyMissingMasters(Set<BlockPos> currentlyDetectedMasters) {
        Set<BlockPos> mastersToRemove = new HashSet<>();

        for (BlockPos masterPos : detectedMasterStorages.keySet()) {
            if (!currentlyDetectedMasters.contains(masterPos)) {
                if (!isMasterStillConnected(masterPos)) {
                    mastersToRemove.add(masterPos);
                    System.out.println("🔌 MAESTRO PERDIDO - Ya no está conectado: " + masterPos);
                }
            }
        }

        for (BlockPos masterPos : mastersToRemove) {
            detectedMasterStorages.remove(masterPos);
        }

        if (!mastersToRemove.isEmpty()) {
            recalculateTotalEnergyAndCapacity();
        }
    }

    private boolean isMasterStillConnected(BlockPos masterPos) {
        if (!level.isLoaded(masterPos)) return false;
        BlockEntity be = level.getBlockEntity(masterPos);
        if (!(be instanceof EnergyStorageBlockEntity masterStorage)) return false;
        if (!masterStorage.isMaster()) return false;
        return isConnectedToGenerator(masterPos);
    }

    private void deleteStorage(BlockPos storagePos) {
        BlockPos masterToRemove = null;
        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            BlockPos masterPos = entry.getKey();
            BlockEntity masterEntity = level.getBlockEntity(masterPos);
            if (masterEntity instanceof EnergyStorageBlockEntity masterStorage) {
                if (masterStorage.mergedBlocks.contains(storagePos)) {
                    masterToRemove = masterPos;
                    break;
                }
            }
        }

        if (masterToRemove != null) {
            float[] data = detectedMasterStorages.remove(masterToRemove);
            float storageCapacity = (data != null) ? data[0] : 0;
            float energyStorage = (data != null) ? data[1] : 0;

            System.out.println("❌ MAESTRO eliminado: " + masterToRemove +
                    " - Energía: " + energyStorage +
                    " - Capacidad: " + storageCapacity);

            energyStoredVisible -= energyStorage;
            totalCapacityGestion -= storageCapacity;

            recalculateTotalEnergyAndCapacity();
        }

        storageQueue.removeIf(entry -> entry.getKey().equals(storagePos));
    }

    private void detectConnectedGenerators() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        connectedGenerators.clear();
        connectedGenerators.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);

                if (adjacentState.getBlock() instanceof PipeBlock) {
                    visited.add(adjacentPos);
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyGeneratorBlock) {
                    visited.add(adjacentPos);
                    connectedGenerators.add(adjacentPos);
                }
            }
        }
    }

    private void distributeEnergy() {
        updateConnectedExtractors();
        updateConnectedDarkEnergyTables();
        detectConnectedGenerators();

        int machinesNeedingEnergy = 0;
        float availableEnergy = Math.min(energyStoredVisible, energyTransferPerTick);

        for (BlockPos extractorPos : connectedExtractors) {
            AbstractExtractorBlockEntity extractor = detectedExtractors.get(extractorPos);
            if (extractor != null && extractor.getEnergyStored() < extractor.energyCapacity) {
                machinesNeedingEnergy++;
            }
        }

        for (BlockPos tablePos : connectedDarkEnergyTable) {
            AbstractDarkEnergyTableBlockEntity table = detectedDarkEnergyTable.get(tablePos);
            if (table != null && table.getEnergyStored() < table.getEnergyCapacity()) {
                machinesNeedingEnergy++;
            }
        }

        if (machinesNeedingEnergy > 0) {
            if(availableEnergy > 0) {
                float energyPerMachine = availableEnergy / machinesNeedingEnergy;
                for (BlockPos extractorPos : connectedExtractors) {
                    AbstractExtractorBlockEntity extractor = detectedExtractors.get(extractorPos);
                    if (extractor != null && extractor.getEnergyStored() < extractor.getEnergyCapacity()) {
                        float energyNeeded = extractor.getEnergyCapacity() - extractor.getEnergyStored();
                        float energyToSend = Math.min(energyPerMachine, energyNeeded);

                        if (energyToSend > 0 && energyStoredVisible >= energyToSend) {
                            this.energyStoredVisible -= energyToSend;
                            extractor.setEnergyStored(energyToSend);
                        }
                    }
                }

                for (BlockPos tablePos : connectedDarkEnergyTable) {
                    AbstractDarkEnergyTableBlockEntity table = detectedDarkEnergyTable.get(tablePos);
                    if (table != null && table.getEnergyStored() < table.getEnergyCapacity()) {
                        float energyNeeded = table.getEnergyCapacity() - table.getEnergyStored();
                        float energyToSend = Math.min(energyPerMachine, energyNeeded);

                        if (energyToSend > 0 && energyStoredVisible >= energyToSend) {
                            this.energyStoredVisible -= energyToSend;
                            table.setEnergyStored(energyToSend);
                        }
                    }
                }
            }
        } else {
            for (BlockPos masterPos : detectedMasterStorages.keySet()) {
                BlockEntity storageEntity = level.getBlockEntity(masterPos);
                if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                    if (this.energyStoredVisible > 0 && storage.canReceive()) {
                        int storageSpace = (int) (storage.getCapacity() - storage.getEnergyStored());
                        if (storageSpace > 0) {
                            float energyToTransfer = Math.min(
                                    Math.min(this.energyStoredVisible, energyTransferPerTick),
                                    storageSpace
                            );

                            if (energyToTransfer > 0) {
                                storage.addEnergy(energyToTransfer, false);
                                this.energyStoredVisible -= energyToTransfer;
                            }
                        }
                    }
                }
            }
        }

        // Sincronizar energía entre generadores conectados
        float totalEnergy = 0;
        for (BlockPos masterPos : detectedMasterStorages.keySet()) {
            float[] data = detectedMasterStorages.get(masterPos);
            if (data != null) {
                totalEnergy += data[1]; // Energía
            }
        }

        for (BlockPos generatorPos : connectedGenerators) {
            BlockEntity generatorEntity = level.getBlockEntity(generatorPos);
            if (generatorEntity instanceof AbstractEnergyGeneratorBlockEntity generator) {
                generator.energyStoredVisible = totalEnergy;
            }
        }
    }

    public void extractEnergyFromNetwork(AbstractArmorTableBlockEntity armorTable) {
        for (BlockPos masterPos : detectedMasterStorages.keySet()) {
            BlockEntity storageEntity = level.getBlockEntity(masterPos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                if (storage.getEnergyStored() >= 0) {
                    if (armorTable.energyStored < armorTable.energyCapacity) {
                        armorTable.energyStored += energyTransferPerTick;
                        storage.consumeEnergy(energyTransferPerTick, false);
                        if (storage.getEnergyStored() <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void adjustGeneratorCapacity(float totalCapacityGestion) {
        if (energyStoredVisible > totalCapacityGestion) {
            energyStoredVisible = totalCapacityGestion;
        }
    }

    public void generateEnergy() {
        this.energyStoredVisible = Math.min(this.energyStoredVisible + energyGeneratedPerTick, this.totalCapacityGestion);
    }

    public void synchronizeEnergy() {
        if (!detectedMasterStorages.isEmpty() || !detectedExtractors.isEmpty() || !detectedDarkEnergyTable.isEmpty()) {
            distributeEnergy();
        }
    }

    public boolean hasEnergy() {
        return energyStoredVisible > 0;
    }

    private void removeDisconnectedStorages() {
        Set<BlockPos> mastersToRemove = new HashSet<>();

        for (BlockPos masterPos : detectedMasterStorages.keySet()) {
            if (!isMasterStillConnected(masterPos)) {
                mastersToRemove.add(masterPos);
                System.out.println("❌ ELIMINANDO MAESTRO DESCONECTADO: " + masterPos);
            }
        }

        for (BlockPos masterPos : mastersToRemove) {
            detectedMasterStorages.remove(masterPos);
        }

        if (!mastersToRemove.isEmpty()) {
            recalculateTotalEnergyAndCapacity();
        }

        subscribedMasterStorages.removeIf(masterPos -> {
            if (!level.isLoaded(masterPos)) return true;
            BlockEntity be = level.getBlockEntity(masterPos);
            if (!(be instanceof EnergyStorageBlockEntity masterStorage)) return true;
            return !masterStorage.isMaster() || !isConnectedToGenerator(masterPos);
        });
    }

    private boolean isConnectedToGenerator(BlockPos targetPos) {
        if (worldPosition.equals(targetPos)) return true;

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        int maxSteps = 100;
        int steps = 0;

        while (!queue.isEmpty() && steps < maxSteps) {
            BlockPos currentPos = queue.poll();
            steps++;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                if (visited.contains(adjacentPos)) continue;
                if (!level.isLoaded(adjacentPos)) continue;

                visited.add(adjacentPos);

                if (adjacentPos.equals(targetPos)) {
                    return true;
                }

                BlockState adjacentState = level.getBlockState(adjacentPos);

                if (adjacentState.getBlock() instanceof PipeBlock ||
                        adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    queue.add(adjacentPos);
                }
            }
        }

        return false;
    }

    private void removeExtractor(BlockPos extractorPos) {
        detectedExtractors.remove(extractorPos);
        connectedExtractors.remove(extractorPos);
        System.out.println("❌ Extractor removido de la red: " + extractorPos);
    }

    private void removeDarkEnergyTable(BlockPos darkEnergyTablePos) {
        detectedDarkEnergyTable.remove(darkEnergyTablePos);
        connectedDarkEnergyTable.remove(darkEnergyTablePos);
        System.out.println("❌ DarkEnergyTable removido de la red: " + darkEnergyTablePos);
    }

    private void updateGeneratorCapacity(Queue<Map.Entry<BlockPos, Float>> storageQueue) {
        Set<BlockPos> processedMasters = new HashSet<>();

        for (Map.Entry<BlockPos, Float> entry : storageQueue) {
            BlockPos storagePos = entry.getKey();
            BlockEntity storageEntity = level.getBlockEntity(storagePos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                EnergyStorageBlockEntity masterStorage = storage;
                BlockPos masterPos = storagePos;

                if (!storage.isMaster()) {
                    masterPos = storage.getMasterPos();
                    BlockEntity masterEntity = level.getBlockEntity(masterPos);
                    if (masterEntity instanceof EnergyStorageBlockEntity) {
                        masterStorage = (EnergyStorageBlockEntity) masterEntity;
                    }
                }

                // ✅ SOLO agregar si es un maestro nuevo
                if (!detectedMasterStorages.containsKey(masterPos)) {
                    processedMasters.add(masterPos);
                    float capacity = masterStorage.getCapacity();
                    float energy = masterStorage.getEnergyStored();

                    float[] storageData = new float[] {capacity, energy};
                    detectedMasterStorages.put(masterPos, storageData);

                    System.out.println("🔋 NUEVO MAESTRO agregado: " + masterPos +
                            " - Energía: " + energy + " - Capacidad: " + capacity);
                }
            }
        }

        if (!processedMasters.isEmpty()) {
            // ✅ CALCULAR TOTALES DESDE CERO
            this.totalCapacityGestion = calculateTotalCapacity();
            this.energyStoredVisible = calculateTotalEnergy();
            adjustGeneratorCapacity(totalCapacityGestion);

            System.out.println("🎯 CAPACIDAD ACTUALIZADA - Energía total: " + energyStoredVisible +
                    " / Capacidad total: " + totalCapacityGestion +
                    " - Maestros totales: " + detectedMasterStorages.size());
        }
    }

    // ✅ NUEVO MÉTODO: Calcular capacidad total desde cero
    private float calculateTotalCapacity() {
        float total = 0;
        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            float[] data = entry.getValue();
            total += data[0]; // Capacidad en índice 0
        }
        return total;
    }

    // ✅ NUEVO MÉTODO: Calcular energía total desde cero
    private float calculateTotalEnergy() {
        float total = 0;
        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            float[] data = entry.getValue();
            total += data[1]; // Energía en índice 1
        }
        return total;
    }

    private void updateConnectedExtractors() {
        connectedExtractors.clear();
        for (BlockPos extractorPos : detectedExtractors.keySet()) {
            if (isConnectedToGenerator(extractorPos)) {
                connectedExtractors.add(extractorPos);
            }
        }
    }

    private void updateConnectedDarkEnergyTables() {
        connectedDarkEnergyTable.clear();
        for (BlockPos tablePos : detectedDarkEnergyTable.keySet()) {
            if (isConnectedToGenerator(tablePos)) {
                connectedDarkEnergyTable.add(tablePos);
            }
        }
    }

    public void onStorageNetworkUpdated(float storageEnergy, float storageCapacity, int storageCount, BlockPos masterPos) {
        if (level != null && !level.isClientSide) {
            if (storageCapacity == 0 && storageEnergy == 0) {
                detectedMasterStorages.remove(masterPos);
                System.out.println("🗑️ MAESTRO removido: " + masterPos);
            } else {
                float[] masterData = new float[] {storageCapacity, storageEnergy};
                detectedMasterStorages.put(masterPos, masterData);
                System.out.println("🔄 MAESTRO actualizado: " + masterPos +
                        " - Energía: " + storageEnergy +
                        " - Capacidad: " + storageCapacity);
            }

            recalculateTotalEnergyAndCapacity();
        }
    }

    private void recalculateTotalEnergyAndCapacity() {
        float totalEnergy = 0;
        float totalCapacity = 0;

        for (Map.Entry<BlockPos, float[]> entry : detectedMasterStorages.entrySet()) {
            float[] data = entry.getValue();
            totalCapacity += data[0];
            totalEnergy += data[1];
        }

        this.totalCapacityGestion = totalCapacity;
        this.energyStoredVisible = totalEnergy;
        adjustGeneratorCapacity(totalCapacityGestion);

        System.out.println("🔢 RECALCULADO - Maestros: " + detectedMasterStorages.size() +
                ", Energía: " + energyStoredVisible + ", Capacidad: " + totalCapacityGestion);

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ✅ NUEVO: Método para forzar re-detección
    public void forceDetection() {
        this.forceRedetection = true;
        this.ticksSinceLastDetection = DETECTION_INTERVAL;
        System.out.println("🔄 DETECCIÓN FORZADA solicitada");
    }
}