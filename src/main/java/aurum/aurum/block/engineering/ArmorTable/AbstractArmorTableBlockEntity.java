package aurum.aurum.block.engineering.ArmorTable;

import aurum.aurum.block.engineering.ExtractorBlock.AbstractExtractorBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.DarkPipeBlock;
import aurum.aurum.block.engineering.PipeSystem.PipeNetworkUtils;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.item.Aurelite_ingot;
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
import net.minecraft.world.item.ArmorItem;
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

public abstract class AbstractArmorTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible, PipeNetworkUtils.IEnergyDevice {

    protected static final int SLOT_ARMOR_IN = 0;
    protected static final int SLOT_AURELITE_IN = 1;
    protected static final int SLOT_ARMOR_OUT = 2; // Vista previa
    protected static final int SLOT_PROTECTOR = 3;
    protected static final int SLOT_RANGE_EXTRACTOR = 4;
    private static final int SLOTS_COUNT = 5;

    private static final int[] SLOTS_FOR_UP = new int[]{SLOT_ARMOR_IN};
    private static final int[] SLOTS_FOR_DOWN = new int[]{SLOT_ARMOR_OUT, SLOT_AURELITE_IN};
    private static final int[] SLOTS_FOR_SIDES = new int[]{SLOT_AURELITE_IN};

    private static final int FLOAT_SCALING_FACTOR = 1000;

    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);

    private float litTime;
    float litDuration;
    int extractingProgress;
    int extractingTotalTime;

    // Almacenamiento Dual
    public float cleanEnergyStored = 0;
    public int cleanEnergyCapacity = 2000000;

    public float darkEnergyStored = 0;
    public int darkEnergyCapacity = 200000;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return (int) AbstractArmorTableBlockEntity.this.litTime;
                case 1: return (int) Math.min(AbstractArmorTableBlockEntity.this.litDuration, Short.MAX_VALUE);
                case 2: return AbstractArmorTableBlockEntity.this.extractingProgress;
                case 3: return AbstractArmorTableBlockEntity.this.extractingTotalTime;

                // Energía Limpia (EC) - 32 bits divididos
                case 4: return ((int) AbstractArmorTableBlockEntity.this.cleanEnergyStored) & 0xFFFF;
                case 5: return (((int) AbstractArmorTableBlockEntity.this.cleanEnergyStored) >> 16) & 0xFFFF;
                case 6: return AbstractArmorTableBlockEntity.this.cleanEnergyCapacity & 0xFFFF;
                case 7: return (AbstractArmorTableBlockEntity.this.cleanEnergyCapacity >> 16) & 0xFFFF;

                // Energía Oscura (ED) - Directo / 16 bits
                case 8: return (int) AbstractArmorTableBlockEntity.this.darkEnergyStored;
                case 9: return AbstractArmorTableBlockEntity.this.darkEnergyCapacity;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: AbstractArmorTableBlockEntity.this.litTime = value; break;
                case 1: AbstractArmorTableBlockEntity.this.litDuration = value; break;
                case 2: AbstractArmorTableBlockEntity.this.extractingProgress = value; break;
                case 3: AbstractArmorTableBlockEntity.this.extractingTotalTime = value; break;
                case 4:
                    int curL = (int) AbstractArmorTableBlockEntity.this.cleanEnergyStored;
                    AbstractArmorTableBlockEntity.this.cleanEnergyStored = (curL & 0xFFFF0000) | (value & 0xFFFF);
                    break;
                case 5:
                    int curH = (int) AbstractArmorTableBlockEntity.this.cleanEnergyStored;
                    AbstractArmorTableBlockEntity.this.cleanEnergyStored = (value << 16) | (curH & 0xFFFF);
                    break;
                case 8: AbstractArmorTableBlockEntity.this.darkEnergyStored = value; break;
            }
        }

        @Override
        public int getCount() { return 10; }
    };

    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractArmorTableBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType) {
        super(pType, pPos, pBlockState);
        this.recipeType = pRecipeType;
    }

    // --- LÓGICA DE ACTUALIZACIÓN (TICK) Y RASTREO VAMPIRO ---

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractArmorTableBlockEntity pBlockEntity) {
        boolean stateChanged = false;

        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        // 1. Rastreamos la red en busca de un Extractor de Energía Oscura
        BlockPos containerPos = pBlockEntity.findDarkEnergyContainer();

        if (containerPos != null) {
            BlockEntity containerEntity = pLevel.getBlockEntity(containerPos);

            if (containerEntity instanceof AbstractExtractorBlockEntity extractor) {
                // ¿Cuánta energía le falta a la mesa?
                float demand = pBlockEntity.darkEnergyCapacity - pBlockEntity.darkEnergyStored;

                if (demand > 0) {
                    // Robamos un máximo de 1000 de energía por tick para no secar el extractor de golpe
                    float maxTransfer = Math.min(demand, 1000f);
                    float extracted = extractor.extractDarkEnergy(maxTransfer);

                    if (extracted > 0) {
                        pBlockEntity.darkEnergyStored += extracted;
                        stateChanged = true;
                    }
                }
            }
        }

        if (stateChanged) {
            setChanged(pLevel, pPos, pState);
        }
    }

    /**
     * Rastrea los bloques DarkPipeBlock hasta encontrar un AbstractExtractorBlockEntity.
     */
    private BlockPos findDarkEnergyContainer() {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            if (level == null) continue;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);
                if (visited.contains(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);
                BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);

                // Si encontramos el Extractor, devolvemos su posición
                if (adjacentEntity instanceof AbstractExtractorBlockEntity) {
                    return adjacentPos;
                }

                // Si es una Tubería Oscura, seguimos el camino
                if (adjacentState.getBlock() instanceof DarkPipeBlock) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            }
        }
        return null;
    }

    // --- LÓGICA DE VISTA PREVIA (YUNQUE) ---

    public void updateOutputSlot() {
        ItemStack armorStack = this.items.get(SLOT_ARMOR_IN);
        ItemStack aureliteStack = this.items.get(SLOT_AURELITE_IN);

        // 1. Verificamos si es una pieza de armadura y si hay un lingote de Aurelita
        if (!armorStack.isEmpty() && isArmorItem(armorStack)
                && !aureliteStack.isEmpty() && aureliteStack.is(ModItems.AURELITE_INGOT.get())) {

            int purity = Aurelite_ingot.getPurity(aureliteStack);
            if (purity <= 0) purity = 30; // Valor seguro base

            int xpGained = purity / 2;

            ItemStack previewStack = armorStack.copy();

            // 2. Usamos getOrDefault para que funcione con armaduras nuevas sin componentes previos
            ArmorExpData currentData = previewStack.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());

            int newExp = currentData.currentExp() + xpGained;
            int currentLevel = currentData.currentLevel();
            int expToNext = currentData.expToNextLevel();

            while (newExp >= expToNext) {
                newExp -= expToNext;
                currentLevel++;
                expToNext = (int) (expToNext * 1.5f);
            }

            // CORREGIDO: Respetamos el orden (currentExp, currentLevel, expToNextLevel) de tu record
            ArmorExpData updatedData = new ArmorExpData(newExp, currentLevel, expToNext);
            previewStack.set(ModDataComponents.ARMOR_EXP.get(), updatedData);

            // Colocamos el resultado en el slot de previsualización
            this.items.set(SLOT_ARMOR_OUT, previewStack);
        } else {
            this.items.set(SLOT_ARMOR_OUT, ItemStack.EMPTY);
        }

        this.setChanged();
    }

    private boolean isArmorItem(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem || stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));

        // Si cambian los ingredientes (Armor o Aurelite), actualizamos la vista previa
        if ((pIndex == SLOT_ARMOR_IN || pIndex == SLOT_AURELITE_IN) && !flag) {
            this.updateOutputSlot();
        }

        this.setChanged();
    }

    // --- IENERGY DEVICE (RED DE ENERGÍA OSCURA) ---

    @Override
    public boolean canReceiveEnergy() {
        return this.darkEnergyStored < this.darkEnergyCapacity;
    }

    @Override
    public float getDarkEnergyCapacity() {
        return this.darkEnergyCapacity;
    }

    @Override
    public float getDarkEnergyStored() {
        return this.darkEnergyStored;
    }

    @Override
    public void receiveDarkEnergy(float amount) {
        this.darkEnergyStored += amount;
        if (this.darkEnergyStored > this.darkEnergyCapacity) {
            this.darkEnergyStored = this.darkEnergyCapacity;
        }
        this.setChanged();
    }

    // --- CARGA Y GUARDADO ---

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.litTime = pTag.getInt("BurnTime");
        this.extractingProgress = pTag.getInt("ExtractorTime");
        this.extractingTotalTime = pTag.getInt("CookTimeTotal");
        this.darkEnergyStored = pTag.getFloat("DarkEnergyStorage");
        this.litDuration = this.getExtractingDuration();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putFloat("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        pTag.putFloat("DarkEnergyStorage", this.darkEnergyStored);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    // --- OTROS MÉTODOS VANILLA Y DE INVENTARIO ---

    private boolean isLit() {
        return this.litTime > 0;
    }

    protected float getExtractingDuration() {
        return this.darkEnergyStored;
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) return SLOTS_FOR_DOWN;
        return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
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
    public int getContainerSize() { return this.items.size(); }

    @Override
    protected NonNullList<ItemStack> getItems() { return this.items; }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) { this.items = pItems; }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == SLOT_ARMOR_OUT) return false;
        if (pIndex != SLOT_AURELITE_IN) return true;
        ItemStack itemstack = this.items.get(SLOT_AURELITE_IN);
        return pStack.getBurnTime(this.recipeType) > 0 || pStack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {}

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() { return null; }

    @Override
    public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {}

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }
}