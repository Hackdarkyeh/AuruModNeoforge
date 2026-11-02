package aurum.aurum.init;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        woodKitRecipes(recipeOutput, ModBlocks.AMBAR_PLANKS,
                ModBlocks.AURUM_STAIRS, ModBlocks.AURUM_SLAB, ModBlocks.AURUM_PRESSURE_PLATE,
                ModBlocks.AURUM_BUTTON, ModBlocks.AURUM_FENCE, ModBlocks.AURUM_FENCE_GATE,
                ModBlocks.AURUM_WALL, ModBlocks.AURUM_DOOR, "ambar_planks");

        /*woodKitRecipes(recipeOutput, ModBlocks.AURUM_PLANKS_RED,
                ModBlocks.AURUM_STAIRS_RED, ModBlocks.AURUM_SLAB_RED, ModBlocks.AURUM_PRESSURE_PLATE_RED,
                ModBlocks.AURUM_BUTTON_RED, ModBlocks.AURUM_FENCE_RED, ModBlocks.AURUM_FENCE_GATE_RED,
                ModBlocks.AURUM_WALL_RED, ModBlocks.AURUM_DOOR_RED, "aurum_planks_red");
           */
        // ------------------------------------------------------------
        // TUS OTRAS RECETAS VAN AQUÍ
        // ------------------------------------------------------------
    }

    /** Recetas de vainilla (tablones -> escaleras, losa, placa, botón, valla, puerta de valla, muro y puerta). */
    private void woodKitRecipes(RecipeOutput out, DeferredBlock<Block> planks,
                                DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                                DeferredBlock<PressurePlateBlock> plate, DeferredBlock<ButtonBlock> button,
                                DeferredBlock<FenceBlock> fence, DeferredBlock<FenceGateBlock> gate,
                                DeferredBlock<WallBlock> wall, DeferredBlock<DoorBlock> door, String group) {
        Ingredient planksIngredient = Ingredient.of(planks.get());
        String unlock = "has_" + group;

        stairBuilder(stairs.get(), planksIngredient)
                .group(group).unlockedBy(unlock, has(planks)).save(out);

        slab(out, RecipeCategory.BUILDING_BLOCKS, slab.get(), planks.get());

        pressurePlate(out, plate.get(), planks.get());

        buttonBuilder(button.get(), planksIngredient)
                .group(group).unlockedBy(unlock, has(planks)).save(out);

        fenceBuilder(fence.get(), planksIngredient)
                .group(group).unlockedBy(unlock, has(planks)).save(out);

        fenceGateBuilder(gate.get(), planksIngredient)
                .group(group).unlockedBy(unlock, has(planks)).save(out);

        wall(out, RecipeCategory.DECORATIONS, wall.get(), planks.get());

        doorBuilder(door.get(), planksIngredient)
                .group(group).unlockedBy(unlock, has(planks)).save(out);
    }
}