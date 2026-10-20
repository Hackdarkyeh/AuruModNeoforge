package aurum.aurum.block.engineering.ExtractorBlock;

import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.DarkPipeBlock;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.block.engineering.PipeSystem.PipeNetworkUtils;
import aurum.aurum.energy.engineering.EnergyStorage;
import aurum.aurum.energy.engineering.IEnergyConsumer;
import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractExtractorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible, IEnergyConsumer {
    protected static final int SLOT_INPUT = 0; // Tuberías
    protected static final int SLOT_PEAK = 1;  // Pico Extractor
    protected static final int SLOT_MINERAL_OUTPUT = 2; // Salida
    protected static final int SLOT_PROTECTOR = 3; // Protector
    protected static final int SLOT_RANGE = 4; // Rango

    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    private static final int SLOTS_COUNT = 5;

    // CACHÉS DE RED (Para no destruir el rendimiento)
    private final Map<BlockPos, EnergyStorageBlockEntity> detectedMasterStorages = new HashMap<>();
    private final Map<BlockPos, PipeNetworkUtils.IEnergyDevice> connectedDarkEnergyDevices = new HashMap<>();

    private int cleanNetworkTicks = 0;
    private int darkNetworkTicks = 0;
    private static final int MAX_ENERGY_PULL_PER_TICK = 1000;

    public int EXTRACTING_COST_AURELITE_ORE_TIME = 4000;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);

    private float litTime;
    float litDuration;
    int extractingProgress;
    int extractingTotalTime;

    public int MIN_AURELITE_PURITY = 30;
    public int MAX_AURELITE_PURITY = 60;

    private boolean hasEnoughExperience = false;
    private int maxDistance = 10;

    // Almacenamiento de Energía
    private final EnergyStorage energyStorage = new EnergyStorage(2000000, 0, 10, 10);
    private final EnergyStorage darkEnergyStorage = new EnergyStorage(20000, 0, 10, 10);

    public float energyCapacity = 2000000;

    private static final int DATA_COUNT = 11;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return (int) AbstractExtractorBlockEntity.this.litTime;
                case 1: return (int) Math.min(AbstractExtractorBlockEntity.this.litDuration, Short.MAX_VALUE);
                case 2: return AbstractExtractorBlockEntity.this.extractingProgress;
                case 3: return AbstractExtractorBlockEntity.this.extractingTotalTime;

                // Índices 4 y 5: Energía limpia actual (Bajos y Altos)
                case 4: return ((int) AbstractExtractorBlockEntity.this.energyStorage.getEnergyStored()) & 0xFFFF;
                case 5: return (((int) AbstractExtractorBlockEntity.this.energyStorage.getEnergyStored()) >> 16) & 0xFFFF;

                // Índices 6 y 7: Capacidad máxima limpia (Bajos y Altos)
                case 6: return ((int) AbstractExtractorBlockEntity.this.energyStorage.getCapacity()) & 0xFFFF;
                case 7: return (((int) AbstractExtractorBlockEntity.this.energyStorage.getCapacity()) >> 16) & 0xFFFF;

                // Índice 8: Experiencia
                case 8: return AbstractExtractorBlockEntity.this.hasEnoughExperience ? 1 : 0;

                // Índices 9 y 10: Energía oscura almacenada y su capacidad
                case 9: return (int) AbstractExtractorBlockEntity.this.darkEnergyStorage.getEnergyStored();
                case 10: return (int) AbstractExtractorBlockEntity.this.darkEnergyStorage.getCapacity();

                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: AbstractExtractorBlockEntity.this.litTime = value; break;
                case 1: AbstractExtractorBlockEntity.this.litDuration = value; break;
                case 2: AbstractExtractorBlockEntity.this.extractingProgress = value; break;
                case 3: AbstractExtractorBlockEntity.this.extractingTotalTime = value; break;

                // ✅ CORREGIDO: Índices 4 y 5 exactos para la energía limpia (coinciden con el get)
                case 4: {
                    int cur = (int) AbstractExtractorBlockEntity.this.energyStorage.getEnergyStored();
                    AbstractExtractorBlockEntity.this.energyStorage.setStoredEnergy((cur & 0xFFFF0000) | (value & 0xFFFF));
                    break;
                }
                case 5: {
                    int cur = (int) AbstractExtractorBlockEntity.this.energyStorage.getEnergyStored();
                    AbstractExtractorBlockEntity.this.energyStorage.setStoredEnergy((value << 16) | (cur & 0xFFFF));
                    break;
                }

                case 7: AbstractExtractorBlockEntity.this.hasEnoughExperience = (value == 1); break;
            }
        }

        @Override
        public int getCount() { return DATA_COUNT; }
    };

    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractExtractorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType) {
        super(pType, pPos, pBlockState);
        this.recipeType = pRecipeType;
    }

    private boolean isLit() {
        return this.energyStorage.getEnergyStored() > 0;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractExtractorBlockEntity pBlockEntity) {
        boolean wasLit = pBlockEntity.isLit();
        boolean stateChanged = false;

        // 1. Escaneo Optimizado de Redes (1 vez por segundo)
        pBlockEntity.cleanNetworkTicks++;
        if (pBlockEntity.cleanNetworkTicks >= 20) {
            pBlockEntity.detectCleanEnergyNetwork();
            pBlockEntity.cleanNetworkTicks = 0;
        }

        pBlockEntity.darkNetworkTicks++;
        if (pBlockEntity.darkNetworkTicks >= 20) {
            pBlockEntity.detectDarkEnergyNetwork();
            pBlockEntity.darkNetworkTicks = 0;
        }

        // 2. Verificar experiencia del jugador más cercano
        Player nearestPlayer = pLevel.getNearestPlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 5.0D, false);
        pBlockEntity.hasEnoughExperience = (nearestPlayer != null && nearestPlayer.experienceLevel >= 30);

        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        // 3. Extraer energía limpia de las BATERÍAS CACHEADAS si falta energía
        float energyDemand = pBlockEntity.energyStorage.getCapacity() - pBlockEntity.energyStorage.getEnergyStored();
        if (energyDemand > 0) {
            float amountToPull = Math.min(energyDemand, MAX_ENERGY_PULL_PER_TICK);
            if (pBlockEntity.pullCleanEnergyFromNetwork(amountToPull)) {
                stateChanged = true;
            }
        }

        // 4. Actualizar modificadores de ítems
        ItemStack pipeStack = pBlockEntity.items.get(SLOT_INPUT);
        pBlockEntity.updatePeak(pBlockEntity.items.get(SLOT_PEAK));
        pBlockEntity.updateRangeExtractor(pBlockEntity.items.get(SLOT_RANGE));

        boolean hasEnergy = pBlockEntity.energyStorage.getEnergyStored() >= pBlockEntity.getExtractingDuration();

        // 5. GESTIÓN DE EXTRACCIÓN Y CONSUMO CONDICIONAL (No consume si está desconectado)
        if (hasEnergy && pBlockEntity.manageAureliteConnection(pipeStack) && pBlockEntity.canExtractAurelite()) {
            pBlockEntity.litTime = pBlockEntity.getExtractingDuration();
            pBlockEntity.litDuration = pBlockEntity.litTime;

            if (pBlockEntity.darkEnergyStorage.getEnergyStored() > 0) {
                pBlockEntity.processDarkEnergy();
            }

            pBlockEntity.energyStorage.consumeEnergy(1, false);
            pBlockEntity.energyStorage.consumeEnergy(600, false);
            pBlockEntity.darkEnergyStorage.addEnergy(1, false);

            pBlockEntity.extractingProgress++;
            stateChanged = true;

            if (pBlockEntity.extractingProgress >= pBlockEntity.extractingTotalTime) {
                ItemStack currentStack = pBlockEntity.getItem(SLOT_MINERAL_OUTPUT);

                int randomPurity = pBlockEntity.MIN_AURELITE_PURITY + pLevel.random.nextInt(pBlockEntity.MAX_AURELITE_PURITY - pBlockEntity.MIN_AURELITE_PURITY + 1);
                ItemStack newItem = aurum.aurum.item.Aurelite_ingot.createWithPurity(ModItems.AURELITE_INGOT.get(), randomPurity);

                if (currentStack.isEmpty()) {
                    pBlockEntity.setItem(SLOT_MINERAL_OUTPUT, newItem);
                    pBlockEntity.extractingProgress = 0;
                    stateChanged = true;
                } else if (ItemStack.isSameItemSameComponents(currentStack, newItem) && currentStack.getCount() + newItem.getCount() <= currentStack.getMaxStackSize()) {
                    currentStack.grow(newItem.getCount());
                    pBlockEntity.extractingProgress = 0;
                    stateChanged = true;
                } else {
                    pBlockEntity.extractingProgress = pBlockEntity.extractingTotalTime;
                }
            }
        } else {
            pBlockEntity.extractingProgress = 0;
        }

        if (!pBlockEntity.isLit() && pBlockEntity.litTime > 0) {
            pBlockEntity.litTime = Mth.clamp(pBlockEntity.litTime - 2, 0, pBlockEntity.litDuration);
            stateChanged = true;
        }

        if (wasLit != pBlockEntity.isLit()) {
            pState = pState.setValue(AbstractFurnaceBlock.LIT, pBlockEntity.isLit());
            pLevel.setBlock(pPos, pState, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private void detectCleanEnergyNetwork() {
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

                if (adjacentState.getBlock() instanceof PipeBlock && !(adjacentState.getBlock() instanceof DarkPipeBlock)) {
                    queue.add(adjacentPos);
                } else if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                    BlockEntity be = level.getBlockEntity(adjacentPos);
                    if (be instanceof EnergyStorageBlockEntity storage) {
                        BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                        if (level.isLoaded(masterPos) && level.getBlockEntity(masterPos) instanceof EnergyStorageBlockEntity masterStorage) {
                            currentMasters.add(masterPos);
                            detectedMasterStorages.put(masterPos, masterStorage);
                        }
                    }
                }
            }
        }
        detectedMasterStorages.keySet().retainAll(currentMasters);
    }

    private void detectDarkEnergyNetwork() {
        if (level == null) return;
        Map<BlockPos, PipeNetworkUtils.IEnergyDevice> devices = PipeNetworkUtils.findConnectedDevices(level, worldPosition, PipeNetworkUtils.IEnergyDevice.class, DarkPipeBlock.class);
        this.connectedDarkEnergyDevices.clear();
        this.connectedDarkEnergyDevices.putAll(devices);
    }

    private boolean pullCleanEnergyFromNetwork(float amountNeeded) {
        boolean pulledAny = false;

        // 1. COMPROBACIÓN DIRECTA: Buscar primero en los bloques adyacentes inmediatos (sin caché de red)
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            if (level == null || !level.isLoaded(adjacentPos)) continue;

            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                BlockEntity be = level.getBlockEntity(adjacentPos);
                if (be instanceof EnergyStorageBlockEntity storage && !storage.isRemoved()) {
                    // Encontramos un almacenamiento adyacente directo físico
                    float available = storage.getEnergyStored();
                    if (available > 0) {
                        float toPull = Math.min(amountNeeded, available);
                        float actuallyPulled = storage.consumeEnergy(toPull, false);

                        if (actuallyPulled > 0) {
                            this.energyStorage.addEnergy(actuallyPulled, false);
                            amountNeeded -= actuallyPulled;
                            pulledAny = true;
                            if (amountNeeded <= 0) return true;
                        }
                    }
                }
            }
        }

        // 2. COMPROBACIÓN POR RED DE TUBERÍAS (Si no hay suficiente en los adyacentes, usa la caché de tuberías)
        if (detectedMasterStorages.isEmpty()) return pulledAny;

        Iterator<Map.Entry<BlockPos, EnergyStorageBlockEntity>> iterator = detectedMasterStorages.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, EnergyStorageBlockEntity> entry = iterator.next();
            BlockPos masterPos = entry.getKey();
            EnergyStorageBlockEntity storage = entry.getValue();

            if (storage.isRemoved() || level == null || !level.isLoaded(masterPos)) {
                iterator.remove();
                continue;
            }

            // Validar que el camino por tuberías sigue existiendo
            if (!isPathStillValid(worldPosition, masterPos)) {
                iterator.remove();
                continue;
            }

            float available = storage.getEnergyStored();
            if (available > 0) {
                float toPull = Math.min(amountNeeded, available);
                float actuallyPulled = storage.consumeEnergy(toPull, false);

                if (actuallyPulled > 0) {
                    this.energyStorage.addEnergy(actuallyPulled, false);
                    amountNeeded -= actuallyPulled;
                    pulledAny = true;
                    if (amountNeeded <= 0) break;
                }
            }
        }
        return pulledAny;
    }

    private boolean isPathStillValid(BlockPos startPos, BlockPos targetMasterPos) {
        if (level == null) return false;

        // 1. COMPROBACIÓN DIRECTA: Si el almacenamiento maestro (o sus adyacentes) está al lado del extractor sin tuberías
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = startPos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);

            if (adjacentState.getBlock() instanceof EnergyStorageBlock) {
                BlockEntity be = level.getBlockEntity(adjacentPos);
                if (be instanceof EnergyStorageBlockEntity storage) {
                    BlockPos masterPos = storage.isMaster() ? adjacentPos : storage.getMasterPos();
                    if (masterPos.equals(targetMasterPos)) {
                        return true; // Siguen pegados directamente, la conexión es válida
                    }
                }
            }
        }

        // 2. COMPROBACIÓN POR RED DE TUBERÍAS (Si no están pegados, busca el camino por tubos)
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

                if (adjacentState.getBlock() instanceof PipeBlock && !(adjacentState.getBlock() instanceof DarkPipeBlock)) {
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

    public void processDarkEnergy() {
        float availableEnergy = this.darkEnergyStorage.getEnergyStored();
        if (availableEnergy <= 0 || connectedDarkEnergyDevices.isEmpty()) return;

        List<PipeNetworkUtils.IEnergyDevice> receivers = connectedDarkEnergyDevices.values().stream()
                .filter(PipeNetworkUtils.IEnergyDevice::canReceiveEnergy)
                .collect(Collectors.toList());

        if (receivers.isEmpty()) return;

        float energyPerTick = Math.min(availableEnergy, 10f);

        for (PipeNetworkUtils.IEnergyDevice device : receivers) {
            if (availableEnergy <= 0) break;

            BlockPos devicePos = connectedDarkEnergyDevices.entrySet().stream()
                    .filter(entry -> entry.getValue() == device)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            if (devicePos == null) continue;

            float energyLossPercentage = PipeNetworkUtils.calculateEnergyLoss(level, worldPosition, devicePos, DarkPipeBlock.class);
            float energyToSend = Math.min(energyPerTick, availableEnergy);

            float energyAfterLoss = energyToSend * (1.0f - energyLossPercentage);
            float energyNeeded = device.getDarkEnergyCapacity() - device.getDarkEnergyStored();

            float actualEnergy = Math.min(energyAfterLoss, energyNeeded);

            if (actualEnergy > 0) {
                device.receiveDarkEnergy(actualEnergy);
                availableEnergy -= energyToSend;
            }
        }
        this.darkEnergyStorage.setStoredEnergy(availableEnergy);
    }

    private boolean manageAureliteConnection(ItemStack pipeStack) {
        BlockPos currentPos = worldPosition;
        boolean isFullyConnected = true;

        for (int i = 1; i <= maxDistance; i++) {
            BlockPos checkPos = currentPos.below(i);
            BlockState checkState = level.getBlockState(checkPos);

            if (checkState.getBlock() == ModBlocks.AURELITE_ORE.get()) {
                this.extractingTotalTime = EXTRACTING_COST_AURELITE_ORE_TIME;
                return isFullyConnected;
            }

            if (!(checkState.getBlock() instanceof PipeBlock)) {
                isFullyConnected = false;
                if (!pipeStack.isEmpty() && checkState.canBeReplaced()) {
                    // Colocación segura sin FACING para evitar crasheos en aurum:pipe
                    BlockState pipeState = ModBlocks.PIPE_BLOCK.get().defaultBlockState();
                    level.setBlockAndUpdate(checkPos, pipeState);
                    pipeStack.shrink(1);
                    return false;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    protected float getExtractingDuration() { return 500f; }
    private boolean canExtractAurelite() { return this.energyStorage.getEnergyStored() >= 500; }

    private void updatePeak(ItemStack extractorPeak) {
        if (extractorPeak.isEmpty()) { EXTRACTING_COST_AURELITE_ORE_TIME = 4000; return; }
        if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER3.get())) EXTRACTING_COST_AURELITE_ORE_TIME = 2500;
        else if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER2.get())) EXTRACTING_COST_AURELITE_ORE_TIME = 3000;
        else if (extractorPeak.is(ModItems.EXTRACTOR_PEAK_TIER1.get())) EXTRACTING_COST_AURELITE_ORE_TIME = 3500;
        else EXTRACTING_COST_AURELITE_ORE_TIME = 4000;
    }

    private void updateRangeExtractor(ItemStack rangeExtractor) {
        if (rangeExtractor.isEmpty()) { maxDistance = 10; return; }
        if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_5.get())) maxDistance = 35;
        else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_4.get())) maxDistance = 30;
        else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_3.get())) maxDistance = 25;
        else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_2.get())) maxDistance = 20;
        else if (rangeExtractor.is(ModItems.RANGE_EXTRACTOR_UPDATER_TIER_1.get())) maxDistance = 15;
    }

    public float extractDarkEnergy(float maxExtract) {
        float available = this.darkEnergyStorage.getEnergyStored();
        float toExtract = Math.min(available, maxExtract);
        if (toExtract > 0) {
            this.darkEnergyStorage.consumeEnergy(toExtract, false);
            this.setChanged();
        }
        return toExtract;
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.litTime = pTag.getInt("BurnTime");
        this.extractingProgress = pTag.getInt("ExtractorTime");
        this.extractingTotalTime = pTag.getInt("CookTimeTotal");
        this.energyStorage.setStoredEnergy(pTag.getInt("EnergyStorage"));
        this.litDuration = this.getExtractingDuration();
        this.darkEnergyStorage.setStoredEnergy(pTag.getInt("DarkEnergyStorage"));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putFloat("BurnTime", this.litTime);
        pTag.putInt("ExtractorTime", this.extractingProgress);
        pTag.putInt("CookTimeTotal", this.extractingTotalTime);
        pTag.putFloat("EnergyStorage", this.energyStorage.getEnergyStored());
        pTag.putFloat("DarkEnergyStorage", this.darkEnergyStorage.getEnergyStored());
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    @Override public int[] getSlotsForFace(Direction pSide) { return pSide == Direction.DOWN ? SLOTS_FOR_DOWN : (pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES); }
    @Override public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) { return this.canPlaceItem(pIndex, pItemStack); }
    @Override public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) { return pDirection == Direction.DOWN && pIndex == 1 ? pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET) : true; }
    @Override public int getContainerSize() { return this.items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return this.items; }
    @Override protected void setItems(NonNullList<ItemStack> pItems) { this.items = pItems; }

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

    @Override public boolean canPlaceItem(int pIndex, ItemStack pStack) { return pIndex != 2; }
    @Override public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {}
    @Nullable @Override public RecipeHolder<?> getRecipeUsed() { return null; }
    @Override public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {}
    @Override public void fillStackedContents(StackedContents pHelper) { for (ItemStack itemstack : this.items) pHelper.accountStack(itemstack); }

    @Override public float getEnergyStored() { return this.energyStorage.getEnergyStored(); }
    @Override public float getEnergyCapacity() { return this.energyStorage.getCapacity(); }
    @Override public void receiveEnergy(float amount) { this.energyStorage.addEnergy(amount, false); setChanged(); }
    @Override public boolean canReceiveEnergy() { return true; }
    @Override public boolean isActive() { return true; }
    @Override public float getEnergyDemand() { return this.energyCapacity - this.energyStorage.getEnergyStored(); }
}