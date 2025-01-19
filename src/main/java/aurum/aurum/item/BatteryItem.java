package aurum.aurum.item;

import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.energy.IEnergyStorage;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

public class BatteryItem extends Item {

    private static final int MAX_ENERGY = 1000;

    public BatteryItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static class BatteryEnergyProvider implements ICapabilityProvider, IEnergyStorage {

        private final EnergyStorage energyStorage;
        private final int MAX_TRANSFER = 10;
        private final int MAX_RECEIVE = 1000;
        private int tier= 1;

        public BatteryEnergyProvider() {
            switch (tier){
                case 2:
                    this.energyStorage = new EnergyStorage(MAX_ENERGY*20, 0, MAX_TRANSFER*10,MAX_RECEIVE*10);
                    break;
                case 3:
                    this.energyStorage = new EnergyStorage(MAX_ENERGY*200, 0, MAX_TRANSFER*100,MAX_RECEIVE*100);
                    break;
                default:
                    this.energyStorage = new EnergyStorage(MAX_ENERGY, 0, MAX_TRANSFER,MAX_RECEIVE);
            }
        }

        public void updateBattery(int tier){
            this.tier = tier;
        }


        public static int getEnergy(EnergyStorage energyStorage) {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getRemainingCapacity() {
            return energyStorage.getRemainingCapacity();
        }

        @Override
        public int getEnergyStored() {
            return getEnergy(energyStorage);
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_ENERGY;
        }

        @Override
        public int addEnergy(int energy, boolean simulate) {
            return 0;
        }

        @Override
        public int consumeEnergy(int energy, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public CompoundTag saveEnergyToTag(CompoundTag tag) {
            return null;
        }

        @Override
        public void loadEnergyFromTag(CompoundTag tag) {

        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public void setStoredEnergy(int energyStored) {

        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public @Nullable Object getCapability(Object object, Object context) {
            return null;
        }
    }
}

