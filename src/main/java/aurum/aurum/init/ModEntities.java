package aurum.aurum.init;


import aurum.aurum.Aurum;
import aurum.aurum.entity.CooperGolem.CooperGolemEntity;

import aurum.aurum.entity.DolphinAltered.DolphinAlteredEntity;
import aurum.aurum.entity.EnchantedArmor.DivineArmorEntity;
import aurum.aurum.entity.EnderDragonAltered.EnderDragonAltered;
import aurum.aurum.entity.SkeletonAltered.SkeletonAltered;
import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import aurum.aurum.entity.SpiderCorrupted.SpiderCorruptedEntity;
import aurum.aurum.entity.VexAltered.VexAlteredEntity;
import aurum.aurum.entity.ZombieAltered.ZombieAlteredEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

//@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Aurum.MODID);


    public static final Supplier<EntityType<CooperGolemEntity>> COOPER_GOLEM = ENTITY_REGISTER.register("cooper_golem",
            () -> EntityType.Builder.of(CooperGolemEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.45f)
                    .build("cooper_golem"));

    public static final Supplier<EntityType<SoreBossEntity>> SORE_BOSS = ENTITY_REGISTER.register("sore_boss",
            () -> EntityType.Builder.of(SoreBossEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.0f)
                    .build("sore_boss"));

    public static final Supplier<EntityType<ZombieAlteredEntity>> ZOMBIE_ALTERED = ENTITY_REGISTER.register("zombie_altered",
            () -> EntityType.Builder.of(ZombieAlteredEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.0f)
                    .build("zombie_altered"));

    public static final Supplier<EntityType<SpiderCorruptedEntity>> SPIDER_CORRUPTED = ENTITY_REGISTER.register("spider_corrupted",
            () -> EntityType.Builder.of(SpiderCorruptedEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F)
                    .build("spider_corrupted"));

    public static final Supplier<EntityType<SkeletonAltered>> SKELETON_ALTERED = ENTITY_REGISTER.register("skeleton_altered",
            () -> EntityType.Builder.of(SkeletonAltered::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.0f)
                    .build("skeleton_altered"));

    public static final Supplier<EntityType<DolphinAlteredEntity>> DOLPHIN_ALTERED = ENTITY_REGISTER.register("dolphin_altered",
            () -> EntityType.Builder.of(DolphinAlteredEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.0f)
                    .build("dolphin_altered"));

    public static final Supplier<EntityType<EnderDragonAltered>> ENDER_DRAGON_ALTERED = ENTITY_REGISTER.register("ender_dragon_altered",
            () -> EntityType.Builder.of(EnderDragonAltered::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.0f)
                    .build("ender_dragon_altered"));

    public static final Supplier<EntityType<VexAlteredEntity>> VEX_ALTERED = ENTITY_REGISTER.register("vex_altered",
            () -> EntityType.Builder.of(VexAlteredEntity::new, MobCategory.MONSTER)
                    .sized(0.4f, 0.8f)
                    .build("vex_altered"));

    public static final Supplier<EntityType<DivineArmorEntity>> DIVINE_ARMOR = ENTITY_REGISTER.register("divine_armor",
            () -> EntityType.Builder.of(DivineArmorEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .build("divine_armor"));
}

