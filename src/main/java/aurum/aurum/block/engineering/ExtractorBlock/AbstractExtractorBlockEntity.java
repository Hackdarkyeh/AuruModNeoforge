package aurum.aurum.block.engineering.ExtractorBlock;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.PipeBlock;
import aurum.aurum.init.ModBlocks;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractExtractorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
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
    public static final int EXTRACTING_COST_AURELITE_ORE = 1000;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int extractingProgress;
    int extractingTotalTime;


    public int energyCapacity; // Capacidad base mínima
    public int energyStoredVisible = 0; // Energía almacenada actualmente
    private boolean blockToExtractConnected = false;
    private EnergyGeneratorBlockEntity singleGeneratorInNetwork = null;
    private BlockState extractBlockState = null;

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    if (litDuration > Short.MAX_VALUE) {
                        // Neo: preserve litTime / litDuration ratio on the client as data slots are synced as shorts.
                        return Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE);
                    }

                    return AbstractExtractorBlockEntity.this.litTime;
                case 1:
                    return Math.min(AbstractExtractorBlockEntity.this.litDuration, Short.MAX_VALUE);
                case 2:
                    return AbstractExtractorBlockEntity.this.extractingProgress;
                case 3:
                    return AbstractExtractorBlockEntity.this.extractingTotalTime;
                case 4:
                    return AbstractExtractorBlockEntity.this.energyStoredVisible;
                case 5:
                    return AbstractExtractorBlockEntity.this.energyCapacity;

                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractExtractorBlockEntity.this.litTime = p_58434_;
                    break;
                case 1:
                    AbstractExtractorBlockEntity.this.litDuration = p_58434_;
                    break;
                case 2:
                    AbstractExtractorBlockEntity.this.extractingProgress = p_58434_;
                    break;
                case 3:
                    AbstractExtractorBlockEntity.this.extractingTotalTime = p_58434_;
                    break;
                case 4:
                    AbstractExtractorBlockEntity.this.energyStoredVisible = p_58434_;
                    break;
                case 5:
                    AbstractExtractorBlockEntity.this.energyCapacity = p_58434_;
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

    protected AbstractExtractorBlockEntity(
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
        this.extractingProgress = pTag.getInt("ExtractorTime");
        this.extractingTotalTime = pTag.getInt("CookTimeTotal");
        this.litDuration = this.getExtractingDuration();
        CompoundTag compoundtag = pTag.getCompound("RecipesUsed");

        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(s), compoundtag.getInt(s));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        //CompoundTag compoundtag = new CompoundTag();
        //this.recipesUsed.forEach((p_187449_, p_187450_) -> compoundtag.putInt(p_187449_.toString(), p_187450_));
        //pTag.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractExtractorBlockEntity pBlockEntity) {
        BlockPos coordsEnergyGenerator = pBlockEntity.findSingleGeneratorInNetwork();
        if (coordsEnergyGenerator != null) {
            pBlockEntity.singleGeneratorInNetwork = new EnergyGeneratorBlockEntity(coordsEnergyGenerator, pLevel.getBlockState(coordsEnergyGenerator));
        }else{
            pBlockEntity.singleGeneratorInNetwork = null;
        }
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;

        // Reducir tiempo de combustión si está encendido
        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        ItemStack pipeStack = pBlockEntity.items.get(0); // Slot de tuberías
        ItemStack extractorPeak = pBlockEntity.items.get(1); // Slot de pico extractor (velocidad de extracción y capacidad)
        ItemStack mineralOutput = pBlockEntity.items.get(2); // Slot de salida de minerales
        ItemStack protector = pBlockEntity.items.get(3); // Slot de protector de explosiones
        ItemStack rangeExtractor = pBlockEntity.items.get(4); // Slot de extractor de rango

        int maxDistance = 10; // Distancia máxima de búsqueda de bloques de diamante

        boolean hasEnergy = pBlockEntity.singleGeneratorInNetwork.hasEnergy();
        boolean hasPipe = !pipeStack.isEmpty();

        int maxStackSize = pBlockEntity.getMaxStackSize();

        // Iniciar combustión si no está encendido pero tiene combustible y puede procesar
        if (!pBlockEntity.isLit() && hasEnergy) {
            pBlockEntity.litTime = pBlockEntity.getExtractingDuration();
            pBlockEntity.litDuration = pBlockEntity.litTime;
            if (pBlockEntity.isLit()) {
                stateChanged = true;
                pBlockEntity.detectAdjacentBlocksGradual(pipeStack, maxDistance);

            }
        }

        // Procesar receta si está encendido
        if (pBlockEntity.isLit()) {
            pBlockEntity.extractingProgress++;
            if (pBlockEntity.extractingProgress == pBlockEntity.extractingTotalTime) {
                pBlockEntity.extractingProgress = 0;
                pBlockEntity.extractingTotalTime = getTotalCookTime(pLevel, pBlockEntity);

                stateChanged = true;
            }
        } else {
            pBlockEntity.extractingProgress = 0;
        }

        // Reducir progreso si no hay combustión
        if (!pBlockEntity.isLit() && pBlockEntity.extractingProgress > 0) {
            pBlockEntity.extractingProgress = Mth.clamp(pBlockEntity.extractingProgress - 2, 0, pBlockEntity.extractingTotalTime);
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

    protected int getExtractingDuration() {
        if (extractBlockState.getBlock() == ModBlocks.AURELITE_ORE.get()) {
            return singleGeneratorInNetwork.getExtractingDuration(EXTRACTING_COST_AURELITE_ORE);
        }
        return 0;
    }

    private static int getTotalCookTime(Level pLevel, AbstractExtractorBlockEntity pBlockEntity) {
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
            this.extractingTotalTime = getTotalCookTime(this.level, this);
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








    private void detectAdjacentBlocksGradual(ItemStack pipeStack, int maxDistance) {
        Set<BlockPos> visited = new HashSet<>();


        BlockPos currentPos = worldPosition;
        visited.add(currentPos);

        boolean diamondBlockFound = false;
        BlockPos diamondBlockPos = null;

        // Buscar bloque de diamante debajo hasta la distancia máxima
        for (int i = 1; i <= maxDistance; i++) {
            BlockPos belowPos = currentPos.below(i);
            assert level != null;
            BlockState belowState = level.getBlockState(belowPos);

            if (belowState.getBlock() == ModBlocks.AURELITE_ORE.get()) {
                diamondBlockFound = true;
                this.extractBlockState = belowState;
                diamondBlockPos = belowPos;
                break;
            }
        }

        if (!diamondBlockFound) {
            // No se encontró bloque de diamante dentro del rango
            return;
        }

        // Verificar si hay tuberías entre la posición actual y el bloque de diamante
        BlockPos checkPos = currentPos;
        boolean hasPipes = true;

        for (int i = 1; i <= maxDistance && !checkPos.equals(diamondBlockPos); i++) {
            checkPos = checkPos.below();
            BlockState checkState = level.getBlockState(checkPos);

            if (!(checkState.getBlock() instanceof PipeBlock)) {
                hasPipes = false;
                break;
            }
        }

        if (!hasPipes) {
            // Colocar tuberías si están disponibles en la interfaz
            checkPos = currentPos;
            for (int i = 1; i <= maxDistance && !checkPos.equals(diamondBlockPos); i++) {
                checkPos = checkPos.below();
                BlockState checkState = level.getBlockState(checkPos);

                if (!(checkState.getBlock() instanceof PipeBlock)) {
                    if (pipeStack.isEmpty()) {
                        // No hay tuberías disponibles
                        return;
                    }
                    placePipeAt(checkPos, pipeStack);
                }
            }
        }

        blockToExtractConnected = true;
    }

    private void placePipeAt(BlockPos pos , ItemStack pipeStack) {
        // Lógica para colocar una tubería en la posición especificada
        assert level != null;
        level.setBlockAndUpdate(pos, ModBlocks.PIPE_BLOCK.get().defaultBlockState());
        pipeStack.shrink(1);
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

