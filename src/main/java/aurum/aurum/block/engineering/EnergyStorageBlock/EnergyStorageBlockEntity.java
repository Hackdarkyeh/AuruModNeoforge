package aurum.aurum.block.engineering.EnergyStorageBlock;

import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.energy.IEnergyStorage;
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
import java.util.HashSet;
import java.util.Set;

public class EnergyStorageBlockEntity extends BlockEntity implements IEnergyStorage {
    private static final int BASE_CAPACITY = 10000;
    private static final int TRANSFER_RATE = 100;
    private static final int MAX_NETWORK_SIZE = 16;
    private static final int NETWORK_UPDATE_INTERVAL = 20; // ticks

    final EnergyStorage energyStorage;
    Set<BlockPos> mergedBlocks = new HashSet<>();
    boolean isMaster = true;
    BlockPos masterPos;
    private int lastNetworkSize = 1;
    private long lastUpdateTick = 0;

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

        if (!newMergedBlocks.equals(mergedBlocks)) {
            mergedBlocks = newMergedBlocks;
            updateNetwork();
            setChanged();
        }
    }

    private boolean isValidPosition(@Nullable BlockPos pos) {
        return pos != null && level.isLoaded(pos);
    }

    public void onBlockRemoved() {
        if (level == null || level.isClientSide) return;

        try {
            if (isMaster) {
                promoteNewMaster();
            } else {
                notifyMasterAboutRemoval();
            }
        } catch (Exception e) {
            System.err.println("Error in onBlockRemoved: " + e.getMessage());
        }
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
    public float getMaxEnergyStored() {
        if (!isMaster && isValidPosition(masterPos)) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof EnergyStorageBlockEntity master) {
                return master.getMaxEnergyStored();
            }
        }
        return energyStorage.getMaxEnergyStored();
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
        return getMaxEnergyStored() - getEnergyStored();
    }
}