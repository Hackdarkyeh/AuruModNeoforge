package aurum.aurum.block.engineering.EnergyStorageBlock;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.AbstractEnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.PipeSystem.PipeBlock;
import aurum.aurum.energy.engineering.EnergyStorage;
import aurum.aurum.energy.engineering.IEnergyStorage;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public class EnergyStorageBlockEntity extends BlockEntity implements IEnergyStorage {
    private static final int BASE_CAPACITY = 10000;
    private static final int TRANSFER_RATE = 100;
    private static final int MAX_NETWORK_SIZE = 16;
    private static final int NETWORK_UPDATE_INTERVAL = 20; // ticks

    final EnergyStorage energyStorage;
    public Set<BlockPos> mergedBlocks = new HashSet<>();
    boolean isMaster = true;
    BlockPos masterPos;
    private int lastNetworkSize = 1;
    private long lastUpdateTick = 0;

    // ---- Nivel de llenado visual ----
    public static final int FILL_STEPS = 64;
    private int fillLevel = 0;
    private int lastFillRatioKey = -1;
    private int lastFillBlocksKey = 0;

    public float displayFill = 0f;
    public long lastRenderMillis = 0L;

    private final Set<BlockPos> subscribedGenerators = new HashSet<>();

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BLOCK.get(), pos, state);
        this.energyStorage = new EnergyStorage(BASE_CAPACITY, TRANSFER_RATE, TRANSFER_RATE, TRANSFER_RATE);
        this.masterPos = pos;
        this.mergedBlocks.add(pos);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyStorageBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        try {
            long currentTick = level.getGameTime();

            // ✅ VALIDACIÓN INSTANTÁNEA: Si es esclavo, comprobar cada pocos ticks si su maestro sigue existiendo físicamente o conectado
            if (!entity.isMaster && currentTick % 10 == 0) {
                if (!entity.isConnectedToMaster()) {
                    entity.becomeMaster();
                    return;
                }
            }

            if (currentTick - entity.lastUpdateTick >= NETWORK_UPDATE_INTERVAL) {
                entity.checkForAdjacentBlocks();
                entity.lastUpdateTick = currentTick;
            }

            if (entity.isMaster && currentTick % 2 == 0) {
                if (currentTick % 100 == 0) {
                    entity.lastFillRatioKey = -1;
                }
                entity.updateFillDistribution();
            }
        } catch (Exception e) {
            System.err.println("Error in EnergyStorageBlockEntity tick: " + e.getMessage());
        }
    }

    private boolean isValidPosition(@Nullable BlockPos pos) {
        return pos != null && level.isLoaded(pos);
    }

    private void promoteNewMaster() {
        BlockPos newMasterPos = mergedBlocks.stream()
                .filter(pos -> !pos.equals(this.worldPosition) && isValidPosition(pos))
                .findFirst()
                .orElse(null);

        if (newMasterPos != null) {
            BlockEntity be = level.getBlockEntity(newMasterPos);
            if (be instanceof EnergyStorageBlockEntity newMaster) {
                float energyPerBlock = this.energyStorage.getEnergyStored() / mergedBlocks.size();
                float newEnergy = energyPerBlock * (mergedBlocks.size() - 1);

                newMaster.isMaster = true;
                newMaster.masterPos = newMasterPos;
                newMaster.mergedBlocks = new HashSet<>(this.mergedBlocks);
                newMaster.mergedBlocks.remove(this.worldPosition);
                newMaster.energyStorage.setStoredEnergy(newEnergy);
                newMaster.updateNetwork();
                newMaster.setChanged();
            }
        }
    }

    private void notifyMasterAboutRemoval() {
        if (isValidPosition(masterPos)) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof EnergyStorageBlockEntity master) {
                master.mergedBlocks.remove(this.worldPosition);

                float currentEnergy = master.getEnergyStored();
                float newEnergy = currentEnergy * (master.mergedBlocks.size()) / (master.mergedBlocks.size() + 1);
                master.energyStorage.setStoredEnergy(newEnergy);

                master.updateNetwork();
                master.setChanged();
            }
        }
    }

    void updateNetwork() {
        if (!isMaster || level == null || level.isClientSide) return;

        int newSize = Math.max(1, mergedBlocks.size());
        if (newSize == lastNetworkSize) return;

        int totalCapacity = BASE_CAPACITY * newSize;
        float currentEnergy = this.energyStorage.getEnergyStored();

        if (currentEnergy > totalCapacity) {
            currentEnergy = totalCapacity;
        }

        this.energyStorage.setMaxEnergyStored(totalCapacity);
        this.energyStorage.setStoredEnergy(currentEnergy);
        this.lastNetworkSize = newSize;

        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition) && isValidPosition(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity slave) {
                    slave.isMaster = false;
                    slave.masterPos = this.worldPosition;
                    slave.mergedBlocks.clear();
                    slave.setChanged();
                    level.sendBlockUpdated(pos, slave.getBlockState(), slave.getBlockState(), 3);
                }
            }
        }

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        notifyConnectedGenerators();
    }

    // IEnergyStorage implementation con validación de seguridad integrada
    @Override
    public float getEnergyStored() {
        if (!isMaster) {
            if (isValidPosition(masterPos) && isConnectedToMaster()) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    return master.getEnergyStored();
                }
            } else {
                // Si perdió la conexión, actúa como entidad independiente aislada
                return energyStorage.getEnergyStored();
            }
        }
        return energyStorage.getEnergyStored();
    }

    @Override
    public float getCapacity() {
        if (!isMaster) {
            if (isValidPosition(masterPos) && isConnectedToMaster()) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    return master.getCapacity();
                }
            }
        }
        return energyStorage.getCapacity();
    }

    @Override
    public float addEnergy(float amount, boolean simulate) {
        if (!isMaster) {
            if (isValidPosition(masterPos) && isConnectedToMaster()) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    return master.addEnergy(amount, simulate);
                }
            }
            return 0;
        }
        float result = energyStorage.addEnergy(amount, simulate);
        if (result > 0 && !simulate) setChanged();
        return result;
    }

    @Override
    public float consumeEnergy(float amount, boolean simulate) {
        if (!isMaster) {
            if (isValidPosition(masterPos) && isConnectedToMaster()) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    return master.consumeEnergy(amount, simulate);
                }
            }
            return 0; // 🛑 Si está desconectado, no permite robar energía del maestro fantasma
        }
        float result = energyStorage.consumeEnergy(amount, simulate);
        if (result > 0 && !simulate) setChanged();
        return result;
    }

    @Override
    public void setStoredEnergy(float amount) {
        if (!isMaster) {
            if (isValidPosition(masterPos) && isConnectedToMaster()) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    master.setStoredEnergy(amount);
                }
            }
            return;
        }
        energyStorage.setStoredEnergy(amount);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsMaster", isMaster);
        tag.putLong("MasterPos", masterPos.asLong());

        long[] mergedArray = mergedBlocks.stream()
                .mapToLong(BlockPos::asLong)
                .toArray();
        tag.putLongArray("MergedBlocks", mergedArray);

        energyStorage.saveEnergyToTag(tag);
        tag.putInt("FillLevel", fillLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isMaster = tag.getBoolean("IsMaster");
        masterPos = BlockPos.of(tag.getLong("MasterPos"));

        mergedBlocks.clear();
        long[] mergedArray = tag.getLongArray("MergedBlocks");
        for (long pos : mergedArray) {
            mergedBlocks.add(BlockPos.of(pos));
        }

        energyStorage.loadEnergyFromTag(tag);
        lastNetworkSize = mergedBlocks.size();
        fillLevel = tag.getInt("FillLevel");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public boolean isDarkEnergy() {
        return false;
    }

    public float getFillFraction() {
        return fillLevel / (float) FILL_STEPS;
    }

    private void setFillLevel(int steps) {
        if (fillLevel == steps) return;
        fillLevel = steps;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void updateFillDistribution() {
        if (!isMaster || level == null || level.isClientSide) return;

        float capacity = getCapacity();
        float ratio = capacity > 0 ? Mth.clamp(getEnergyStored() / capacity, 0f, 1f) : 0f;
        int ratioKey = Math.round(ratio * 4096f);
        int blocksKey = mergedBlocks.hashCode();
        if (ratioKey == lastFillRatioKey && blocksKey == lastFillBlocksKey) return;
        lastFillRatioKey = ratioKey;
        lastFillBlocksKey = blocksKey;

        TreeMap<Integer, List<BlockPos>> layers = new TreeMap<>();
        for (BlockPos p : mergedBlocks) {
            layers.computeIfAbsent(p.getY(), y -> new ArrayList<>()).add(p);
        }

        float remaining = ratio * mergedBlocks.size();
        for (List<BlockPos> layer : layers.values()) {
            float layerFill = Mth.clamp(remaining / layer.size(), 0f, 1f);
            if (layerFill > 0.9999f) layerFill = 1f;
            remaining -= layerFill * layer.size();

            int steps = Math.round(layerFill * FILL_STEPS);
            if (layerFill > 0.0001f && steps == 0) steps = 1;
            if (layerFill < 1f && steps >= FILL_STEPS) steps = FILL_STEPS - 1;

            for (BlockPos p : layer) {
                if (!isValidPosition(p)) continue;
                if (level.getBlockEntity(p) instanceof EnergyStorageBlockEntity storage) {
                    storage.setFillLevel(steps);
                }
            }
        }
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean canReceive() {
        return energyStorage.canReceive();
    }

    public boolean isMaster() {
        return isMaster;
    }

    @Override
    public boolean canExtract() {
        return energyStorage.canExtract();
    }

    @Override
    public CompoundTag saveEnergyToTag(CompoundTag tag) {
        return null;
    }

    @Override
    public void loadEnergyFromTag(CompoundTag tag) {}

    @Override
    public float receiveEnergy(float maxReceive, boolean simulate) {
        return addEnergy(maxReceive, simulate);
    }

    @Override
    public float getRemainingCapacity() {
        return getCapacity() - getEnergyStored();
    }

    private void notifyConnectedGenerators() {
        if (level == null || level.isClientSide || !isMaster) return;

        Set<BlockPos> connectedGenerators = findConnectedGenerators();
        for (BlockPos generatorPos : connectedGenerators) {
            BlockEntity be = level.getBlockEntity(generatorPos);
            if (be instanceof AbstractEnergyGeneratorBlockEntity generator) {
                generator.onStorageNetworkUpdated(
                        this.getEnergyStored(),
                        this.getCapacity(),
                        this.mergedBlocks.size(),
                        this.worldPosition
                );
            }
        }

        Iterator<BlockPos> iterator = subscribedGenerators.iterator();
        while (iterator.hasNext()) {
            BlockPos generatorPos = iterator.next();
            if (level.isLoaded(generatorPos)) {
                BlockEntity be = level.getBlockEntity(generatorPos);
                if (be instanceof AbstractEnergyGeneratorBlockEntity generator) {
                    generator.onStorageNetworkUpdated(
                            this.getEnergyStored(),
                            this.getCapacity(),
                            this.mergedBlocks.size(),
                            this.worldPosition
                    );
                } else {
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

    private Set<BlockPos> findConnectedGenerators() {
        Set<BlockPos> generators = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(this.worldPosition);
        visited.add(this.worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);

                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                BlockState neighborState = level.getBlockState(neighbor);

                if (neighborState.getBlock() instanceof PipeBlock) {
                    queue.add(neighbor);
                } else {
                    BlockEntity be = level.getBlockEntity(neighbor);
                    if (be instanceof AbstractEnergyGeneratorBlockEntity) {
                        generators.add(neighbor);
                    }
                }
            }
        }
        return generators;
    }

    public void syncWithGenerator(AbstractEnergyGeneratorBlockEntity generator) {
        if (level != null && !level.isClientSide && isMaster) {
            subscribedGenerators.add(generator.getBlockPos());
            generator.onStorageNetworkUpdated(this.getEnergyStored(), this.getCapacity(), this.mergedBlocks.size(), this.worldPosition);
        }
    }

    private void checkForAdjacentBlocks() {
        if (!isMaster) return;

        Set<BlockPos> oldMergedBlocks = new HashSet<>(this.mergedBlocks);
        Set<BlockPos> newMergedBlocks = new HashSet<>();
        Set<BlockPos> toCheck = new HashSet<>();
        toCheck.add(this.worldPosition);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.iterator().next();
            toCheck.remove(current);

            if (!isValidPosition(current)) continue;

            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof EnergyStorageBlockEntity) {
                newMergedBlocks.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (isValidPosition(neighbor) && !newMergedBlocks.contains(neighbor)) {
                        toCheck.add(neighbor);
                    }
                }
            }
        }

        // Si la red cambió (se separaron bloques)
        if (!newMergedBlocks.equals(oldMergedBlocks)) {
            float oldEnergy = this.getEnergyStored();
            float oldCapacity = this.getCapacity();
            int oldSize = oldMergedBlocks.size();

            // Detectar qué bloques se han separado del maestro principal
            Set<BlockPos> lostBlocks = new HashSet<>(oldMergedBlocks);
            lostBlocks.removeAll(newMergedBlocks);

            // Actualizar la red principal con los bloques que se quedan
            this.mergedBlocks = newMergedBlocks;
            int newSize = Math.max(1, newMergedBlocks.size());
            int newCapacity = BASE_CAPACITY * newSize;

            float energyPerBlock = oldSize > 0 ? (oldEnergy / oldSize) : 0f;
            float mainNewEnergy = energyPerBlock * newSize;

            this.energyStorage.setMaxEnergyStored(newCapacity);
            this.energyStorage.setStoredEnergy(mainNewEnergy);
            this.lastNetworkSize = newSize;

            updateSlaves();
            notifyConnectedGenerators();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            // ✅ DISTRIBUIR ENERGÍA PROPORCIONALMENTE A LOS BLOQUES QUE SE SEPARARON
            if (!lostBlocks.isEmpty() && energyPerBlock > 0) {
                processLostBlocks(lostBlocks, energyPerBlock);
            }
        }
    }

    private void processLostBlocks(Set<BlockPos> lostBlocks, float energyPerBlock) {
        Set<BlockPos> remainingLost = new HashSet<>(lostBlocks);

        while (!remainingLost.isEmpty()) {
            BlockPos seed = remainingLost.iterator().next();
            Set<BlockPos> subNetwork = findPlayerSubNetwork(seed, remainingLost);
            remainingLost.removeAll(subNetwork);

            if (subNetwork.isEmpty()) continue;

            // Elegir el primer bloque de este grupo como el nuevo maestro
            BlockPos newMasterPos = subNetwork.iterator().next();
            BlockEntity be = level.getBlockEntity(newMasterPos);
            if (be instanceof EnergyStorageBlockEntity newMaster) {
                newMaster.isMaster = true;
                newMaster.masterPos = newMasterPos;
                newMaster.mergedBlocks = new HashSet<>(subNetwork);

                int subCapacity = BASE_CAPACITY * subNetwork.size();
                float subEnergy = energyPerBlock * subNetwork.size(); // ✅ Heredan exactamente su energía proporcional

                newMaster.energyStorage.setMaxEnergyStored(subCapacity);
                newMaster.energyStorage.setStoredEnergy(subEnergy);
                newMaster.lastNetworkSize = subNetwork.size();

                // Configurar a los esclavos de este subgrupo
                for (BlockPos pos : subNetwork) {
                    if (!pos.equals(newMasterPos) && isValidPosition(pos)) {
                        BlockEntity slaveBe = level.getBlockEntity(pos);
                        if (slaveBe instanceof EnergyStorageBlockEntity slave) {
                            slave.isMaster = false;
                            slave.masterPos = newMasterPos;
                            slave.mergedBlocks.clear();
                            slave.setChanged();
                            level.sendBlockUpdated(pos, slave.getBlockState(), slave.getBlockState(), 3);
                        }
                    }
                }

                newMaster.setChanged();
                level.sendBlockUpdated(newMasterPos, newMaster.getBlockState(), newMaster.getBlockState(), 3);
                newMaster.notifyConnectedGenerators();
            }
        }
    }

    private Set<BlockPos> findPlayerSubNetwork(BlockPos seed, Set<BlockPos> availableBlocks) {
        Set<BlockPos> network = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(seed);
        network.add(seed);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (availableBlocks.contains(neighbor) && !network.contains(neighbor)) {
                    network.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return network;
    }

    private void redistributeEnergyAfterSplit(float oldEnergy, float oldCapacity) {
        if (level == null || level.isClientSide) return;

        int newSize = Math.max(1, mergedBlocks.size());
        int newCapacity = BASE_CAPACITY * newSize;

        // Mantener la proporción exacta de llenado (porcentaje) que tenía el bloque maestro antes de dividirse
        float energyRatio = oldCapacity > 0 ? Mth.clamp(oldEnergy / oldCapacity, 0f, 1f) : 0f;
        float newEnergy = newCapacity * energyRatio;

        this.energyStorage.setMaxEnergyStored(newCapacity);
        this.energyStorage.setStoredEnergy(newEnergy);
        this.lastNetworkSize = newSize;

        findAndSetupNewMasters(); // ✅ Sin argumentos, como espera el método original
        updateSlaves();
        notifyConnectedGenerators();

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void findAndSetupNewMasters() {
        if (level == null || level.isClientSide) return;

        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity storage && !storage.isMaster) {
                    storage.verifyMasterConnection();
                }
            }
        }
    }

    private void updateSlaves() {
        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition) && isValidPosition(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity slave) {
                    slave.isMaster = false;
                    slave.masterPos = this.worldPosition;
                    slave.mergedBlocks.clear();
                    slave.setChanged();
                    level.sendBlockUpdated(pos, slave.getBlockState(), slave.getBlockState(), 3);
                }
            }
        }
    }

    public void verifyMasterConnection() {
        if (isMaster || level == null || level.isClientSide) return;

        if (!isConnectedToMaster()) {
            becomeMaster();
        }
    }

    private boolean isConnectedToMaster() {
        if (isMaster || !isValidPosition(masterPos)) return false;

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(this.worldPosition);
        visited.add(this.worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                if (neighbor.equals(masterPos)) {
                    return true;
                }

                BlockEntity be = level.getBlockEntity(neighbor);
                if (be instanceof EnergyStorageBlockEntity) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    private void becomeMaster() {
        if (level == null || level.isClientSide) return;

        // Guardamos la energía que tenía antes de dejar de ser esclavo
        float inheritedEnergy = this.energyStorage.getEnergyStored();

        this.isMaster = true;
        this.masterPos = this.worldPosition;

        rediscoverConnectedBlocks();

        int newCapacity = BASE_CAPACITY * mergedBlocks.size();
        this.energyStorage.setMaxEnergyStored(newCapacity);

        // Si el bloque suelto tenía energía propia retenida, la respeta; si no, parte de la heredada
        if (this.energyStorage.getEnergyStored() <= 0 && inheritedEnergy > 0) {
            this.energyStorage.setStoredEnergy(Math.min(inheritedEnergy, newCapacity));
        }

        updateNetwork();
        setChanged();
        notifyConnectedGenerators();
    }

    private void rediscoverConnectedBlocks() {
        mergedBlocks.clear();
        Set<BlockPos> toCheck = new HashSet<>();
        toCheck.add(this.worldPosition);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.iterator().next();
            toCheck.remove(current);

            if (!isValidPosition(current)) continue;

            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof EnergyStorageBlockEntity) {
                mergedBlocks.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (isValidPosition(neighbor) && !mergedBlocks.contains(neighbor)) {
                        toCheck.add(neighbor);
                    }
                }
            }
        }
    }

    public void onBlockRemoved() {
        if (level == null || level.isClientSide) return;

        try {
            if (isMaster) {
                notifyConnectedGeneratorsAboutRemoval();
                promoteNewMaster();
            } else {
                notifyMasterAboutRemoval();
            }
        } catch (Exception e) {
            System.err.println("Error in onBlockRemoved: " + e.getMessage());
        }
    }

    private void notifyConnectedGeneratorsAboutRemoval() {
        if (level == null || level.isClientSide) return;

        Set<BlockPos> generators = findConnectedGenerators();
        for (BlockPos generatorPos : generators) {
            BlockEntity be = level.getBlockEntity(generatorPos);
            if (be instanceof AbstractEnergyGeneratorBlockEntity generator) {
                generator.onStorageNetworkUpdated(0, 0, 0, this.worldPosition);
            }
        }
    }
}