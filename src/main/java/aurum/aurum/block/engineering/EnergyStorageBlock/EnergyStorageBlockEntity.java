package aurum.aurum.block.engineering.EnergyStorageBlock;

import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.energy.IEnergyStorage;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyStorageBlockEntity extends BlockEntity implements IEnergyStorage {
    private final EnergyStorage energyStorage;

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BLOCK.get(), pos, state);
        energyStorage = new EnergyStorage(10000, 100, 100,100);
    }

    @Override
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public int addEnergy(int amount, boolean simulate) {
        return energyStorage.addEnergy(amount, simulate);
    }

    public int getRemainingCapacity() {
        return energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
    }


    @Override
    public int consumeEnergy(int energy, boolean simulate) {
        return energyStorage.consumeEnergy(energy, simulate);
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public CompoundTag saveEnergyToTag(CompoundTag tag) {
        return null;
    }

    @Override
    public void loadEnergyFromTag(CompoundTag tag) {
        // Recupera el valor de la clave "EnergyStored" del tag y lo asigna a energyStored
        this.setStoredEnergy(tag.getInt("StoredEnergy"));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return energyStorage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public void setStoredEnergy(int storedEnergy) {
        energyStorage.setStoredEnergy(storedEnergy);
    }




}

