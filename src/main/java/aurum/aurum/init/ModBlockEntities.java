package aurum.aurum.init;

import aurum.aurum.block.AntiElytraBlock.AntiElytraBlockEntity;
import aurum.aurum.block.ArmorSystem.SoulModificationTable.SoulModificationTableBlockEntity;
import aurum.aurum.block.engineering.ArmorTable.ArmorTableBlockEntity;
import aurum.aurum.block.engineering.DarkEnergyTable.DarkEnergyTableBlockEntity;
import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import aurum.aurum.block.engineering.ExtractorBlock.ExtractorBlockEntity;
import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntity;
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

    public static final Supplier<BlockEntityType<ExtractorBlockEntity>> EXTRACTOR_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTRY.register(
            "extractor_block",
            () -> BlockEntityType.Builder.of(
                    ExtractorBlockEntity::new, // Constructor de la entidad
                    ModBlocks.EXTRACTOR_BLOCK.get() // Bloque asociado
            ).build(null)
    );

    public static final Supplier<BlockEntityType<ArmorTableBlockEntity>> ARMOR_TABLE_BLOCK = BLOCK_ENTITIES_REGISTRY.register(
            "armor_table_block",
            () -> BlockEntityType.Builder.of(
                    ArmorTableBlockEntity::new, // Constructor de la entidad
                    ModBlocks.ARMOR_TABLE_BLOCK.get() // Bloque asociado
            ).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_BLOCK = BLOCK_ENTITIES_REGISTRY.register(
            "energy_storage_block",
            () -> BlockEntityType.Builder.of(
                    EnergyStorageBlockEntity::new,
                    ModBlocks.ENERGY_STORAGE_BLOCK.get()
                    ).build(null));;

    public static final Supplier<BlockEntityType<PedestalBlockEntity>> PEDESTAL_BE =
            BLOCK_ENTITIES_REGISTRY.register("pedestal_be", () -> BlockEntityType.Builder.of(
                    PedestalBlockEntity::new, ModBlocks.PEDESTAL.get()).build(null));

    public static final Supplier<BlockEntityType<SoulModificationTableBlockEntity>> SOUL_MODIFICATION_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTRY.register(
            "soul_modification_table_block",
            () -> BlockEntityType.Builder.of(
                    SoulModificationTableBlockEntity::new, // Constructor de la entidad
                    ModBlocks.SOUL_MODIFICATION_TABLE_BLOCK.get() // Bloque asociado
            ).build(null)
    );

    public static final Supplier<BlockEntityType<AntiElytraBlockEntity>> ANTI_ELYTRA_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTRY.register(
            "anti_elytra_block_entity",
            () -> BlockEntityType.Builder.of(
                    AntiElytraBlockEntity::new, // Constructor de la entidad
                    ModBlocks.ANTI_ELYTRA_BLOCK.get() // Bloque asociado
            ).build(null)
    );


    public static final Supplier<BlockEntityType<DarkEnergyTableBlockEntity>> DARK_ENERGY_TABLE_BLOCK = BLOCK_ENTITIES_REGISTRY.register(
            "dark_energy_table_block",
            () -> BlockEntityType.Builder.of(
                    DarkEnergyTableBlockEntity::new,
                    ModBlocks.DARK_ENERGY_TABLE.get()
            ).build(null));



}