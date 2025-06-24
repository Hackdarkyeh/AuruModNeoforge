package aurum.aurum.block.engineering.ExtractorBlock;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.PipeBlock;
import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModItems;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractExtractorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int MINERAL_OUTPUT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    private static final int SLOTS_COUNT = 5;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    public int EXTRACTING_COST_AURELITE_ORE_TIME = 4000;
    public static int EXTRACTING_COST_AURELITE_ORE =  1000000;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);
    private float litTime;
    float litDuration;
    int extractingProgress;
    int extractingTotalTime;

    private static int MAX_TRANSFER_RATE = 10; // Tasa de transferencia máxima
    private boolean hasEnoughExperience = false; // Indica si el jugador tiene suficiente experiencia para extraer
    private int maxDistance = 10; // Distancia máxima de búsqueda de bloques de diamante
    public int energyCapacity = 2000000; // Capacidad base mínima
    public float energyStored = 0; // Energía almacenada actualmente
    private EnergyGeneratorBlockEntity singleGeneratorInNetwork = null;

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
                        return Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE);
                    }

                    return (int )AbstractExtractorBlockEntity.this.litTime * FLOAT_SCALING_FACTOR;
                case 1:
                    return (int) Math.min(AbstractExtractorBlockEntity.this.litDuration, Short.MAX_VALUE) * FLOAT_SCALING_FACTOR;
                case 2:
                    return AbstractExtractorBlockEntity.this.extractingProgress;
                case 3:
                    return AbstractExtractorBlockEntity.this.extractingTotalTime;
                case 4:
                    return (int)AbstractExtractorBlockEntity.this.energyStored * FLOAT_SCALING_FACTOR;
                case 5:
                    return AbstractExtractorBlockEntity.this.energyCapacity;
                case 6:
                    return AbstractExtractorBlockEntity.this.hasEnoughExperience ? 1 : 0;

                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractExtractorBlockEntity.this.litTime = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
                case 1:
                    AbstractExtractorBlockEntity.this.litDuration = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
                case 2:
                    AbstractExtractorBlockEntity.this.extractingProgress = p_58434_;
                    break;
                case 3:
                    AbstractExtractorBlockEntity.this.extractingTotalTime = p_58434_;
                    break;
                case 4:
                    AbstractExtractorBlockEntity.this.energyStored = p_58434_;
                    break;
                case 5:
                    AbstractExtractorBlockEntity.this.energyCapacity = p_58434_;
                    break;
                case 6:
                    AbstractExtractorBlockEntity.this.hasEnoughExperience = p_58434_ == 1;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractExtractorBlockEntity(
            BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType
    ) {
        super(pType, pPos, pBlockState);
        this.recipeType = pRecipeType;
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
        this.energyStored = pTag.getInt("EnergyStorage");
        this.litDuration = this.getExtractingDuration();
        //CompoundTag compoundtag = pTag.getCompound("RecipesUsed");
        /*
        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(s), compoundtag.getInt(s));
        }*/
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putFloat("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        pTag.putFloat("EnergyStorage", this.energyStored);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        //CompoundTag compoundtag = new CompoundTag();
        //this.recipesUsed.forEach((p_187449_, p_187450_) -> compoundtag.putInt(p_187449_.toString(), p_187450_));
        //pTag.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractExtractorBlockEntity pBlockEntity) {
        BlockPos coordsEnergyGenerator = pBlockEntity.findSingleGeneratorInNetwork();
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;
        if (pLevel.hasNearbyAlivePlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 5.0D)) {
            pBlockEntity.hasEnoughExperience = true;
            if (pLevel.getNearestPlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 5, false).experienceLevel == 30) {
                pBlockEntity.hasEnoughExperience = true;
            }else{
                pBlockEntity.hasEnoughExperience = false;
            }
        }


        // Reducir tiempo de combustión si está encendido
        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        ItemStack pipeStack = pBlockEntity.items.get(0); // Slot de tuberías
        ItemStack extractorPeak = pBlockEntity.items.get(1); // Slot de pico extractor (velocidad de extracción y capacidad)
        updatePeak(pBlockEntity, extractorPeak);
        ItemStack mineralOutput = pBlockEntity.items.get(2); // Slot de salida de minerales
        ItemStack protector = pBlockEntity.items.get(3); // Slot de protector de explosiones
        ItemStack rangeExtractor = pBlockEntity.items.get(4); // Slot de extractor de rango
        updateRangeExtractor(pBlockEntity, rangeExtractor);

        if (coordsEnergyGenerator != null) {
            BlockEntity energyGeneratorBlockEntity = pBlockEntity.level.getBlockEntity(coordsEnergyGenerator);
            if (energyGeneratorBlockEntity instanceof EnergyGeneratorBlockEntity) {
                pBlockEntity.singleGeneratorInNetwork = (EnergyGeneratorBlockEntity) energyGeneratorBlockEntity;
                boolean singleGeneratorInNetworkhasEnergy = pBlockEntity.singleGeneratorInNetwork.hasEnergy();
                if (singleGeneratorInNetworkhasEnergy){
                    pBlockEntity.singleGeneratorInNetwork.extractEnergyFromNetwork(pBlockEntity);
                }
                // Iniciar combustión si no está encendido pero tiene combustible y puede procesar
                boolean hasEnergy = pBlockEntity.energyStored >= pBlockEntity.getExtractingDuration();
                if (hasEnergy) {
                    pBlockEntity.litTime = pBlockEntity.getExtractingDuration();
                    pBlockEntity.litDuration = pBlockEntity.litTime;
                    if (pBlockEntity.isLit()) {
                        stateChanged = true;
                        pBlockEntity.detectAdjacentBlocksGradual(pipeStack, pBlockEntity.maxDistance);

                    }
                }
            }
        }else{
            pBlockEntity.singleGeneratorInNetwork = null;
        }

        // Procesar receta si está encendido
        if (pBlockEntity.isLit() && pBlockEntity.hasConnectedPipesToAurelite(pBlockEntity.maxDistance) && pBlockEntity.canExtractAurelite() ) {
            pBlockEntity.extractingProgress++;
            pBlockEntity.energyStored -= 500;

            if (pBlockEntity.extractingProgress == pBlockEntity.extractingTotalTime) {
                pBlockEntity.extractingProgress = 0;

                // Obtener el ItemStack actual en el slot de salida
                ItemStack currentStack = pBlockEntity.getItem(MINERAL_OUTPUT);
                ItemStack newItem = ModBlocks.AURELITE_ORE.get().asItem().getDefaultInstance();

                if (currentStack.isEmpty()) {
                    // Si el slot está vacío, colocar el nuevo ítem
                    pBlockEntity.setItem(MINERAL_OUTPUT, newItem);
                } else if (currentStack.is(newItem.getItem())) {
                    // Si el mismo ítem ya está en el slot, aumentar la cantidad
                    currentStack.grow(newItem.getCount());
                }

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

    protected float getExtractingDuration() {
        return this.energyStored;
    }

    private boolean canExtractAurelite() {
        return this.energyStored >= 500;
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    private static void updatePeak(AbstractExtractorBlockEntity pBlockEntity, ItemStack extractorPeak) {
        if (extractorPeak.isEmpty()) {
            return;
        }
        if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER1.get().asItem())) {
            pBlockEntity.EXTRACTING_COST_AURELITE_ORE_TIME = 3500;
        } else if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER2.get().asItem())) {
            pBlockEntity.EXTRACTING_COST_AURELITE_ORE_TIME = 3000;
        } else if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER3.get().asItem())) {
            pBlockEntity.EXTRACTING_COST_AURELITE_ORE_TIME = 2500;
        }else{
            pBlockEntity.EXTRACTING_COST_AURELITE_ORE_TIME = 4000;
        }
    }

    private static void updateRangeExtractor(AbstractExtractorBlockEntity pBlockEntity, ItemStack rangeExtractor) {
        if (rangeExtractor.isEmpty()) {
            return;
        }
        if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_1.get().asItem())) {
            pBlockEntity.maxDistance = 15;
        } else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_2.get().asItem())) {
            pBlockEntity.maxDistance = 20;
        } else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_3.get().asItem())) {
            pBlockEntity.maxDistance = 25;
        } else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_4.get().asItem())) {
            pBlockEntity.maxDistance = 30;
        }  else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_5.get().asItem())) {
            pBlockEntity.maxDistance = 35;
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

        boolean aureliteOreBlockFound = false;
        BlockPos diamondBlockPos = null;

        // Buscar bloque de diamante debajo hasta la distancia máxima
        for (int i = 1; i <= maxDistance; i++) {
            BlockPos belowPos = currentPos.below(i);
            assert level != null;
            BlockState belowState = level.getBlockState(belowPos);

            if (belowState.getBlock() == ModBlocks.AURELITE_ORE.get()) {
                aureliteOreBlockFound = true;
                this.extractingTotalTime = EXTRACTING_COST_AURELITE_ORE_TIME;
                diamondBlockPos = belowPos;
                break;
            }
        }

        if (!aureliteOreBlockFound) {
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
                if (diamondBlockPos.equals(checkPos)) {
                    // Llegamos al bloque de diamante
                    break;
                }
                if (!(checkState.getBlock() instanceof PipeBlock)) {
                    if (pipeStack.isEmpty()) {
                        // No hay tuberías disponibles
                        return;
                    }
                    placePipeAt(checkPos, pipeStack);
                }
            }
        }

    }

    private boolean hasConnectedPipesToAurelite(int maxDistance) {
        BlockPos currentPos = worldPosition;

        // Buscar el bloque de Aurelita en el rango especificado
        BlockPos aureliteBlockPos = null;
        for (int i = 1; i <= maxDistance; i++) {
            BlockPos belowPos = currentPos.below(i);
            assert level != null;
            BlockState belowState = level.getBlockState(belowPos);

            if (belowState.getBlock() == ModBlocks.AURELITE_ORE.get()) {
                aureliteBlockPos = belowPos;
                break;
            }
        }

        // Si no se encontró Aurelita, retornar false
        if (aureliteBlockPos == null) {
            return false;
        }

        // Verificar si hay tuberías conectando el bloque actual con el bloque de Aurelita
        BlockPos checkPos = currentPos;
        for (int i = 1; i <= maxDistance && !checkPos.equals(aureliteBlockPos); i++) {
            checkPos = checkPos.below();
            BlockState checkState = level.getBlockState(checkPos);

            if (!(checkState.getBlock() instanceof PipeBlock) && !checkPos.equals(aureliteBlockPos)) {
                return false; // Se encontró un bloque que no es tubería en el camino
            }
        }

        return true; // Hay conexión completa de tuberías hasta la Aurelita
    }


    private void placePipeAt(BlockPos pos , ItemStack pipeStack) {
        // Lógica para colocar una tubería en la posición especificada
        assert level != null;
        BlockState pipeState = ModBlocks.PIPE_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN); // Asegurar que la tubería esté orientada hacia abajo
        level.setBlockAndUpdate(pos, pipeState);
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

