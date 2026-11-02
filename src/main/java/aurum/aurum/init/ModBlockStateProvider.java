package aurum.aurum.init;

import aurum.aurum.Aurum;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Aurum.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // ------------------------------------------------------------
        // MADERA MORADA (ambar_planks)
        // ------------------------------------------------------------
        woodKit(ModBlocks.AMBAR_PLANKS,
                ModBlocks.AURUM_STAIRS, ModBlocks.AURUM_SLAB, ModBlocks.AURUM_PRESSURE_PLATE,
                ModBlocks.AURUM_BUTTON, ModBlocks.AURUM_FENCE, ModBlocks.AURUM_FENCE_GATE,
                ModBlocks.AURUM_WALL, ModBlocks.AURUM_DOOR, "");

        // ------------------------------------------------------------
        // TRONCO Y HOJAS
        // Descomenta SOLO si no tienes ya sus JSON hechos a mano en src/main/resources
        // ------------------------------------------------------------
        // simpleBlockWithItem(ModBlocks.AMBAR_LOG.get(),
        //         models().cubeColumn("ambar_log",
        //                 modLoc("block/trees/ambar_log"),
        //                 modLoc("block/trees/ambar_log_top")));
        // simpleBlockWithItem(ModBlocks.AMBAR_LEAVES.get(),
        //         models().cubeAll("ambar_leaves", modLoc("block/trees/ambar_leaves"))
        //                 .renderType("cutout"));

        // ------------------------------------------------------------
        // TUS OTROS BLOQUES VAN AQUÍ
        // ------------------------------------------------------------
    }

    private void woodKit(DeferredBlock<Block> planks,
                         DeferredBlock<StairBlock> stairs,
                         DeferredBlock<SlabBlock> slab,
                         DeferredBlock<PressurePlateBlock> plate,
                         DeferredBlock<ButtonBlock> button,
                         DeferredBlock<FenceBlock> fence,
                         DeferredBlock<FenceGateBlock> gate,
                         DeferredBlock<WallBlock> wall,
                         DeferredBlock<DoorBlock> door,
                         String doorTextureSuffix) {

        // La ruta de la textura (en trees/)
        ResourceLocation planksTexture = modLoc("block/trees/" + planks.getId().getPath());
        // La ruta del modelo base (generado en block/)
        ResourceLocation planksModel = modLoc("block/" + planks.getId().getPath());

        System.out.println("Generating block states and models for " + planksTexture + " and related blocks.");

        // Bloque completo (genera el modelo block/ambar_planks que usa la losa)
        blockWithItem(planks, planksTexture);

        stairsBlock(stairs.get(), planksTexture);
        blockItem(stairs);

        // 2º parámetro: MODELO de la losa doble (bloque completo); 3º: TEXTURA
        slabBlock(slab.get(), planksModel, planksTexture);
        blockItem(slab);

        pressurePlateBlock(plate.get(), planksTexture);
        blockItem(plate);

        buttonBlock(button.get(), planksTexture);
        simpleBlockItem(button.get(),
                models().buttonInventory(button.getId().getPath() + "_inventory", planksTexture));

        fenceBlock(fence.get(), planksTexture);
        simpleBlockItem(fence.get(),
                models().fenceInventory(fence.getId().getPath() + "_inventory", planksTexture));

        fenceGateBlock(gate.get(), planksTexture);
        blockItem(gate);

        wallBlock(wall.get(), planksTexture);
        simpleBlockItem(wall.get(),
                models().wallInventory(wall.getId().getPath() + "_inventory", planksTexture));

        doorBlockWithRenderType(door.get(),
                modLoc("block/trees/ambar_door_bottom" + doorTextureSuffix),
                modLoc("block/trees/ambar_door_top" + doorTextureSuffix),
                "cutout");
        // El ítem de la puerta (item/aurum_door) lo genera ModItemModelProvider
    }

    // Genera el cubo usando tu textura de la subcarpeta 'trees'
    private void blockWithItem(DeferredBlock<?> deferredBlock, ResourceLocation texture) {
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(deferredBlock.getId().getPath(), texture));
    }

    // Para otros bloques normales con textura en block/
    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> block) {
        simpleBlockItem(block.get(), new ModelFile.UncheckedModelFile(Aurum.MODID + ":block/" + block.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> block, String appendix) {
        simpleBlockItem(block.get(), new ModelFile.UncheckedModelFile(Aurum.MODID + ":block/" + block.getId().getPath() + appendix));
    }
}