package aurum.aurum;

import aurum.aurum.Commands.AurumBlightCommands;
import aurum.aurum.client.renderer.CooperGolemRenderer;
import aurum.aurum.eventHandler.AurumBlightRain;
import aurum.aurum.eventHandler.BlockChecker;
import aurum.aurum.init.ModEntities;
import aurum.aurum.init.ModParticles;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static aurum.aurum.init.ModBloques.*;
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

        // Registra el Registro Diferido al bus de eventos del mod para que los ModBloques se registren
        BLOCK_REGISTRY.register(modEventBus);
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

        //NeoForge.EVENT_BUS.register(BlockChecker.class);
        NeoForge.EVENT_BUS.register(AurumBlightCommands.class);
        NeoForge.EVENT_BUS.register(AurumBlightRain.class);
        ModEntities.ENTITY_REGISTER.register(modEventBus);


        // Registra la ForgeConfigSpec de nuestro mod para que Forge pueda crear y cargar el archivo de configuración por nosotros
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Algun código de configuración común
        LOGGER.info("HOLA DESDE LA CONFIGURACIÓN COMÚN");
        LOGGER.info("BLOQUE DE TIERRA >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

    }

    // Añade el item de bloque de ejemplo a la pestaña de ModBloques de construcción
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

        }
    }

    // Crea una pestaña creativa con el id "examplemod:INTERFAZ_CREATIVO" para el item de ejemplo, que se coloca después de la pestaña de combate
    public static final Supplier<CreativeModeTab> INTERFAZ_CREATIVO = CREATIVE_MODE_TABS.register("interfaz_creativo", () -> CreativeModeTab.builder()
            .title(Component.translatable("item_group." + MODID + ".interfaz_creativo"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Añade el item de ejemplo a la pestaña. Para tus propias pestañas, este método es preferido sobre el evento
                output.accept(WITHERED_GRASS_BLOCK.get()); // Añade el item de bloque de ejemplo a la pestaña
                output.accept(WITHERED_DIRT_BLOCK.get()); // Añade el item de bloque de tierra marchita a la pestaña
            }).build());



}
