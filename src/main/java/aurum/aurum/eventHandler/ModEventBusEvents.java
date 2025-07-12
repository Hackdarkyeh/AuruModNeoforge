package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.entity.CooperGolemEntity;
import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import aurum.aurum.init.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;


@EventBusSubscriber(modid= Aurum.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COOPER_GOLEM.get(), CooperGolemEntity.createAttributes().build());
        event.put(ModEntities.SORE_BOSS.get(), SoreBossEntity.createAttributes().build());
    }
}
