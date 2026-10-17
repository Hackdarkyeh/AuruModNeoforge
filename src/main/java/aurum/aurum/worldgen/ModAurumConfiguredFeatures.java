package aurum.aurum.worldgen;

import aurum.aurum.init.ModFeatures;
import aurum.aurum.worldgen.Dimension.Features.VolcanoCraterFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import static aurum.aurum.Aurum.MODID;

public class ModAurumConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> AURUM_VINES = registerKey("aurum_vines");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VOLCANO_CRATER = registerKey("volcano_crater");
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(AURUM_VINES,
                new ConfiguredFeature<>(ModFeatures.AURUM_VINES_FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(VOLCANO_CRATER, new ConfiguredFeature<>(ModFeatures.VOLCANO_CRATER.get(),
                new VolcanoCraterFeature.Config(250)));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}