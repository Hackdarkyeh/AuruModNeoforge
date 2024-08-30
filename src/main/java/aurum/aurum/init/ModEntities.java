package aurum.aurum.init;


/*
 *    MCreator note: This file will be REGENERATED on each build.
 */

import aurum.aurum.Aurum;
import aurum.aurum.entity.CooperGolemEntity;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

//@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Aurum.MODID);


    public static final Supplier<EntityType<CooperGolemEntity>> COOPER_GOLEM = ENTITY_REGISTER.register("cooper_golem",
            () -> EntityType.Builder.of(CooperGolemEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.45f)
                    .build("cooper_golem"));


}

