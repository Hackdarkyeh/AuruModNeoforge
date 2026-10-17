package aurum.aurum.plugin;


import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.entity.CooperGolem.CooperGolemEntity;
import aurum.aurum.plugin.JadeComponentsProvider.blocks.EnergyStorageComponentProvider;
import aurum.aurum.plugin.JadeComponentsProvider.entities.AurumBloodComponentProvider;
import aurum.aurum.plugin.JadeComponentsProvider.entities.CooperGolemComponentProvider;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(CooperGolemComponentProvider.INSTANCE, CooperGolemEntity.class);
        registration.registerBlockDataProvider(EnergyStorageComponentProvider.INSTANCE, EnergyStorageBlockEntity.class);

        registration.registerEntityDataProvider(AurumBloodComponentProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CooperGolemComponentProvider.INSTANCE, CooperGolemEntity.class);
        registration.registerBlockComponent(EnergyStorageComponentProvider.INSTANCE, EnergyStorageBlock.class);

        registration.registerEntityComponent(AurumBloodComponentProvider.INSTANCE, LivingEntity.class);
    }
}
