package aurum.aurum.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;

import java.util.List;

import static aurum.aurum.Aurum.MODID;

public class ModAurumPlacedFeatures {

    public static final ResourceKey<PlacedFeature> AURUM_VINES_PLACED = registerKey("aurum_vines_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(AURUM_VINES_PLACED,
                new PlacedFeature(
                        configuredFeatures.getOrThrow(ModAurumConfiguredFeatures.AURUM_VINES),
                        List.of(
                                //RarityFilter.onAverageOnceEvery(3),
                                //InSquarePlacement.spread()
                        )
                ));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}