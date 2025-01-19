package aurum.aurum.plugin.JadeComponentsProvider.entities;

import aurum.aurum.entity.CooperGolemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.config.IPluginConfig;


import net.minecraft.network.chat.Component;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;

public enum CooperGolemComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains("EnergyPercentage")) {
            int energyPercentage = accessor.getServerData().getInt("EnergyPercentage");
            tooltip.add(Component.translatable("aurum.aurum.energy", energyPercentage + "%"));
        }
        if (accessor.getServerData().contains("IsChanneling")) {
            boolean isChanneling = accessor.getServerData().getBoolean("IsChanneling");
            tooltip.add(Component.translatable("aurum.aurum.channeling", isChanneling ? "Yes" : "No"));

        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof CooperGolemEntity golem) {
            data.putInt("EnergyPercentage", golem.getEnergyPercentage());
            data.putBoolean("IsChanneling", golem.isChanneling());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.tryBuild("aurum", "cooper_golem");
    }
}

