package aurum.aurum.worldgen.Dimension;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Supplier;

public class AurumDimension {
    public static class Dimension1SpecialEffectsHandler {
        @SubscribeEvent
        public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
            DimensionSpecialEffects customEffect = new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, false) {
                @Override
                public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
                    return color;
                }

                @Override
                public boolean isFoggyAt(int x, int y) {
                    return true;
                }
            };
            event.register(ResourceLocation.parse("aurum:dimension_1"), customEffect);
        }
    }
    @SubscribeEvent
    public static void onRegisterDimensions(RegisterEvent event) {
        event.register(Registries.DIMENSION_TYPE, helper -> {
            helper.register(
                    ResourceKey.create(Registries.DIMENSION_TYPE,
                            ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension_type")),
                    AURUM_DIMENSION_TYPE.get()
            );
        });
        event.register(Registries.WORLD_PRESET, helper -> {
            helper.register(
                    ResourceKey.create(Registries.WORLD_PRESET,
                            ResourceLocation.fromNamespaceAndPath("aurum", "aurum_world_preset")),
                    AURUM_WORLD_PRESET.get()
            );
        });
        event.register(Registries.LEVEL_STEM, helper -> {
            // Obtener RegistryAccess
            RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

            // Crear el ChunkGenerator
            ChunkGenerator chunkGenerator = new AurumChunkGenerator(
                    new FixedBiomeSource(registryAccess.registryOrThrow(Registries.BIOME)
                            .getHolderOrThrow(Biomes.PLAINS)),
                    registryAccess.registryOrThrow(Registries.NOISE_SETTINGS)
                            .getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED)
            );

            // Crear y registrar el LevelStem
            helper.register(
                    ResourceKey.create(Registries.LEVEL_STEM,
                            ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension")),
                    new LevelStem(
                            registryAccess.registryOrThrow(Registries.DIMENSION_TYPE)
                                    .getHolderOrThrow(ResourceKey.create(
                                            Registries.DIMENSION_TYPE,
                                            ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension_type"))),
                            chunkGenerator
                    )
            );
        });


    }

    public static final ResourceKey<Level> AURUM_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("aurum", "dimension_1"));
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = DeferredRegister.create(Registries.DIMENSION_TYPE, "aurum");

    public static final DeferredRegister<WorldPreset> WORLD_PRESETS = DeferredRegister.create(Registries.WORLD_PRESET, "aurum");

    public static final Supplier<DimensionType> AURUM_DIMENSION_TYPE = DIMENSION_TYPES.register("aurum_dimension_type",
            () -> new DimensionType(
                    OptionalLong.empty(), // fixedTime
                    true, // hasSkylight
                    false, // hasCeiling
                    false, // ultraWarm
                    true, // natural
                    1.0, // coordinateScale
                    false, // bedWorks
                    false, // respawnAnchorWorks
                    0, // minY
                    256, // height
                    256, // logicalHeight
                    BlockTags.INFINIBURN_OVERWORLD, // infiniburn (using overworld's for example)
                    ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension_type"), // effectsLocation
                    0.0f, // ambientLight
                    new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0 // monster settings
                    )));



    public static final Supplier<WorldPreset> AURUM_WORLD_PRESET = WORLD_PRESETS.register(
            "aurum_world_preset",
            () -> {
                // 1. Crear el MappedRegistry con el constructor correcto
                MappedRegistry<LevelStem> dimensionsRegistry = new MappedRegistry<>(
                        Registries.LEVEL_STEM,  // ResourceKey del registro
                        Lifecycle.stable(),      // Lifecycle
                        false                   // hasIntrusiveHolders
                );

                // 2. Obtener RegistryAccess
                RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

                ResourceKey<LevelStem> aurumKey = ResourceKey.create(
                        Registries.LEVEL_STEM,
                        ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension")
                );

                // 3. Obtener registros necesarios
                Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
                Registry<NoiseGeneratorSettings> noiseSettingsRegistry = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS);
                Registry<DimensionType> dimensionTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);

                // 4. Crear el chunk generator
                ChunkGenerator chunkGenerator = new AurumChunkGenerator(
                        new FixedBiomeSource(biomeRegistry.getHolderOrThrow(Biomes.PLAINS)),
                        noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED)
                );

                // 5. Obtener Holder del DimensionType
                Holder<DimensionType> dimensionTypeHolder = dimensionTypeRegistry.getHolderOrThrow(
                        ResourceKey.create(Registries.DIMENSION_TYPE,
                                ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension_type"))
                );

                // 6. Crear LevelStem
                LevelStem aurumStem  = new LevelStem(dimensionTypeHolder, chunkGenerator);

                // 7. Registrar la dimensión
                ResourceKey<LevelStem> dimensionKey = ResourceKey.create(
                        Registries.LEVEL_STEM,
                        ResourceLocation.fromNamespaceAndPath("aurum", "aurum_dimension")
                );
                // Versión alternativa directa
                dimensionsRegistry.register(
                        dimensionKey,
                        aurumStem ,
                        RegistrationInfo.BUILT_IN
                );

                // 8. Crear WorldDimensions
                WorldDimensions worldDimensions = new WorldDimensions(dimensionsRegistry);

                // 9. Crear WorldGenSettings
                WorldGenSettings settings = new WorldGenSettings(
                        new WorldOptions(0L, false, false),
                        worldDimensions
                );

                Map<ResourceKey<LevelStem>, LevelStem> dimensions = new HashMap<>();
                dimensions.put(aurumKey, aurumStem);

                // Crear WorldPreset con el mapa de dimensiones
                return new WorldPreset(dimensions);
            }
    );


}

