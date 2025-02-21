package aurum.aurum.init;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static aurum.aurum.Aurum.MODID;

public class ModBiomes {

    // Crear un DeferredRegister para los biomas
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(Registries.BIOME, MODID);

    // Registro del bioma
    public static final DeferredHolder<Biome,Biome> MY_CUSTOM_BIOME = BIOMES.register("aurum_biome1",
            ModBiomes::createCustomBiome);
    public static final DeferredHolder<Biome,Biome> MY_CUSTOM_BIOME1 = BIOMES.register("aurum_biome2",
            ModBiomes::createCustomBiome);


    // Método para crear el bioma
    private static Biome createCustomBiome() {
        // Configuración de efectos del bioma
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(0xC0D8FF)  // Color de la niebla
                .waterColor(0x3F76E4)  // Color del agua
                .skyColor(0x77ADFF)    // Color del cielo
                .build();

        // Obteniendo los HolderGetters para características y carveres desde el acceso al registro
        return new Biome.BiomeBuilder()
                .temperature(0.8F)  // Temperatura del bioma
                .downfall(0.4F)     // Nivel de precipitación
                .specialEffects(effects)  // Efectos visuales
                .generationSettings(createGenerationSettings())  // Configuración de la generación
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())  // Configuración de mobs
                .build();
    }

    private static BiomeGenerationSettings createGenerationSettings() {
        // Este método espera que estés en el contexto de la generación del mundo para obtener acceso a los registros
        // que usas para las características de tu bioma (PlacedFeature y ConfiguredWorldCarver).
        // Aquí necesitarás pasar un acceso al registro o contexto desde donde puedas obtener estos valores.
        HolderGetter<PlacedFeature> placedFeatureHolder = getPlacedFeatureHolder();
        HolderGetter<ConfiguredWorldCarver<?>> worldCarverHolder = getWorldCarverHolder();

        // Construimos la configuración de generación
        return new BiomeGenerationSettings.Builder(placedFeatureHolder, worldCarverHolder)
                // Añadir características aquí, como decoración vegetal, etc.
                .build();
    }

    // Métodos para obtener los HolderGetters
    private static HolderGetter<PlacedFeature> getPlacedFeatureHolder() {
        // En el contexto del servidor o cliente, obtén el acceso al registro para las características colocadas
        return RegistryAccess.EMPTY.lookupOrThrow(Registries.PLACED_FEATURE);
    }

    private static HolderGetter<ConfiguredWorldCarver<?>> getWorldCarverHolder() {
        // De igual manera, obtiene los carveres configurados para la generación del mundo
        return RegistryAccess.EMPTY.lookupOrThrow(Registries.CONFIGURED_CARVER);
    }
}

