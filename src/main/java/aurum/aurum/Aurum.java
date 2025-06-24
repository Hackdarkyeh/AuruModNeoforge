package aurum.aurum;

import aurum.aurum.Commands.AurumBlightCommands;
import aurum.aurum.block.SoulModificationTable.SoulModificationTableBlockEntityRenderer;
import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntityRenderer;
import aurum.aurum.client.renderer.CooperGolemRenderer;
import aurum.aurum.eventHandler.ArmorExpEventHandler;
import aurum.aurum.eventHandler.AurumBlightRain;
import aurum.aurum.init.*;
import aurum.aurum.init.GUI.ModMenuType;
import aurum.aurum.init.GUI.ModScreens;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static aurum.aurum.init.ModBlockEntities.BLOCK_ENTITIES_REGISTRY;
import static aurum.aurum.init.ModBlocks.*;
import static aurum.aurum.init.ModEffects.EFFECTS_REGISTRY;
import static aurum.aurum.init.ModItems.*;

// El valor aquí debe coincidir con una entrada en el archivo META-INF/mods.toml
@Mod(Aurum.MODID)
public class Aurum {

    // Define el id del mod en un lugar común para que todo lo referencie
    public static final String MODID = "aurum";
    // Referencia directa a un logger slf4j
    private static final Logger LOGGER = LogUtils.getLogger();

    public Aurum() {
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();

        // Registra el método commonSetup para la carga del mod
        assert modEventBus != null;
        modEventBus.addListener(this::commonSetup);

        // Registra el Registro Diferido al bus de eventos del mod para que los ModBlocks se registren
        BLOCK_REGISTRY.register(modEventBus);
        BLOCK_ENTITIES_REGISTRY.register(modEventBus);
        //BLOCKS_REGISTRY_DEFERRED_REGISTER.register(modEventBus);
        // Registra el Registro Diferido al bus de eventos del mod para que los ModItems se registren
        ITEMS_REGISTRY.register(modEventBus);

        // Registra el Registro Diferido al bus de eventos del mod para que los ModEffects se registren
        EFFECTS_REGISTRY.register(modEventBus);


        // Registra el Registro Diferido al bus de eventos del mod para que las pestañas se registren
        CREATIVE_MODE_TABS.register(modEventBus);




        // Nos registramos para los eventos del servidor y otros eventos del juego que nos interesan
        NeoForge.EVENT_BUS.register(this);

        // Registra el item a una pestaña creativa
        modEventBus.addListener(this::addCreative);

        ModParticles.REGISTRY.register(modEventBus);

        NeoForge.EVENT_BUS.register(AurumBlightCommands.class);
        NeoForge.EVENT_BUS.register(AurumBlightRain.class);

        ModEntities.ENTITY_REGISTER.register(modEventBus);

        ModBiomes.BIOMES.register(modEventBus);

        ModFluids.REGISTRY_FLUIDS.register(modEventBus);
        ModFluidTypes.REGISTRY_FLUID_TYPE.register(modEventBus);
        ModStructures.register(modEventBus);
        ModMenuType.MENU_TYPE_REGISTRY.register(modEventBus);

        ModComponents.register(modEventBus); // <-- Añade esta línea

        NeoForge.EVENT_BUS.register(ArmorExpEventHandler.class);



        // Registrar características configuradas

        // Registrar características colocadas

        // Registra la ForgeConfigSpec de nuestro mod para que Forge pueda crear y cargar el archivo de configuración por nosotros
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }



    private void commonSetup(final FMLCommonSetupEvent event) {
        // Algun código de configuración común
        LOGGER.info("HOLA DESDE LA CONFIGURACIÓN COMÚN");
        LOGGER.info("BLOQUE DE TIERRA >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
    }

    // Añade el item de bloque de ejemplo a la pestaña de ModBlocks de construcción
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        //if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            //event.accept(WITHERED_GRASS_BLOCK.get());
    }
    // Puedes usar SubscribeEvent y dejar que el Event Bus descubra métodos para llamar
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Haz algo cuando el servidor comienza
        LOGGER.info("HOLA desde el inicio del servidor");
    }

    // Puedes usar EventBusSubscriber para registrar automáticamente todos los métodos estáticos en la clase anotada con @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Algun código de configuración del cliente
            LOGGER.info("HOLA DESDE LA CONFIGURACIÓN DEL CLIENTE");
            LOGGER.info("NOMBRE DE MINECRAFT >> {}", Minecraft.getInstance().getUser().getName());
            LOGGER.info("Pack de recursos activos DE MINECRAFT >> {}", Minecraft.getInstance().getDownloadedPackSource());
            EntityRenderers.register(ModEntities.COOPER_GOLEM.get(), CooperGolemRenderer::new);
            ItemBlockRenderTypes.setRenderLayer(ENERGY_STORAGE_BLOCK.get(), RenderType.translucent());
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.SOUL_MODIFICATION_TABLE_BLOCK_ENTITY.get(), SoulModificationTableBlockEntityRenderer::new);
        }

    }

    // Crea una pestaña creativa con el id "examplemod:INTERFAZ_CREATIVO" para el item de ejemplo, que se coloca después de la pestaña de combate
    public static final Supplier<CreativeModeTab> INTERFAZ_CREATIVO = CREATIVE_MODE_TABS.register("interfaz_creativo", () -> CreativeModeTab.builder()
            .title(Component.translatable("item_group." + MODID + ".interfaz_creativo"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> AURUM_HEALING_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Añade el item de ejemplo a la pestaña. Para tus propias pestañas, este método es preferido sobre el evento
                output.accept(WITHERED_GRASS_BLOCK.get()); // Añade el item de bloque de ejemplo a la pestaña
                output.accept(WITHERED_DIRT_BLOCK.get()); // Añade el item de bloque de tierra marchita a la pestaña
                output.accept(ANCIENT_AURUM_GRASS_BLOCK.get()); // Añade el item de hierba de Aurum a la pestaña
                output.accept(ANCIENT_AURUM_DIRT_BLOCK.get()); // Añade el item de tierra de Aurum a la pestaña
                output.accept(DRY_WITHERED_GRASS_BLOCK.get()); // Añade el item de hierba marchita seca a la pestaña
                output.accept(DRY_WITHERED_DIRT_BLOCK.get()); // Añade el item de tierra marchita seca a la pestaña
                output.accept(AURUM_HEALING_ITEM.get()); // Añade el item de curación de Aurum a la pestaña
                output.accept(AURUMROSA_BUCKET.get()); // Añade el cubo de Aurum a la pestaña
                output.accept(AURELITE_ORE.get()); // Añade el bloque de Aurum a la pestaña
                output.accept(PIPE_BLOCK.get()); // Añade el bloque de tubería a la pestaña
                output.accept(PANEL_BLOCK.get()); // Añade el bloque de panel a la pestaña
                output.accept(EXTRACTOR_BLOCK.get()); // Añade el bloque de extractor a la pestaña
                output.accept(ENERGY_STORAGE_BLOCK.get()); // Añade el bloque de almacenamiento de energía a la pestaña
                output.accept(ENERGY_GENERATOR_BLOCK.get()); // Añade el bloque de generador de energía a la pestaña
                output.accept(BATTERY_ITEM.get()); // Añade el item de batería a la pestaña
                output.accept(ENERGY_GENERATOR_UPDATER_TIER1.get()); // Añade el item de actualización de generador de energía de nivel 1 a la pestaña
                output.accept(ENERGY_GENERATOR_UPDATER_TIER2.get()); // Añade el item de actualización de generador de energía de nivel 2 a la pestaña
                output.accept(ENERGY_GENERATOR_UPDATER_TIER3.get()); // Añade el item de actualización de generador de energía de nivel 3 a la pestaña
                output.accept(ENERGY_GENERATOR_UPDATER_TIER4.get()); // Añade el item de actualización de generador de energía de nivel 4 a la pestaña
                output.accept(EXTRACTOR_PEAK_TIER1.get()); // Añade el item de pico de extractor de nivel 1 a la pestaña
                output.accept(EXTRACTOR_PEAK_TIER2.get()); // Añade el item de pico de extractor de nivel 2 a la pestaña
                output.accept(EXTRACTOR_PEAK_TIER3.get()); // Añade el item de pico de extractor de nivel 3 a la pestaña
                output.accept(EXTRACTOR_PROTECTOR.get()); // Añade el item de protector de extractor a la pestaña
                output.accept(RANGE_EXTRACTOR_UPDATER_TIER_1.get()); // Añade el item de actualización de extractor de rango de nivel 1 a la pestaña
                output.accept(RANGE_EXTRACTOR_UPDATER_TIER_2.get()); // Añade el item de actualización de extractor de rango de nivel 2 a la pestaña
                output.accept(RANGE_EXTRACTOR_UPDATER_TIER_3.get()); // Añade el item de actualización de extractor de rango de nivel 3 a la pestaña
                output.accept(RANGE_EXTRACTOR_UPDATER_TIER_4.get()); // Añade el item de actualización de extractor de rango de nivel 4 a la pestaña
                output.accept(RANGE_EXTRACTOR_UPDATER_TIER_5.get()); // Añade el item de actualización de extractor de rango de nivel 5 a la pestaña
                output.accept(AURELITE_INGOT.get()); // Añade el item de lingote de Aurum a la pestaña
                output.accept(AURELITE_HELMET.get()); // Añade el item de casco de Aurum a la pestaña
                output.accept(AURELITE_CHESTPLATE.get()); // Añade el item de peto de Aurum a la pestaña
                output.accept(AURELITE_LEGGINGS.get()); // Añade el item de pantalones de Aurum a la pestaña
                output.accept(AURELITE_BOOTS.get()); // Añade el item de botas de Aurum a la pestaña
                output.accept(VEILPIERCER.get()); // Añade el item Veilpiercer a la pestaña
                output.accept(EXPANSION_DASH.get()); // Añade el item de expansión Dash a la pestaña
                output.accept(EXPANSION_EXPLOSION.get()); // Añade el item de expansión Jump a la pestaña
                output.accept(EXPANSION_DAMAGE_RESISTANCE.get()); // Añade el item de expansión Fall a la pestaña
                output.accept(EXPANSION_FIRE_IMMUNE.get()); // Añade el item de expansión Speed a la pestaña
                output.accept(EXPANSION_SUPER_SPEED.get()); // Añade el item de expansión Super Speed a la pestaña
                output.accept(EXPANSION_HIGH_JUMP.get()); // Añade el item de expansión Super Jump a la pestaña
                output.accept(EXPANSION_REGENERATION.get()); // Añade el item de expansión Super Fall a la pestaña
                output.accept(EXPANSION_MAGIC_SHIELD.get()); // Añade el item de expansión Super Jump a la pestaña
                output.accept(EXPANSION_LAVA_IMMUNE.get()); // Añade el item de expansión Dash a la pestaña
                output.accept(EXPANSION_SOUL_TOTEM_1.get());
                output.accept(EXPANSION_SOUL_TOTEM_2.get());
                output.accept(EXPANSION_SOUL_TOTEM_3.get());
                output.accept(SOUL_MODIFICATION_TABLE_BLOCK.get());
                output.accept(PEDESTAL.get());
            }).build());


}
