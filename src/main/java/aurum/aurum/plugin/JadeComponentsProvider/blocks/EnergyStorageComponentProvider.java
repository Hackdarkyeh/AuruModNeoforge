package aurum.aurum.plugin.JadeComponentsProvider.blocks;

import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.entity.CooperGolemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum EnergyStorageComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int energyStorage = accessor.getServerData().getInt("EnergyPercentage");
        int maxEnergy = accessor.getServerData().getInt("MaxEnergy");
        tooltip.add(Component.translatable("aurum.aurum.energy_storage_check", energyStorage));
        tooltip.append(Component.translatable(String.valueOf(maxEnergy)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EnergyStorageBlockEntity energyStorageBlockEntity) {
            data.putFloat("EnergyPercentage", energyStorageBlockEntity.getEnergyStored());
            data.putFloat("MaxEnergy", energyStorageBlockEntity.getCapacity());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.tryBuild("aurum", "energy_storage_block");
    }


}

