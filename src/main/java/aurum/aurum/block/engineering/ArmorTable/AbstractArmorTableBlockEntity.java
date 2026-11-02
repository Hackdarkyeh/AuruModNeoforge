package aurum.aurum.block.engineering.ArmorTable;

import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.AbstractExtractorBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.DarkPipeBlock;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.block.engineering.PipeSystem.PipeNetworkUtils;
import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.item.ArmorItem.AurumArmor;
import aurum.aurum.item.Aurelite_ingot;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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
    protected static final int SLOT_PURIFIER = 5;
    private static final int SLOTS_COUNT = 6;

    // Purificador: convierte energía limpia en energía oscura (1000 limpia -> 1 oscura).
    // La durabilidad gastada por conversión es la que usaba DarkEnergyTable.
    private static final int PURIFIER_CLEAN_COST = 1000;
    private static final int PURIFIER_DARK_OUTPUT = 1;
    private static final int PURIFIER_DURABILITY_COST = 3;

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

    // Caché de los bancos de energía conectados (igual que el extractor, para no recorrer la red cada tick):
    // limpios por tuberías normales y oscuros por dark pipes
    private final Map<BlockPos, EnergyStorageBlockEntity> detectedMasterStorages = new HashMap<>();
    private final Map<BlockPos, EnergyStorageBlockEntity> detectedDarkMasterStorages = new HashMap<>();
    private int cleanNetworkTicks = 0;
    private static final int MAX_ENERGY_PULL_PER_TICK = 1000;

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

        // 0. Cada segundo buscamos los bancos de energía conectados
        //    (limpios por tuberías normales, oscuros por dark pipes)
        pBlockEntity.cleanNetworkTicks++;
        if (pBlockEntity.cleanNetworkTicks >= 20) {
            pBlockEntity.detectEnergyNetwork(false);
            pBlockEntity.detectEnergyNetwork(true);
            pBlockEntity.cleanNetworkTicks = 0;
        }

        // 1. Energía oscura. Primero rastreamos la red en busca de un Extractor de Energía Oscura
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

        // 1b. Si aún falta oscura, la tomamos de los bancos de energía oscura (por dark pipes o pegados a la mesa)
        float darkDemand = pBlockEntity.darkEnergyCapacity - pBlockEntity.darkEnergyStored;
        if (darkDemand > 0) {
            float amountToPull = Math.min(darkDemand, MAX_ENERGY_PULL_PER_TICK);
            if (pBlockEntity.pullFromNetwork(true, amountToPull)) {
                stateChanged = true;
            }
        }

        // 2. Energía limpia: la mesa la busca en los bancos limpios
        //    (primero los pegados a ella, luego los de la red de tuberías normales)
        float cleanDemand = pBlockEntity.cleanEnergyCapacity - pBlockEntity.cleanEnergyStored;
        if (cleanDemand > 0) {
            float amountToPull = Math.min(cleanDemand, MAX_ENERGY_PULL_PER_TICK);
            if (pBlockEntity.pullFromNetwork(false, amountToPull)) {
                stateChanged = true;
            }
        }

        // 2b. Purificador: convierte energía limpia en oscura
        if (purify(pBlockEntity)) {
            stateChanged = true;
        }

        // 3. Cargamos el ítem del slot de carga con la energía almacenada (oscura primero)
        if (chargeItem(pBlockEntity)) {
            stateChanged = true;

            // La vista previa de la mejora es una COPIA de la armadura: si hay una, la rehacemos para que
            // lleve la energía recién cargada (si no, al cogerla se perdería esa energía)
            if (!pBlockEntity.items.get(SLOT_ARMOR_OUT).isEmpty()) {
                pBlockEntity.updateOutputSlot();
            }
        }

        if (stateChanged) {
            setChanged(pLevel, pPos, pState);
        }
    }

    /**
     * Pasa energía de los tanques de la mesa al ítem del slot de carga (ver {@link EnergyItemCharger}).
     *
     * @return true si el ítem ha recibido energía este tick
     */
    private static boolean chargeItem(AbstractArmorTableBlockEntity table) {
        ItemStack stack = table.items.get(SLOT_ARMOR_IN);
        if (stack.isEmpty()) return false;

        int dark = (int) table.darkEnergyStored;
        int clean = (int) table.cleanEnergyStored;

        // Carga instantánea: el límite por tick pasa a ser toda la energía disponible.
        // Lo que se transfiere sigue limitado por la capacidad máxima del ítem.
        int instantRate = dark + clean;

        EnergyItemCharger.Result result = EnergyItemCharger.charge(
                stack,
                dark,
                clean,
                instantRate);

        table.darkEnergyStored -= result.dark();
        table.cleanEnergyStored -= result.clean();
        return result.any();
    }

    /**
     * Purificador: convierte energía limpia en oscura. Consume PURIFIER_CLEAN_COST de limpia y produce
     * PURIFIER_DARK_OUTPUT de oscura por tick, gastando durabilidad del purificador.
     *
     * @return true si ha hecho una conversión este tick
     */
    private static boolean purify(AbstractArmorTableBlockEntity table) {
        ItemStack purifier = table.items.get(SLOT_PURIFIER);
        if (purifier.isEmpty() || !purifier.is(ModItems.PURIFIER.get())) return false;
        if (purifier.getDamageValue() >= purifier.getMaxDamage()) return false;
        if (table.cleanEnergyStored < PURIFIER_CLEAN_COST) return false;
        if (table.darkEnergyStored + PURIFIER_DARK_OUTPUT > table.darkEnergyCapacity) return false;

        table.cleanEnergyStored -= PURIFIER_CLEAN_COST;
        table.darkEnergyStored += PURIFIER_DARK_OUTPUT;

        purifier.setDamageValue(purifier.getDamageValue() + PURIFIER_DURABILITY_COST);
        if (purifier.getDamageValue() >= purifier.getMaxDamage()) {
            table.items.set(SLOT_PURIFIER, ItemStack.EMPTY);
        }
        return true;
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

    // --- REDES DE BANCOS DE ENERGÍA (misma lógica que AbstractExtractorBlockEntity) ---
    // La limpia viaja por tuberías normales y la oscura por dark pipes. Un banco solo cuenta para el tanque
    // de su propio tipo: un banco oscuro nunca llena el tanque de limpia ni al revés.

    private static boolean isPipeOfType(BlockState state, boolean dark) {
        return dark
                ? state.getBlock() instanceof DarkPipeBlock
                : state.getBlock() instanceof PipeBlock && !(state.getBlock() instanceof DarkPipeBlock);
    }

    private Map<BlockPos, EnergyStorageBlockEntity> storagesFor(boolean dark) {
        return dark ? detectedDarkMasterStorages : detectedMasterStorages;
    }

    private void detectEnergyNetwork(boolean dark) {
        Map<BlockPos, EnergyStorageBlockEntity> detected = storagesFor(dark);
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
                if (visited.contains(adjacentPos) || level == null || !level.isLoaded(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);
                visited.add(adjacentPos);

                if (isPipeOfType(adjacentState, dark)) {
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    BlockEntity be = level.getBlockEntity(adjacentPos);
                    if (be instanceof EnergyStorageBlockEntity storage && storage.isDarkEnergy() == dark) {
                        BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                        if (level.isLoaded(masterPos) && level.getBlockEntity(masterPos) instanceof EnergyStorageBlockEntity masterStorage) {
                            currentMasters.add(masterPos);
                            detected.put(masterPos, masterStorage);
                        }
                    }
                }
            }
        }
        detected.keySet().retainAll(currentMasters);
    }

    /** Saca energía de un banco y la mete en el tanque de la mesa del tipo indicado. */
    private float takeFrom(EnergyStorageBlockEntity storage, boolean dark, float amountNeeded) {
        float available = storage.getEnergyStored();
        if (available <= 0) return 0f;

        float pulled = storage.consumeEnergy(Math.min(amountNeeded, available), false);
        if (pulled <= 0) return 0f;

        if (dark) {
            this.darkEnergyStored = Math.min(this.darkEnergyStored + pulled, this.darkEnergyCapacity);
        } else {
            this.cleanEnergyStored = Math.min(this.cleanEnergyStored + pulled, this.cleanEnergyCapacity);
        }
        return pulled;
    }

    /**
     * Toma energía de los bancos conectados del tipo indicado (dark = oscura, si no limpia).
     *
     * @return true si ha sacado algo
     */
    private boolean pullFromNetwork(boolean dark, float amountNeeded) {
        boolean pulledAny = false;

        // 1. COMPROBACIÓN DIRECTA: primero los bancos pegados a la mesa (sin caché de red)
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            if (level == null || !level.isLoaded(adjacentPos)) continue;

            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                BlockEntity be = level.getBlockEntity(adjacentPos);
                if (be instanceof EnergyStorageBlockEntity storage && !storage.isRemoved() && storage.isDarkEnergy() == dark) {
                    float pulled = takeFrom(storage, dark, amountNeeded);
                    if (pulled > 0) {
                        amountNeeded -= pulled;
                        pulledAny = true;
                        if (amountNeeded <= 0) return true;
                    }
                }
            }
        }

        // 2. COMPROBACIÓN POR RED DE TUBERÍAS (usa la caché de bancos detectados)
        Map<BlockPos, EnergyStorageBlockEntity> detected = storagesFor(dark);
        if (detected.isEmpty()) return pulledAny;

        Iterator<Map.Entry<BlockPos, EnergyStorageBlockEntity>> iterator = detected.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, EnergyStorageBlockEntity> entry = iterator.next();
            BlockPos masterPos = entry.getKey();
            EnergyStorageBlockEntity storage = entry.getValue();

            // Fuera de juego, descargado o con el banco ya cambiado de tipo
            if (storage.isRemoved() || level == null || !level.isLoaded(masterPos) || storage.isDarkEnergy() != dark) {
                iterator.remove();
                continue;
            }

            // Validar que el camino por tuberías sigue existiendo
            if (!isPathStillValid(worldPosition, masterPos, dark)) {
                iterator.remove();
                continue;
            }

            float pulled = takeFrom(storage, dark, amountNeeded);
            if (pulled > 0) {
                amountNeeded -= pulled;
                pulledAny = true;
                if (amountNeeded <= 0) break;
            }
        }
        return pulledAny;
    }

    private boolean isPathStillValid(BlockPos startPos, BlockPos targetMasterPos, boolean dark) {
        if (level == null) return false;

        // 1. COMPROBACIÓN DIRECTA: el banco maestro (o uno de sus bloques) sigue pegado a la mesa
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = startPos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);

            if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                BlockEntity be = level.getBlockEntity(adjacentPos);
                if (be instanceof EnergyStorageBlockEntity storage) {
                    BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                    if (masterPos.equals(targetMasterPos)) {
                        return true;
                    }
                }
            }
        }

        // 2. COMPROBACIÓN POR RED DE TUBERÍAS (si no están pegados, busca el camino por tubos del mismo tipo)
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int maxSteps = 150;
        int steps = 0;

        while (!queue.isEmpty() && steps < maxSteps) {
            BlockPos currentPos = queue.poll();
            steps++;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(direction);

                if (adjacentPos.equals(targetMasterPos)) {
                    return true;
                }

                if (visited.contains(adjacentPos) || !level.isLoaded(adjacentPos)) continue;

                BlockState adjacentState = level.getBlockState(adjacentPos);
                visited.add(adjacentPos);

                if (isPipeOfType(adjacentState, dark)) {
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    BlockEntity be = level.getBlockEntity(adjacentPos);
                    if (be instanceof EnergyStorageBlockEntity storage) {
                        BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                        if (masterPos.equals(targetMasterPos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // --- LÓGICA DE VISTA PREVIA (YUNQUE) ---

    public void updateOutputSlot() {
        ItemStack armorStack = this.items.get(SLOT_ARMOR_IN);
        ItemStack aureliteStack = this.items.get(SLOT_AURELITE_IN);

        // 1. Solo piezas del mod (Aurelite o armaduras autorizadas) y con un lingote de Aurelita
        if (AurumArmor.is(armorStack)
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

            // Respetamos el orden (currentExp, currentLevel, expToNextLevel) del record
            ArmorExpData updatedData = new ArmorExpData(newExp, currentLevel, expToNext);
            previewStack.set(ModDataComponents.ARMOR_EXP.get(), updatedData);

            // Colocamos el resultado en el slot de previsualización
            this.items.set(SLOT_ARMOR_OUT, previewStack);
        } else {
            this.items.set(SLOT_ARMOR_OUT, ItemStack.EMPTY);
        }

        this.setChanged();
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
        this.cleanEnergyStored = pTag.getFloat("CleanEnergyStorage");
        this.litDuration = this.getExtractingDuration();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putFloat("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        pTag.putFloat("DarkEnergyStorage", this.darkEnergyStored);
        pTag.putFloat("CleanEnergyStorage", this.cleanEnergyStored);
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
        if (pIndex == SLOT_PURIFIER) return pStack.is(ModItems.PURIFIER.get());
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