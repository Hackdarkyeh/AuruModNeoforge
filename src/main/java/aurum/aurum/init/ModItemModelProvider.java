package aurum.aurum.init;

import aurum.aurum.Aurum;
// OJO: ajusta este import al paquete real de tu clase ModArmorItem
import aurum.aurum.item.ArmorItem.ModArmorItem;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;

public class ModItemModelProvider extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
        trimMaterials.put(TrimMaterials.IRON, 0.2F);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
        trimMaterials.put(TrimMaterials.COPPER, 0.5F);
        trimMaterials.put(TrimMaterials.GOLD, 0.6F);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
    }

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Aurum.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // ------------------------------------------------------------
        // ARMADURAS (con trims)
        // ------------------------------------------------------------
        trimmedArmorItem(ModItems.AURELITE_HELMET);
        trimmedArmorItem(ModItems.AURELITE_CHESTPLATE);
        trimmedArmorItem(ModItems.AURELITE_LEGGINGS);
        trimmedArmorItem(ModItems.AURELITE_BOOTS);
        trimmedArmorItem(ModItems.AURELITE_HELMET_GOLD);
        trimmedArmorItem(ModItems.AURELITE_CHESTPLATE_GOLD);
        trimmedArmorItem(ModItems.AURELITE_LEGGINGS_GOLD);
        trimmedArmorItem(ModItems.AURELITE_BOOTS_GOLD);

        // ------------------------------------------------------------
        // MADERA MORADA (ambar_planks)
        // ------------------------------------------------------------
        buttonItem(ModBlocks.AURUM_BUTTON, ModBlocks.AMBAR_PLANKS);
        fenceItem(ModBlocks.AURUM_FENCE, ModBlocks.AMBAR_PLANKS);
        wallItem(ModBlocks.AURUM_WALL, ModBlocks.AMBAR_PLANKS);
        basicItem(ModBlocks.AURUM_DOOR.asItem());           // usa textures/item/aurum_door.png

        // ------------------------------------------------------------
        // TUS OTROS ÍTEMS (basicItem(ModItems.X.get())...) VAN AQUÍ
        // ------------------------------------------------------------
    }

    // Las texturas de la madera están en block/trees/
    private static ResourceLocation woodTexture(DeferredBlock<? extends Block> baseBlock) {
        return ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "block/trees/" + baseBlock.getId().getPath());
    }

    public void buttonItem(DeferredBlock<? extends Block> block, DeferredBlock<? extends Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture", woodTexture(baseBlock));
    }

    public void fenceItem(DeferredBlock<? extends Block> block, DeferredBlock<? extends Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture", woodTexture(baseBlock));
    }

    public void wallItem(DeferredBlock<? extends Block> block, DeferredBlock<? extends Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall", woodTexture(baseBlock));
    }

    // Shoutout to El_Redstoniano for making this
    private void trimmedArmorItem(DeferredItem<ModArmorItem> itemDeferredItem) {
        final String MOD_ID = Aurum.MODID; // Change this to your mod id

        if(itemDeferredItem.get() instanceof ModArmorItem armorItem) {
            trimMaterials.forEach((trimMaterial, value) -> {
                float trimValue = value;

                String armorType = switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> "";
                };

                String armorItemPath = armorItem.toString();
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterial.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterial.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = ResourceLocation.parse(armorItemPath);
                ResourceLocation trimResLoc = ResourceLocation.parse(trimPath); // minecraft namespace
                ResourceLocation trimNameResLoc = ResourceLocation.parse(currentTrimName);

                // This is used for making the ExistingFileHelper acknowledge that this texture exist, so this will
                // avoid an IllegalArgumentException
                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                // Trimmed armorItem files
                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc.getNamespace() + ":item/" + armorItemResLoc.getPath())
                        .texture("layer1", trimResLoc);

                // Non-trimmed armorItem file (normal variant)
                this.withExistingParent(itemDeferredItem.getId().getPath(),
                                mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc.getNamespace()  + ":item/" + trimNameResLoc.getPath()))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0",
                                ResourceLocation.fromNamespaceAndPath(MOD_ID,
                                        "item/" + itemDeferredItem.getId().getPath()));
            });
        }
    }
}