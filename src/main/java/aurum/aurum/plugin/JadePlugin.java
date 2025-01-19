package aurum.aurum.plugin;


import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.entity.CooperGolemEntity;
import aurum.aurum.plugin.JadeComponentsProvider.blocks.EnergyStorageComponentProvider;
import aurum.aurum.plugin.JadeComponentsProvider.entities.CooperGolemComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        //TODO register data providers
        registration.registerEntityDataProvider(CooperGolemComponentProvider.INSTANCE, CooperGolemEntity.class);
        registration.registerBlockDataProvider(EnergyStorageComponentProvider.INSTANCE, EnergyStorageBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        //TODO register component providers, icon providers, callbacks, and config options here
        registration.registerEntityComponent(CooperGolemComponentProvider.INSTANCE, CooperGolemEntity.class);
        registration.registerBlockComponent(EnergyStorageComponentProvider.INSTANCE, EnergyStorageBlock.class);
    }
}
