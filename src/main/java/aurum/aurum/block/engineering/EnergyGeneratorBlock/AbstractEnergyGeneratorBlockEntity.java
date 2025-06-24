package aurum.aurum.block.engineering.EnergyGeneratorBlock;

import aurum.aurum.block.engineering.ArmorTable.AbstractArmorTableBlockEntity;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.AbstractExtractorBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.ExtractorBlockEntity;
import aurum.aurum.block.engineering.PipeBlock;
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
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;

    float totalCapacityGestion;
    public float energyCapacity; // Capacidad base mínima
    private float MINENERGYCAPACITY = 100; // Capacidad base mínima
    public float energyStoredVisible = 0; // Energía almacenada actualmente
    private int energyGeneratedPerTick = 1; // Energía generada por tick
    private int energyTransferPerTick = 1; // Energía transferida por tick
    private final Map<BlockPos, float[]> detectedStorages = new HashMap<>();
    Queue<Map.Entry<BlockPos, Float>> storageQueue = new LinkedList<>();
    private final Set<BlockPos> connectedGenerators = new HashSet<>();
    private float internalEnergy = 0;
    private static final int FLOAT_SCALING_FACTOR = 1000; // Factor de escala

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    if (litDuration > Short.MAX_VALUE) {
                        // Neo: preserve litTime / litDuration ratio on the client as data slots are synced as shorts.
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
                    return (int)aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity.this.energyCapacity * FLOAT_SCALING_FACTOR;

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
                    AbstractEnergyGeneratorBlockEntity.this.energyStoredVisible = p_58434_ / (float) FLOAT_SCALING_FACTOR; // Convertir de nuevo a float
                    break;
                case 5:
                    AbstractEnergyGeneratorBlockEntity.this.energyCapacity = p_58434_ / (float) FLOAT_SCALING_FACTOR; // Convertir de nuevo a float
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
        //CompoundTag compoundtag = new CompoundTag();
        //this.recipesUsed.forEach((p_187449_, p_187450_) -> compoundtag.putInt(p_187449_.toString(), p_187450_));
        //pTag.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity pBlockEntity) {
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;

        // Reducir tiempo de combustión si está encendido
        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
            pBlockEntity.generateEnergy(); // Generar energía mientras está encendido
        }
        pBlockEntity.detectAdjacentBlocksGradual(); // Detectar dispositivos de almacenamiento y tuberías

        pBlockEntity.removeDisconnectedStorages(); // Elimina los almacenamientos desconectados


        pBlockEntity.syncronizeEnergy(); // Sincronizar energía con los dispositivos de almacenamiento
        ItemStack fuelStack = pBlockEntity.items.get(1); // Slot de combustible
        ItemStack inputStack = pBlockEntity.items.get(0); // Slot de entrada
        ItemStack energy_generator_updater = pBlockEntity.items.get(3); // Slot de actualizador de generador de energía

        boolean hasFuel = !fuelStack.isEmpty();
        boolean hasInput = !inputStack.isEmpty();
        boolean hasEnergyGeneratorUpdater = !energy_generator_updater.isEmpty();
        if (hasEnergyGeneratorUpdater){
            if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER1.get()){
                pBlockEntity.energyGeneratedPerTick = 2;
                pBlockEntity.energyTransferPerTick = 2;
                pBlockEntity.MINENERGYCAPACITY = 200;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER2.get()){
                pBlockEntity.energyGeneratedPerTick = 3;
                pBlockEntity.energyTransferPerTick = 3;
                pBlockEntity.MINENERGYCAPACITY = 300;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER3.get()){
                pBlockEntity.energyGeneratedPerTick = 4;
                pBlockEntity.energyTransferPerTick = 4;
                pBlockEntity.MINENERGYCAPACITY = 400;
            }
            else if (energy_generator_updater.getItem() == ModItems.ENERGY_GENERATOR_UPDATER_TIER4.get()){
                pBlockEntity.energyGeneratedPerTick = 5;
                pBlockEntity.energyTransferPerTick = 5;
                pBlockEntity.MINENERGYCAPACITY = 500;
            }
        }else{
            pBlockEntity.energyGeneratedPerTick = 1;
            pBlockEntity.energyTransferPerTick = 1;
            pBlockEntity.MINENERGYCAPACITY = 100;
        }

        RecipeHolder<?> recipeHolder = hasInput
                ? pBlockEntity.quickCheck.getRecipeFor(new SingleRecipeInput(inputStack), pLevel).orElse(null)
                : null;

        int maxStackSize = pBlockEntity.getMaxStackSize();

        // Iniciar combustión si no está encendido pero tiene combustible y puede procesar
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

        // Procesar receta si está encendido
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

        // Reducir progreso si no hay combustión
        if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
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


    private static boolean canBurn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity furnace) {
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
                    return itemstack1.getCount() + itemstack.getCount() <= pMaxStackSize && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize() // Neo fix: make furnace respect stack sizes in furnace recipes
                            ? true
                            : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Neo fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity furnace) {
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

    private static int getTotalCookTime(Level pLevel, aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity pBlockEntity) {
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
            this.cookingTotalTime = getTotalCookTime(this.level, this);
            this.cookingProgress = 0;
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








    private void detectAdjacentBlocksGradual() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        storageQueue.clear();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos)) continue;
                assert level != null;
                BlockState adjacentState = level.getBlockState(adjacentPos);
                if (adjacentState.getBlock() instanceof PipeBlock) {
                    visited.add(adjacentPos);
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    if (!detectedStorages.containsKey(adjacentPos)) {
                        EnergyStorageBlockEntity energyStorageBlockEntity = (EnergyStorageBlockEntity) level.getBlockEntity(adjacentPos);
                        assert energyStorageBlockEntity != null;
                        this.energyStoredVisible += energyStorageBlockEntity.getEnergyStored();
                        storageQueue.add(Map.entry(adjacentPos, energyStorageBlockEntity.getMaxEnergyStored()));
                        updateGeneratorCapacity(storageQueue);
                    }
                } else {
                    if (detectedStorages.containsKey(adjacentPos)) {
                        deleteStorage(adjacentPos);  // Solo eliminar si ya estaba detectado antes
                    }
                }
            }
        }
        if (detectedStorages.isEmpty()) {
            adjustGeneratorCapacity(MINENERGYCAPACITY);
        }
    }

    private void deleteStorage(BlockPos storagePos) {
            float[] data = detectedStorages.remove(storagePos);
            float storageCapacity = (data != null) ? data[0] : 0; // Índice 0 para la capacidad máxima
            float energyStorage = (data != null) ? data[1] : 0; // Índice 0 para la capacidad máxima
            System.out.println("Almacenamiento eliminado en: " + storagePos + " con energía: " + energyStorage);
            energyCapacity -= storageCapacity;
            if (internalEnergy > energyCapacity) {
                internalEnergy = energyCapacity;
            }
            energyStoredVisible -= energyStorage;
            totalCapacityGestion -= storageCapacity;
            storageQueue.removeIf(entry -> entry.getKey().equals(storagePos));
            detectedStorages.remove(storagePos);

    }

    private void detectConnectedGenerators() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        connectedGenerators.clear();  // Limpiar la lista de generadores conectados
        connectedGenerators.add(worldPosition);  // Añadir el generador actual

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
    private void synchronizeEnergy() {
        energyStoredVisible = 0;
        detectConnectedGenerators();
        for (BlockPos storagePos : detectedStorages.keySet()) {
            BlockEntity storageEntity = level.getBlockEntity(storagePos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                //float energyToTransfer = Math.min(this.internalEnergy, storage.getRemainingCapacity());
                if (this.internalEnergy > energyTransferPerTick) {
                    this.internalEnergy -= energyTransferPerTick;
                    storage.addEnergy(energyTransferPerTick, false);
                    detectedStorages.get(storagePos)[0] += energyTransferPerTick;
                }


            }
        }
        for (BlockPos generatorPos : connectedGenerators) {
            BlockEntity generatorEntity = level.getBlockEntity(generatorPos);
            if (generatorEntity instanceof AbstractEnergyGeneratorBlockEntity generator) {
                for (BlockPos storagePos : detectedStorages.keySet()) {
                    if (!generatorPos.equals(this.getBlockPos())) {
                        continue;
                    }
                    BlockEntity storageEntity = level.getBlockEntity(storagePos);
                    if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                        generator.energyStoredVisible += storage.getEnergyStored();
                    }
                }
            }
        }

    }

    public void extractEnergyFromNetwork(AbstractExtractorBlockEntity extractor) {
        for (BlockPos storagePos : detectedStorages.keySet()) {
            BlockEntity storageEntity = level.getBlockEntity(storagePos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                if (storage.getEnergyStored() >= 0) {
                    if (extractor.energyStored < extractor.energyCapacity) {
                        extractor.energyStored += 1;
                        storage.consumeEnergy(1, false);
                        if (storage.getEnergyStored() <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void extractEnergyFromNetwork(AbstractArmorTableBlockEntity armorTable) {
        for (BlockPos storagePos : detectedStorages.keySet()) {
            BlockEntity storageEntity = level.getBlockEntity(storagePos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage) {
                if (storage.getEnergyStored() >= 0) {
                    if (armorTable.energyStored < armorTable.energyCapacity) {
                        armorTable.energyStored += 1;
                        storage.consumeEnergy(1, false);
                        if (storage.getEnergyStored() <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }



    private void updateGeneratorCapacity(Queue<Map.Entry<BlockPos, Float>> storageQueue) {
        for (Map.Entry<BlockPos, Float> entry : storageQueue) {
            BlockPos storagePos = entry.getKey();
            BlockEntity storageEntity = level.getBlockEntity(storagePos);
            if (storageEntity instanceof EnergyStorageBlockEntity storage && !detectedStorages.containsKey(storagePos)) {
                float capacity = storage.getMaxEnergyStored();
                totalCapacityGestion += capacity;
                energyStoredVisible += storage.getEnergyStored();
                float[] storageData = new float[] {capacity, storage.getEnergyStored()}; // Capacidad máxima y energía actual
                detectedStorages.put(storagePos, storageData);
                adjustGeneratorCapacity(totalCapacityGestion);
            }
        }
    }



    private void adjustGeneratorCapacity(float newCapacity) {
        this.energyCapacity = Math.max(newCapacity, MINENERGYCAPACITY);  // Valor mínimo de seguridad
        if (internalEnergy > energyCapacity) {
            internalEnergy = energyCapacity;
        }
    }

    public void generateEnergy() {
        this.internalEnergy = Math.min(this.internalEnergy + energyGeneratedPerTick, this.energyCapacity);
    }

    public void syncronizeEnergy() {
        if (!detectedStorages.isEmpty()) {
            synchronizeEnergy();
        }else{
            energyStoredVisible = internalEnergy;
        }
    }

    public boolean hasEnergy() {
        return energyStoredVisible > 0 || internalEnergy > 0;
    }

    private void removeDisconnectedStorages() {
        Set<BlockPos> storagesToRemove = new HashSet<>();

        for (BlockPos storagePos : detectedStorages.keySet()) {
            if (!isConnectedToGenerator(storagePos)) {
                storagesToRemove.add(storagePos);
            }
        }

        // Eliminar los almacenamientos desconectados
        for (BlockPos storagePos : storagesToRemove) {
            deleteStorage(storagePos);
            System.out.println("❌ Eliminado almacenamiento desconectado en: " + storagePos);
        }
    }


    private boolean isConnectedToGenerator(BlockPos storagePos) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                if (visited.contains(adjacentPos)) continue;
                visited.add(adjacentPos);

                BlockState adjacentState = level.getBlockState(adjacentPos);

                // Si encontramos el almacenamiento, significa que sigue conectado
                if (adjacentPos.equals(storagePos)) {
                    return true;
                }

                // Si es una tubería, seguimos buscando
                if (adjacentState.getBlock() instanceof PipeBlock) {
                    queue.add(adjacentPos);
                }
            }
        }

        return false; // Si nunca encontramos el almacenamiento, significa que está desconectado
    }

}

