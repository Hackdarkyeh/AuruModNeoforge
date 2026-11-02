package aurum.aurum.init;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // ------------------------------------------------------------
        // MADERA MORADA
        // ------------------------------------------------------------
        dropSelf(ModBlocks.AMBAR_PLANKS.get());
        dropSelf(ModBlocks.AURUM_STAIRS.get());
        add(ModBlocks.AURUM_SLAB.get(), block -> createSlabItemTable(block));   // la losa doble suelta 2
        dropSelf(ModBlocks.AURUM_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.AURUM_BUTTON.get());
        dropSelf(ModBlocks.AURUM_FENCE.get());
        dropSelf(ModBlocks.AURUM_FENCE_GATE.get());
        dropSelf(ModBlocks.AURUM_WALL.get());
        add(ModBlocks.AURUM_DOOR.get(), block -> createDoorTable(block));       // la puerta ocupa 2 bloques
        // Como la hierba vanilla: suelta tierra, o ella misma con Silk Touch
        add(ModBlocks.WITHERED_GRASS_BLOCK.get(),
                block -> createSingleItemTableWithSilkTouch(block, ModBlocks.WITHERED_DIRT_BLOCK.get()));
        dropSelf(ModBlocks.WITHERED_DIRT_BLOCK.get());

        add(ModBlocks.FROZEN_WITHERED_GRASS_BLOCK.get(),
                block -> createSingleItemTableWithSilkTouch(block, ModBlocks.FROZEN_WITHERED_DIRT_BLOCK.get()));
        dropSelf(ModBlocks.FROZEN_WITHERED_DIRT_BLOCK.get());
        add(ModBlocks.DRY_WITHERED_GRASS_BLOCK.get(),
                block -> createSingleItemTableWithSilkTouch(block, ModBlocks.DRY_WITHERED_DIRT_BLOCK.get()));
        dropSelf(ModBlocks.DRY_WITHERED_DIRT_BLOCK.get());
        add(ModBlocks.ANCIENT_AURUM_GRASS_BLOCK.get(),
                block -> createSingleItemTableWithSilkTouch(block, ModBlocks.ANCIENT_AURUM_DIRT_BLOCK.get()));
        dropSelf(ModBlocks.ANCIENT_AURUM_DIRT_BLOCK.get());

// Piedras y madera
        dropSelf(ModBlocks.VETALITA.get());
        dropSelf(ModBlocks.CERULEO.get());
        dropSelf(ModBlocks.TOBA.get());
        dropSelf(ModBlocks.AMBAR_LOG.get());
        dropSelf(ModBlocks.INOCULATED_OBSIDIAN.get());
        add(ModBlocks.AMBAR_LEAVES.get(), block -> createShearsOnlyDrop(block)); // o dropSelf si quieres

// Máquinas / ingeniería
        dropSelf(ModBlocks.PIPE_BLOCK.get());
        dropSelf(ModBlocks.PIPE_DARK_BLOCK.get());
        dropSelf(ModBlocks.PANEL_BLOCK.get());
        dropSelf(ModBlocks.EXTRACTOR_BLOCK.get());
        dropSelf(ModBlocks.ENERGY_STORAGE_BLOCK.get());
        dropSelf(ModBlocks.ENERGY_GENERATOR_BLOCK.get());
        dropSelf(ModBlocks.ARMOR_TABLE_BLOCK.get());
        dropSelf(ModBlocks.PEDESTAL.get());
        dropSelf(ModBlocks.SOUL_MODIFICATION_TABLE_BLOCK.get());
        dropSelf(ModBlocks.ANTI_ELYTRA_BLOCK.get());
        dropSelf(ModBlocks.DARK_ENERGY_TABLE.get());

// Sin drop
        add(ModBlocks.AURELITE_ORE.get(), noDrop());
        //add(ModBlocks.SOUL_FLUID_BLOCK.get(), noDrop());
        //add(ModBlocks.PLAGUE_AURUM_BLOCK.get(), noDrop());

// Lianas
        add(ModBlocks.AURUM_VINES.get(), block -> createShearsOnlyDrop(block));
        add(ModBlocks.AURUM_VINES_PLANT.get(), block -> createShearsOnlyDrop(block));

        // ------------------------------------------------------------
        // MADERA ROJA
        // ------------------------------------------------------------
        //dropSelf(ModBlocks.AURUM_PLANKS_RED.get());
        //dropSelf(ModBlocks.AURUM_STAIRS_RED.get());
        //add(ModBlocks.AURUM_SLAB_RED.get(), block -> createSlabItemTable(block));
        //dropSelf(ModBlocks.AURUM_PRESSURE_PLATE_RED.get());
        //dropSelf(ModBlocks.AURUM_BUTTON_RED.get());
        //dropSelf(ModBlocks.AURUM_FENCE_RED.get());
        //dropSelf(ModBlocks.AURUM_FENCE_GATE_RED.get());
        //dropSelf(ModBlocks.AURUM_WALL_RED.get());
        //add(ModBlocks.AURUM_DOOR_RED.get(), block -> createDoorTable(block));

        // ------------------------------------------------------------
        // TUS OTROS BLOQUES: OJO, getKnownBlocks() incluye TODOS los bloques del mod,
        // así que cada bloque registrado necesita su loot table aquí o datagen falla.
        // ------------------------------------------------------------
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCK_REGISTRY.getEntries().stream()
                .<Block>map(DeferredHolder::get)
                .toList();
    }
}