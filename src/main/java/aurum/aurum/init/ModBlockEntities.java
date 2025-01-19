package aurum.aurum.init;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;


public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES_REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);


    public static final Supplier<BlockEntityType<EnergyGeneratorBlockEntity>> ENERGY_GENERATOR_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTRY.register(
            "energy_generator_block",
            () -> BlockEntityType.Builder.of(
                    EnergyGeneratorBlockEntity::new, // Constructor de la entidad
                    ModBlocks.ENERGY_GENERATOR_BLOCK.get() // Bloque asociado
            ).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_BLOCK = BLOCK_ENTITIES_REGISTRY.register(
            "enery_storage_block", () -> BlockEntityType.Builder.of(EnergyStorageBlockEntity::new).build(null));;

}