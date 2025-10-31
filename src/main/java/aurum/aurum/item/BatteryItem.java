package aurum.aurum.item;

import aurum.aurum.energy.engineering.EnergyStorage;
import aurum.aurum.energy.engineering.IEnergyStorage;
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


        public static float getEnergy(EnergyStorage energyStorage) {
            return energyStorage.getEnergyStored();
        }

        @Override
        public float getRemainingCapacity() {
            return energyStorage.getRemainingCapacity();
        }

        @Override
        public float getEnergyStored() {
            return getEnergy(energyStorage);
        }

        @Override
        public float getCapacity() {
            return MAX_ENERGY;
        }

        @Override
        public float addEnergy(float energy, boolean simulate) {
            return 0;
        }

        @Override
        public float consumeEnergy(float energy, boolean simulate) {
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
        public float receiveEnergy(float maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public void setStoredEnergy(float energyStored) {

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

