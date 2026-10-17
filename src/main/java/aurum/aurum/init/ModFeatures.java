package aurum.aurum.init;

import aurum.aurum.worldgen.Dimension.Features.AurumVinesFeature;
import aurum.aurum.worldgen.Dimension.Features.VolcanoCraterFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;
import static net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID;


    // Crear clave para la característica configurada (volcán)
    /*public static final ResourceKey<ConfiguredFeature<?, ?>> VOLCANO_CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MODID, "volcano"));

    public static final ResourceKey<PlacedFeature> VOLCANO_KEY_PLACED_FEATURE = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcano_placed_feature")
    );*/


public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MODID);

    public static final DeferredHolder<Feature<?>, AurumVinesFeature> AURUM_VINES_FEATURE =
            FEATURES.register("aurum_vines_feature",
                    () -> new AurumVinesFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<VolcanoCraterFeature> VOLCANO_CRATER = FEATURES.register(
            "volcano_crater", () -> new VolcanoCraterFeature(VolcanoCraterFeature.Config.CODEC));
}
