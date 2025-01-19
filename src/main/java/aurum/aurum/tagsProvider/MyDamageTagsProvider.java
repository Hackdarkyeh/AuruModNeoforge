package aurum.aurum.tagsProvider;

import aurum.aurum.init.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static aurum.aurum.Aurum.MODID;

public class MyDamageTagsProvider extends TagsProvider<DamageType> {
    public MyDamageTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, Registries.DAMAGE_TYPE, pLookupProvider, MODID, existingFileHelper);

    }
    // Get parameters from GatherDataEvent.


    // Add your tag entries here.
    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        // Create a tag builder for our tag. This could also be e.g. a vanilla or NeoForge tag.
        this.tag(DamageTypeTags.WITCH_RESISTANT_TO).add(ModDamageTypes.AURUM_BLIGHT_DAMAGE);
        this.tag(DamageTypeTags.BYPASSES_ARMOR)
                // Add entries. This is a vararg parameter.
                // Non-intrinsic providers must provide ResourceKeys here instead of the actual objects.
                .add(
                        ModDamageTypes.AURUM_BLIGHT_DAMAGE
             )

                // Set the replace property to true.
                //.replace()
                // Set the replace property back to false.
                .replace(false);
        this.tag(DamageTypeTags.NO_KNOCKBACK)
                .add(
                        ModDamageTypes.AURUM_BLIGHT_DAMAGE
                );
                // Remove entries. This is a vararg parameter. Accepts either resource locations, resource keys,
                // tag keys, or (intrinsic providers only) direct values.
                // Can cause unchecked warnings that can safely be suppressed.
                //.remove(ResourceLocation.fromNamespaceAndPath("minecraft", "crimson_slab"), ResourceLocation.fromNamespaceAndPath("minecraft", "warped_slab"));
    }
}
