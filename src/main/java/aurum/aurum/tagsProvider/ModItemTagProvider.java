package aurum.aurum.tagsProvider;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Aurum.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        /*
        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.BISMUTH.get())
                .add(ModItems.RAW_BISMUTH.get())
                .add(Items.COAL)
                .add(Items.STICK)
                .add(Items.COMPASS);

        tag(ItemTags.SWORDS)
                .add(ModItems.BISMUTH_SWORD.get());
        tag(ItemTags.PICKAXES)
                .add(ModItems.BISMUTH_PICKAXE.get());
        tag(ItemTags.SHOVELS)
                .add(ModItems.BISMUTH_SHOVEL.get());
        tag(ItemTags.AXES)
                .add(ModItems.BISMUTH_AXE.get());
        tag(ItemTags.HOES)
                .add(ModItems.BISMUTH_HOE.get());
        */
        System.out.println("Adding item tags");
        tag(ModTags.Items.ABILITY_ITEMS).
                add(ModItems.EXPANSION_SUPER_SPEED.get())
                .add(ModItems.EXPANSION_DAMAGE_RESISTANCE.get())
                .add(ModItems.EXPANSION_HIGH_JUMP.get())
                .add(ModItems.EXPANSION_REGENERATION.get())
                .add(ModItems.EXPANSION_FIRE_IMMUNE.get())
                .add(ModItems.EXPANSION_DASH.get())
                .add(ModItems.EXPANSION_EXPLOSION.get())
                .add(ModItems.EXPANSION_LAVA_IMMUNE.get())
                .add(ModItems.EXPANSION_MAGIC_SHIELD.get());

        this.tag(ModTags.Items.COMPATIBLE_ARMOR)
                .add(ModItems.AURELITE_HELMET.get())
                .add(ModItems.AURELITE_CHESTPLATE.get())
                .add(ModItems.AURELITE_LEGGINGS.get())
                .add(ModItems.AURELITE_BOOTS.get());

        tag(ModTags.Items.SOUL_ITEMS)
                .add(ModItems.EXPANSION_SOUL_TOTEM_1.get())
                .add(ModItems.EXPANSION_SOUL_TOTEM_2.get())
                .add(ModItems.EXPANSION_SOUL_TOTEM_3.get());

        this.tag(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.AURELITE_HELMET.get())
                .add(ModItems.AURELITE_CHESTPLATE.get())
                .add(ModItems.AURELITE_LEGGINGS.get())
                .add(ModItems.AURELITE_BOOTS.get());

    }
}
