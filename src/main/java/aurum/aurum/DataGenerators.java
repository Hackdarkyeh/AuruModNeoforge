package aurum.aurum;
import aurum.aurum.tagsProvider.MyDamageTagsProvider;
import aurum.aurum.worldgen.features.VolcanFeature;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static aurum.aurum.Aurum.MODID;
import static aurum.aurum.init.ModFeatures.VOLCANO_CONFIGURED_FEATURE;
import static aurum.aurum.init.ModFeatures.VOLCANO_KEY_PLACED_FEATURE;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(),
                new MyDamageTagsProvider(generator.getPackOutput(), lookupProvider, existingFileHelper));
        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, bootstrap -> {
                    bootstrap.register(VOLCANO_CONFIGURED_FEATURE, new ConfiguredFeature<>(
                            new VolcanFeature(), // The feature
                            NoneFeatureConfiguration.INSTANCE // The configuration
                    ));
                })
                .add(Registries.PLACED_FEATURE, bootstrap -> {
                    HolderGetter<ConfiguredFeature<?, ?>> otherRegistry = bootstrap.lookup(Registries.CONFIGURED_FEATURE);
                    bootstrap.register(VOLCANO_KEY_PLACED_FEATURE, new PlacedFeature(
                            otherRegistry.getOrThrow(VOLCANO_CONFIGURED_FEATURE), // Get the configured feature
                            List.of() // No-op when placement happens - replace with whatever your placement parameters are
                    ));
                });

        event.getGenerator().addProvider(  event.includeServer()// Only run datapack generation when server data is being generated
                ,                // Create the provider
                new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, builder, Set.of(MODID)));
    }
}
