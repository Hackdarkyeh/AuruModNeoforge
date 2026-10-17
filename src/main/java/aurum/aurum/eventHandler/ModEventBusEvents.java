package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.entity.CooperGolem.CooperGolemEntity;
import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import aurum.aurum.entity.SpiderCorrupted.SpiderCorruptedEntity;
import aurum.aurum.entity.ZombieAltered.ZombieAlteredEntity;
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
        event.put(ModEntities.ZOMBIE_ALTERED.get(), ZombieAlteredEntity.createAttributes().build());
        event.put(ModEntities.SPIDER_CORRUPTED.get(), SpiderCorruptedEntity.createAttributes().build());
        event.put(ModEntities.SKELETON_ALTERED.get(), aurum.aurum.entity.SkeletonAltered.SkeletonAltered.createAttributes().build());
        event.put(ModEntities.DOLPHIN_ALTERED.get(), aurum.aurum.entity.DolphinAltered.DolphinAlteredEntity.createAttributes().build());
        event.put(ModEntities.ENDER_DRAGON_ALTERED.get(), aurum.aurum.entity.EnderDragonAltered.EnderDragonAltered.createAttributes().build());
        event.put(ModEntities.VEX_ALTERED.get(), aurum.aurum.entity.VexAltered.VexAlteredEntity.createAttributes().build());
        event.put(ModEntities.DIVINE_ARMOR.get(), aurum.aurum.entity.EnchantedArmor.DivineArmorEntity.createAttributes().build());

    }
}
