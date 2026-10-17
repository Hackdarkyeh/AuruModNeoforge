package aurum.aurum.plugin.JadeComponentsProvider.entities;

import aurum.aurum.entity.ICorruptedMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.config.IPluginConfig;


import net.minecraft.network.chat.Component;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;

public enum AurumBloodComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String KEY = "AurumBlood";

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains(KEY)) {
            float aurum = accessor.getServerData().getFloat(KEY);
            // Rango del atributo: 0.0 - 10.0 -> lo mostramos como porcentaje sobre el máximo
            int percentage = Math.round((aurum / 100.0F) * 100);
            tooltip.add(Component.translatable("aurum.aurum.aurum_blood", percentage + "%"));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof ICorruptedMob corrupted) {
            data.putFloat(KEY, (float) corrupted.getAurumValue());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.tryBuild("aurum", "aurum_blood");
    }
}