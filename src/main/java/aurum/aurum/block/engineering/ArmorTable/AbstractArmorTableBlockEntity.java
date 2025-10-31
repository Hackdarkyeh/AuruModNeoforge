package aurum.aurum.block.engineering.ArmorTable;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.energy.engineering.EnergyStorage;
import aurum.aurum.energy.engineering.IEnergyConsumer;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractArmorTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible, IEnergyConsumer {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int MINERAL_OUTPUT = 2;
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
    private final EnergyStorage energyStorage = new EnergyStorage(energyCapacity, 0, 0, 1);
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

                    return (int ) AbstractArmorTableBlockEntity.this.litTime * FLOAT_SCALING_FACTOR;
                case 1:
                    return (int) Math.min(AbstractArmorTableBlockEntity.this.litDuration, Short.MAX_VALUE) * FLOAT_SCALING_FACTOR;
                case 2:
                    return AbstractArmorTableBlockEntity.this.extractingProgress;
                case 3:
                    return AbstractArmorTableBlockEntity.this.extractingTotalTime;
                case 4:
                    return (int) AbstractArmorTableBlockEntity.this.energyStored * FLOAT_SCALING_FACTOR;
                case 5:
                    return AbstractArmorTableBlockEntity.this.energyCapacity;
                case 6:
                    return AbstractArmorTableBlockEntity.this.hasEnoughExperience ? 1 : 0;

                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractArmorTableBlockEntity.this.litTime = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
                case 1:
                    AbstractArmorTableBlockEntity.this.litDuration = p_58434_ / (float) FLOAT_SCALING_FACTOR;
                    break;
                case 2:
                    AbstractArmorTableBlockEntity.this.extractingProgress = p_58434_;
                    break;
                case 3:
                    AbstractArmorTableBlockEntity.this.extractingTotalTime = p_58434_;
                    break;
                case 4:
                    AbstractArmorTableBlockEntity.this.energyStored = p_58434_;
                    break;
                case 5:
                    AbstractArmorTableBlockEntity.this.energyCapacity = p_58434_;
                    break;
                case 6:
                    AbstractArmorTableBlockEntity.this.hasEnoughExperience = p_58434_ == 1;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractArmorTableBlockEntity(
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

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractArmorTableBlockEntity pBlockEntity) {
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

        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }
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

                    }
                }
            }
        }else{
            pBlockEntity.singleGeneratorInNetwork = null;
        }
    }

    protected float getExtractingDuration() {
        return this.energyStored;
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


    @Override
    public float getEnergyDemand() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean canReceiveEnergy() {
        return true;
    }

    @Override
    public void receiveEnergy(float amount) {
        this.energyStorage.addEnergy(amount, false);
        setChanged(); // Importante para guardar el estado
    }

    @Override
    public float getEnergyCapacity() {
        return 0;
    }

    @Override
    public float getEnergyStored() {
        return 0;
    }
}

