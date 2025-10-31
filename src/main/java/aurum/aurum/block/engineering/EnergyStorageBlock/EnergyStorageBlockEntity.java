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


    private final Set<BlockPos> subscribedGenerators = new HashSet<>();


    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BLOCK.get(), pos, state);
        this.energyStorage = new EnergyStorage(BASE_CAPACITY, TRANSFER_RATE, TRANSFER_RATE, TRANSFER_RATE);
        this.masterPos = pos; // Initialize as its own master
        this.mergedBlocks.add(pos); // Start with self in network
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyStorageBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        try {
            long currentTick = level.getGameTime();
            if (currentTick - entity.lastUpdateTick >= NETWORK_UPDATE_INTERVAL) {
                entity.checkForAdjacentBlocks();
                entity.lastUpdateTick = currentTick;
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
                // Distribute energy proportionally
                float energyPerBlock = this.energyStorage.getEnergyStored() / mergedBlocks.size();
                float newEnergy = energyPerBlock * (mergedBlocks.size() - 1);

                // Configure new master
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

                // Reduce energy proportionally
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

        // Asegurar que siempre haya al menos 1 bloque (este mismo)
        int newSize = Math.max(1, mergedBlocks.size());

        if (newSize == lastNetworkSize) return;

        // Calcular nueva capacidad
        int totalCapacity = BASE_CAPACITY * newSize;
        float currentEnergy = this.energyStorage.getEnergyStored();

        // Ajustar energía si excede la nueva capacidad
        if (currentEnergy > totalCapacity) {
            currentEnergy = totalCapacity;
        }

        // Actualizar valores
        this.energyStorage.setMaxEnergyStored(totalCapacity);
        this.energyStorage.setStoredEnergy(currentEnergy);
        this.lastNetworkSize = newSize;

        // Actualizar todos los esclavos
        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition) && isValidPosition(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity slave) {
                    slave.isMaster = false;
                    slave.masterPos = this.worldPosition;
                    slave.mergedBlocks.clear(); // Los esclavos no mantienen su propia lista
                    slave.setChanged();

                    // Sincronizar con cliente
                    level.sendBlockUpdated(pos, slave.getBlockState(), slave.getBlockState(), 3);
                }
            }
        }

        // Forzar sincronización del maestro
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        notifyConnectedGenerators();

    }

    // IEnergyStorage implementation
    @Override
    public float getEnergyStored() {
        if (!isMaster && isValidPosition(masterPos)) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof EnergyStorageBlockEntity master) {
                return master.getEnergyStored();
            }
        }
        return energyStorage.getEnergyStored();
    }

    @Override
    public float getCapacity() {
        if (!isMaster && isValidPosition(masterPos)) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof EnergyStorageBlockEntity master) {
                return master.getCapacity();
            }
        }
        return energyStorage.getCapacity();
    }

    @Override
    public float addEnergy(float amount, boolean simulate) {
        if (!isMaster) {
            if (isValidPosition(masterPos)) {
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
            if (isValidPosition(masterPos)) {
                BlockEntity be = level.getBlockEntity(masterPos);
                if (be instanceof EnergyStorageBlockEntity master) {
                    return master.consumeEnergy(amount, simulate);
                }
            }
            return 0;
        }
        float result = energyStorage.consumeEnergy(amount, simulate);
        if (result > 0 && !simulate) setChanged();
        return result;
    }

    @Override
    public void setStoredEnergy(float amount) {
        if (!isMaster) {
            if (isValidPosition(masterPos)) {
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

        energyStorage.saveEnergyToTag(tag);    }

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
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // IEnergyStorage remaining methods
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
    public void loadEnergyFromTag(CompoundTag tag) {

    }

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

        // Buscar generadores conectados por tuberías
        Set<BlockPos> connectedGenerators = findConnectedGenerators();
        for (BlockPos generatorPos : connectedGenerators) {
            BlockEntity be = level.getBlockEntity(generatorPos);
            if (be instanceof AbstractEnergyGeneratorBlockEntity generator) {
                // ✅ Notificar con los datos ACTUALES del maestro
                generator.onStorageNetworkUpdated(
                        this.getEnergyStored(),
                        this.getCapacity(),
                        this.mergedBlocks.size(),
                        this.worldPosition
                );
            }
        }

        // También notificar a generadores suscritos
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
                    // Es una tubería, seguir explorando
                    queue.add(neighbor);
                } else {
                    // Verificar si es un generador
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
            // ✅ ENVIAR DATOS ACTUALIZADOS INMEDIATAMENTE
            generator.onStorageNetworkUpdated(this.getEnergyStored(), this.getCapacity(), this.mergedBlocks.size(), this.worldPosition);
        }
    }












    private void checkForAdjacentBlocks() {
        if (!isMaster) return;

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

        // ✅ DETECTAR SI LA RED SE DIVIDIÓ (pérdida de bloques)
        boolean networkSplit = !newMergedBlocks.containsAll(mergedBlocks) ||
                newMergedBlocks.size() < mergedBlocks.size();

        if (!newMergedBlocks.equals(mergedBlocks)) {
            // ✅ GUARDAR ENERGÍA ANTES DEL CAMBIO
            float oldEnergy = this.getEnergyStored();
            float oldCapacity = this.getCapacity();

            mergedBlocks = newMergedBlocks;

            // ✅ SI LA RED SE DIVIDIÓ, REDISTRIBUIR ENERGÍA
            if (networkSplit && oldEnergy > 0) {
                redistributeEnergyAfterSplit(oldEnergy, oldCapacity);
            } else {
                updateNetwork();
            }

            setChanged();
        }
    }

    // ✅ NUEVO MÉTODO: Redistribuir energía cuando la red se divide
    private void redistributeEnergyAfterSplit(float oldEnergy, float oldCapacity) {
        if (level == null || level.isClientSide) return;

        // Calcular nueva capacidad
        int newSize = Math.max(1, mergedBlocks.size());
        int newCapacity = BASE_CAPACITY * newSize;

        // ✅ DISTRIBUIR ENERGÍA PROPORCIONALMENTE
        // Si teníamos 10,000 de capacidad con 5,000 de energía (50%)
        // Y ahora tenemos 5,000 de capacidad, debemos tener 2,500 de energía (50%)
        float energyRatio = oldEnergy / oldCapacity;
        float newEnergy = newCapacity * energyRatio;


        this.energyStorage.setMaxEnergyStored(newCapacity);
        this.energyStorage.setStoredEnergy(newEnergy);
        this.lastNetworkSize = newSize;

        // ✅ BUSCAR Y CONFIGURAR NUEVOS MAESTROS PARA LOS FRAGMENTOS DESCONECTADOS
        findAndSetupNewMasters();

        // Actualizar esclavos
        updateSlaves();

        // ✅ NOTIFICAR INMEDIATAMENTE A LOS GENERADORES
        notifyConnectedGenerators();

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ✅ NUEVO MÉTODO: Encontrar y configurar nuevos maestros para fragmentos desconectados
    private void findAndSetupNewMasters() {
        if (level == null || level.isClientSide) return;

        // Encontrar todos los bloques que ya no están en nuestra red pero estaban antes
        Set<BlockPos> lostBlocks = new HashSet<>(this.mergedBlocks);
        lostBlocks.removeAll(mergedBlocks); // Esto no funcionará bien, necesitamos un enfoque diferente

        // Mejor enfoque: cada bloque que no es maestro debería verificar su conexión
        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity storage && !storage.isMaster) {
                    // Forzar a que verifique si sigue conectado a su maestro
                    storage.verifyMasterConnection();
                }
            }
        }
    }



    // ✅ NUEVO MÉTODO: Actualizar todos los esclavos
    private void updateSlaves() {
        for (BlockPos pos : mergedBlocks) {
            if (!pos.equals(this.worldPosition) && isValidPosition(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyStorageBlockEntity slave) {
                    slave.isMaster = false;
                    slave.masterPos = this.worldPosition;
                    slave.mergedBlocks.clear(); // Los esclavos no mantienen su propia lista
                    slave.setChanged();

                    // Sincronizar con cliente
                    level.sendBlockUpdated(pos, slave.getBlockState(), slave.getBlockState(), 3);
                }
            }
        }
    }

    // ✅ NUEVO MÉTODO: Verificar conexión con el maestro
    public void verifyMasterConnection() {
        if (isMaster || level == null || level.isClientSide) return;

        // Verificar si todavía está conectado al maestro
        if (!isConnectedToMaster()) {
            becomeMaster(); // Convertirse en maestro de un nuevo grupo
        }
    }

    // ✅ NUEVO MÉTODO: Verificar conexión física con el maestro
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

                // Si encontramos el maestro, estamos conectados
                if (neighbor.equals(masterPos)) {
                    return true;
                }

                // Si es un storage o tubería, seguir buscando
                BlockEntity be = level.getBlockEntity(neighbor);
                if (be instanceof EnergyStorageBlockEntity ||
                        level.getBlockState(neighbor).getBlock() instanceof PipeBlock) {
                    queue.add(neighbor);
                }
            }
        }

        return false;
    }

    // ✅ NUEVO MÉTODO: Convertirse en maestro de un nuevo grupo
    private void becomeMaster() {
        if (level == null || level.isClientSide) return;


        this.isMaster = true;
        this.masterPos = this.worldPosition;

        // Encontrar todos los bloques conectados a este nuevo maestro
        rediscoverConnectedBlocks();

        // Configurar capacidad inicial (puede ajustarse según necesidad)
        this.energyStorage.setMaxEnergyStored(BASE_CAPACITY);
        this.energyStorage.setStoredEnergy(0); // O calcular proporción si es necesario

        updateNetwork();
        setChanged();

        // Notificar a generadores del nuevo maestro
        notifyConnectedGenerators();
    }

    // ✅ NUEVO MÉTODO: Redescubrir bloques conectados
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

    // ✅ MODIFICAR onBlockRemoved para mejor manejo
    public void onBlockRemoved() {
        if (level == null || level.isClientSide) return;

        try {
            if (isMaster) {
                // ✅ NOTIFICAR PRIMERO a generadores antes de promover
                notifyConnectedGeneratorsAboutRemoval();
                promoteNewMaster();
            } else {
                notifyMasterAboutRemoval();
            }
        } catch (Exception e) {
            System.err.println("Error in onBlockRemoved: " + e.getMessage());
        }
    }

    // ✅ NUEVO MÉTODO: Notificar a generadores sobre remoción
    private void notifyConnectedGeneratorsAboutRemoval() {
        if (level == null || level.isClientSide) return;

        Set<BlockPos> generators = findConnectedGenerators();
        for (BlockPos generatorPos : generators) {
            BlockEntity be = level.getBlockEntity(generatorPos);
            if (be instanceof AbstractEnergyGeneratorBlockEntity generator) {
                // Notificar que este maestro será removido
                generator.onStorageNetworkUpdated(0, 0, 0, this.worldPosition);
            }
        }
    }

}