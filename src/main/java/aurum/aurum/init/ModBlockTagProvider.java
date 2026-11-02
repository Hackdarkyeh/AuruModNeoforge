package aurum.aurum.init;

import aurum.aurum.Aurum;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Aurum.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Sin estos tags las vallas, puertas de valla y muros NO conectan entre sí.
        // WOODEN_FENCES además las hace conectar con las vallas de roble.
        tag(BlockTags.FENCES)
                .add(ModBlocks.AURUM_FENCE.get());
        //.add(ModBlocks.AURUM_FENCE_RED.get());
        tag(BlockTags.WOODEN_FENCES)
                .add(ModBlocks.AURUM_FENCE.get());
        //.add(ModBlocks.AURUM_FENCE_RED.get());
        tag(BlockTags.FENCE_GATES)
                .add(ModBlocks.AURUM_FENCE_GATE.get());
        //.add(ModBlocks.AURUM_FENCE_GATE_RED.get());
        tag(BlockTags.WALLS)
                .add(ModBlocks.AURUM_WALL.get());
        //.add(ModBlocks.AURUM_WALL_RED.get());

        tag(BlockTags.STAIRS)
                .add(ModBlocks.AURUM_STAIRS.get());
        //.add(ModBlocks.AURUM_STAIRS_RED.get());
        tag(BlockTags.WOODEN_STAIRS)
                .add(ModBlocks.AURUM_STAIRS.get());
        //.add(ModBlocks.AURUM_STAIRS_RED.get());
        tag(BlockTags.SLABS)
                .add(ModBlocks.AURUM_SLAB.get());
        //.add(ModBlocks.AURUM_SLAB_RED.get());
        tag(BlockTags.WOODEN_SLABS)
                .add(ModBlocks.AURUM_SLAB.get());
        //.add(ModBlocks.AURUM_SLAB_RED.get());
        tag(BlockTags.DOORS)
                .add(ModBlocks.AURUM_DOOR.get());
        //.add(ModBlocks.AURUM_DOOR_RED.get());
        tag(BlockTags.WOODEN_DOORS)
                .add(ModBlocks.AURUM_DOOR.get());
        //.add(ModBlocks.AURUM_DOOR_RED.get());
        tag(BlockTags.WOODEN_BUTTONS)
                .add(ModBlocks.AURUM_BUTTON.get());
        //.add(ModBlocks.AURUM_BUTTON_RED.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES)
                .add(ModBlocks.AURUM_PRESSURE_PLATE.get());
        //.add(ModBlocks.AURUM_PRESSURE_PLATE_RED.get());

        // Romper más rápido con hacha
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.AMBAR_PLANKS.get())
                .add(ModBlocks.AURUM_STAIRS.get())
                .add(ModBlocks.AURUM_SLAB.get())
                .add(ModBlocks.AURUM_FENCE.get())
                .add(ModBlocks.AURUM_FENCE_GATE.get())
                .add(ModBlocks.AURUM_DOOR.get())
                .add(ModBlocks.AURUM_BUTTON.get())
                .add(ModBlocks.AURUM_PRESSURE_PLATE.get());
        //.add(ModBlocks.AURUM_PLANKS_RED.get())
        //.add(ModBlocks.AURUM_STAIRS_RED.get())
        //.add(ModBlocks.AURUM_SLAB_RED.get())
        //.add(ModBlocks.AURUM_FENCE_RED.get())
        //.add(ModBlocks.AURUM_FENCE_GATE_RED.get())
        //.add(ModBlocks.AURUM_DOOR_RED.get())
        //.add(ModBlocks.AURUM_BUTTON_RED.get())
        //.add(ModBlocks.AURUM_PRESSURE_PLATE_RED.get());

        // ------------------------------------------------------------
        // TAGS ANTIGUOS (BISMUTH) - desactivados
        // ------------------------------------------------------------
        /*
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.BISMUTH_BLOCK.get())
                .add(ModBlocks.BISMUTH_ORE.get())
                .add(ModBlocks.BISMUTH_LAMP.get())
                .add(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.BISMUTH_LAMP.get());

        tag(BlockTags.FENCES).add(ModBlocks.BISMUTH_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.BISMUTH_FENCE_GATE.get());
        tag(BlockTags.WALLS).add(ModBlocks.BISMUTH_WALL.get());

        tag(ModTags.Blocks.NEEDS_BISMUTH_TOOL)
                .add(ModBlocks.BISMUTH_LAMP.get())
                .addTag(BlockTags.NEEDS_IRON_TOOL);

        tag(ModTags.Blocks.INCORRECT_FOR_BISMUTH_TOOL)
                .addTag(BlockTags.INCORRECT_FOR_IRON_TOOL)
                .remove(ModTags.Blocks.NEEDS_BISMUTH_TOOL);
        */

        // ------------------------------------------------------------
        // TUS OTROS TAGS VAN AQUÍ
        // ------------------------------------------------------------
    }
}