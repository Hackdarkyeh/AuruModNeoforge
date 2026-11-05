package aurum.aurum;

import aurum.aurum.Commands.AurumCommands;
import aurum.aurum.Commands.GenerateBridgesCommand;
import aurum.aurum.eventHandler.ArmorEvents.ArmorSystemEventHandler;
import aurum.aurum.eventHandler.AurumBlightRain;
import aurum.aurum.eventHandler.ServerEvents;
import aurum.aurum.init.*;
import aurum.aurum.init.GUI.ModMenuType;
import aurum.aurum.worldgen.Dimension.AurumDimension;
import aurum.aurum.worldgen.structure.AurumStructureTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static aurum.aurum.init.ModBlockEntities.BLOCK_ENTITIES_REGISTRY;
import static aurum.aurum.init.ModBlocks.*;
import static aurum.aurum.init.ModEffects.EFFECTS_REGISTRY;
import static aurum.aurum.init.ModFeatures.FEATURES;
import static aurum.aurum.init.ModItems.*;
import static aurum.aurum.worldgen.Dimension.AurumDimension.BIOME_SOURCES;

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

        // Registra los Registros Diferidos al bus de eventos del mod
        BLOCK_REGISTRY.register(modEventBus);
        BLOCK_ENTITIES_REGISTRY.register(modEventBus);
        ITEMS_REGISTRY.register(modEventBus);

        FEATURES.register(modEventBus);
        EFFECTS_REGISTRY.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        ModAttachments.register(modEventBus);

        modEventBus.register(ModNetworking.class);

        BIOME_SOURCES.register(modEventBus);

        // Nos registramos para los eventos del servidor y otros eventos del juego que nos interesan
        NeoForge.EVENT_BUS.register(this);

        // Registra el item a una pestaña creativa
        modEventBus.addListener(this::addCreative);

        ModParticles.REGISTRY.register(modEventBus);

        NeoForge.EVENT_BUS.register(AurumCommands.class);
        NeoForge.EVENT_BUS.register(GenerateBridgesCommand.class);
        NeoForge.EVENT_BUS.register(AurumBlightRain.class);
        NeoForge.EVENT_BUS.register(ServerEvents.class);

        ModEntities.ENTITY_REGISTER.register(modEventBus);

        ModBiomes.BIOMES.register(modEventBus);

        ModFluids.REGISTRY_FLUIDS.register(modEventBus);
        ModFluidTypes.REGISTRY_FLUID_TYPE.register(modEventBus);
        ModMenuType.MENU_TYPE_REGISTRY.register(modEventBus);

        ModDataComponents.register(modEventBus);
        AurumStructureTypes.STRUCTURE_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(ArmorSystemEventHandler.class);
        ModAttributes.ATTRIBUTES.register(modEventBus);

        STStructures.DEFERRED_REGISTRY_STRUCTURE.register(modEventBus);
        STStructurePlacements.DEFERRED_REGISTRY_STRUCTURE_PLACEMENT_TYPE.register(modEventBus);

        STStructureTypes.STRUCTURE_TYPES.register(modEventBus);
        STFeatures.FEATURES.register(modEventBus);

        // AurelitePickaxeHighlight se registra ahora en AurumClient (solo cliente)
        AurumDimension.CHUNK_GENERATORS.register(modEventBus);
        modEventBus.register(ModDataGenerators.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Algun código de configuración común
        LOGGER.info("HOLA DESDE LA CONFIGURACIÓN COMÚN");
        LOGGER.info("BLOQUE DE TIERRA >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        //if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        //event.accept(WITHERED_GRASS_BLOCK.get());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HOLA desde el inicio del servidor");
    }

    // Pestaña creativa
    public static final Supplier<CreativeModeTab> INTERFAZ_CREATIVO = CREATIVE_MODE_TABS.register("interfaz_creativo", () -> CreativeModeTab.builder()
            .title(Component.translatable("item_group." + MODID + ".interfaz_creativo"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> AURUM_HEALING_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WITHERED_GRASS_BLOCK.get());
                output.accept(WITHERED_DIRT_BLOCK.get());
                output.accept(VETALITA.get());
                output.accept(FROZEN_WITHERED_GRASS_BLOCK.get());
                output.accept(FROZEN_WITHERED_DIRT_BLOCK.get());
                output.accept(ANCIENT_AURUM_GRASS_BLOCK.get());
                output.accept(ANCIENT_AURUM_DIRT_BLOCK.get());
                output.accept(DRY_WITHERED_GRASS_BLOCK.get());
                output.accept(DRY_WITHERED_DIRT_BLOCK.get());
                output.accept(AURUM_HEALING_ITEM.get());
                output.accept(AURUMROSA_BUCKET.get());
                output.accept(SOUL_BUCKET.get());
                output.accept(AURELITE_ORE.get());
                output.accept(PIPE_BLOCK.get());
                output.accept(PIPE_DARK_BLOCK.get());
                output.accept(PANEL_BLOCK.get());
                output.accept(EXTRACTOR_BLOCK.get());
                output.accept(ENERGY_STORAGE_BLOCK.get());
                output.accept(ENERGY_GENERATOR_BLOCK.get());
                output.accept(DARK_ENERGY_TABLE.get());
                output.accept(BATTERY_ITEM.get());
                output.accept(ENERGY_GENERATOR_UPDATER_TIER1.get());
                output.accept(ENERGY_GENERATOR_UPDATER_TIER2.get());
                output.accept(ENERGY_GENERATOR_UPDATER_TIER3.get());
                output.accept(ENERGY_GENERATOR_UPDATER_TIER4.get());
                output.accept(EXTRACTOR_PEAK_TIER1.get());
                output.accept(EXTRACTOR_PEAK_TIER2.get());
                output.accept(EXTRACTOR_PEAK_TIER3.get());
                output.accept(EXTRACTOR_PROTECTOR.get());
                output.accept(aurum.aurum.item.Aurelite_ingot.createWithPurity(AURELITE_INGOT.get(), 5));
                output.accept(aurum.aurum.item.Aurelite_ingot.createWithPurity(AURELITE_INGOT.get(), 100));
                output.accept(AURELITE_FRAGMENT.get());
                output.accept(AURELITE_HELMET.get());
                output.accept(AURELITE_CHESTPLATE.get());
                output.accept(AURELITE_LEGGINGS.get());
                output.accept(AURELITE_BOOTS.get());
                output.accept(AURELITE_HELMET_GOLD.get());
                output.accept(AURELITE_CHESTPLATE_GOLD.get());
                output.accept(AURELITE_LEGGINGS_GOLD.get());
                output.accept(AURELITE_BOOTS_GOLD.get());
                output.accept(SYNTHESIZED_AURUM_BOTTLE.get());
                output.accept(EXPANSION_DASH.get());
                output.accept(EXPANSION_EXPLOSION.get());
                output.accept(EXPANSION_DAMAGE_RESISTANCE.get());
                output.accept(EXPANSION_FIRE_IMMUNE.get());
                output.accept(EXPANSION_SUPER_SPEED.get());
                output.accept(EXPANSION_HIGH_JUMP.get());
                output.accept(EXPANSION_REGENERATION.get());
                output.accept(EXPANSION_MAGIC_SHIELD.get());
                output.accept(EXPANSION_LAVA_IMMUNE.get());
                output.accept(EXPANSION_SOUL_TOTEM_1.get());
                output.accept(EXPANSION_SOUL_TOTEM_2.get());
                output.accept(EXPANSION_SOUL_TOTEM_3.get());
                output.accept(SOUL_MODIFICATION_TABLE_BLOCK.get());
                output.accept(ARMOR_TABLE_BLOCK.get());
                output.accept(PEDESTAL.get());
                output.accept(AURELITE_SWORD.get());
                output.accept(AURELITE_PICKAGE.get());
                output.accept(ANTI_ELYTRA_BLOCK.get());
                output.accept(AMBAR_LEAVES.get());
                output.accept(AMBAR_LOG.get());
                output.accept(AMBAR_PLANKS.get());
                output.accept(ModBlocks.AURUM_STAIRS);
                output.accept(ModBlocks.AURUM_SLAB);
                output.accept(ModBlocks.AURUM_PRESSURE_PLATE);
                output.accept(ModBlocks.AURUM_BUTTON);
                output.accept(ModBlocks.AURUM_FENCE);
                output.accept(ModBlocks.AURUM_FENCE_GATE);
                output.accept(ModBlocks.AURUM_WALL);
                output.accept(ModBlocks.AURUM_DOOR);
                output.accept(VEILPIERCER.get());
                output.accept(CERULEO.get());
                output.accept(TOBA.get());
                output.accept(AURUM_VINES.get());
                output.accept(AURUM_VINES_PLANT.get());
                output.accept(AURUM_ARROW.get());
                output.accept(GEIGER_COUNTER.get());
                // Libros de encantamiento de Aurum (todos los niveles)
                parameters.holders().lookup(Registries.ENCHANTMENT).ifPresent(enchants -> {
                    addEnchantBooks(output, enchants, ModEnchantments.AURUM_DEW);
                    addEnchantBooks(output, enchants, ModEnchantments.ENERGY_SIPHON);
                    addEnchantBooks(output, enchants, ModEnchantments.TRIDENT_ECHO);
                });
            }).build());

    private static void addEnchantBooks(CreativeModeTab.Output output,
                                        HolderLookup.RegistryLookup<Enchantment> lookup,
                                        ResourceKey<Enchantment> key) {
        lookup.get(key).ifPresent(holder -> {
            for (int lvl = holder.value().getMinLevel(); lvl <= holder.value().getMaxLevel(); lvl++) {
                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, lvl)));
            }
        });
    }
}