package aurum.aurum.init;

import aurum.aurum.item.ArmorItem.ArmorExpansions;
import aurum.aurum.item.ArmorItem.SoulExpansions;
import aurum.aurum.item.*;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier1;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier2;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier3;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier4;
import aurum.aurum.item.ExtractorPeakTier.ExtractorPeakUpdaterTier1;
import aurum.aurum.item.ExtractorPeakTier.ExtractorPeakUpdaterTier2;
import aurum.aurum.item.ExtractorPeakTier.ExtractorPeakUpdaterTier3;
import aurum.aurum.item.RangeExtractor.*;
import aurum.aurum.item.Swords.AureliteSword;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;


import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;


public class ModItems {
    // Crea un nuevo item de comida con el id "aurum:example_id", nutrición 1 y saturación 2
    public static final DeferredRegister.Items ITEMS_REGISTRY = DeferredRegister.createItems(MODID);


    public static final Supplier<Item> AURUM_HEALING_ITEM = ITEMS_REGISTRY.register("aurum_healing_item", AurumHealing::new);


    public static final Supplier<Item> AURUMROSA_BUCKET = ITEMS_REGISTRY.register("plague_aurum_bucket", AurumBucketItem::new);
    public static final Supplier<Item> SOUL_BUCKET = ITEMS_REGISTRY.register("soul_fluid_bucket_item", SoulFluidBucketItem::new);

    public static final Supplier<Item> BATTERY_ITEM = ITEMS_REGISTRY.register("battery_item", BatteryItem::new);

    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER1 = ITEMS_REGISTRY.register("energy_generator_updater_tier1", EnergyGeneratorUpdaterTier1::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER2 = ITEMS_REGISTRY.register("energy_generator_updater_tier2", EnergyGeneratorUpdaterTier2::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER3 = ITEMS_REGISTRY.register("energy_generator_updater_tier3", EnergyGeneratorUpdaterTier3::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER4 = ITEMS_REGISTRY.register("energy_generator_updater_tier4", EnergyGeneratorUpdaterTier4::new);

    public static final Supplier<Item> EXTRACTOR_PEAK_TIER1 = ITEMS_REGISTRY.register("extractor_peak_tier1", ExtractorPeakUpdaterTier1::new);
    public static final Supplier<Item> EXTRACTOR_PEAK_TIER2 = ITEMS_REGISTRY.register("extractor_peak_tier2", ExtractorPeakUpdaterTier2::new);
    public static final Supplier<Item> EXTRACTOR_PEAK_TIER3 = ITEMS_REGISTRY.register("extractor_peak_tier3", ExtractorPeakUpdaterTier3::new);

    public static Supplier<Item> EXTRACTOR_PROTECTOR = ITEMS_REGISTRY.register("extractor_protector", ExtractorProtector::new);

    public static Supplier<Item> RANGE_EXTRACTOR_UPDATER_TIER_1 = ITEMS_REGISTRY.register("range_extractor_updater_tier1", RangeExtractorUpdaterTier1::new);
    public static Supplier<Item> RANGE_EXTRACTOR_UPDATER_TIER_2 = ITEMS_REGISTRY.register("range_extractor_updater_tier2", RangeExtractorUpdaterTier2::new);
    public static Supplier<Item> RANGE_EXTRACTOR_UPDATER_TIER_3 = ITEMS_REGISTRY.register("range_extractor_updater_tier3", RangeExtractorUpdaterTier3::new);
    public static Supplier<Item> RANGE_EXTRACTOR_UPDATER_TIER_4 = ITEMS_REGISTRY.register("range_extractor_updater_tier4", RangeExtractorUpdaterTier4::new);
    public static Supplier<Item> RANGE_EXTRACTOR_UPDATER_TIER_5 = ITEMS_REGISTRY.register("range_extractor_updater_tier5", RangeExtractorUpdaterTier5::new);


    public static Supplier<Item> VEILPIERCER = ITEMS_REGISTRY.register("veilpiercer", Veilpiercer::new);


    public static Supplier<Item> AURELITE_INGOT = ITEMS_REGISTRY.register("aurelite_ingot", Aurelite_ingot::new);

    public static final DeferredItem<ModArmorItem> AURELITE_HELMET = ITEMS_REGISTRY.register("aurelite_helmet",
            () -> new ModArmorItem(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(35))));
    public static final DeferredItem<ModArmorItem> AURELITE_CHESTPLATE = ITEMS_REGISTRY.register("aurelite_chestplate",
            () -> new ModArmorItem(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(35))));
    public static final DeferredItem<ModArmorItem> AURELITE_LEGGINGS = ITEMS_REGISTRY.register("aurelite_leggings",
            () -> new ModArmorItem(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(35))));
    public static final DeferredItem<ModArmorItem> AURELITE_BOOTS = ITEMS_REGISTRY.register("aurelite_boots",
            () -> new ModArmorItem(ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(35))));


    public static final Supplier<Item> EXPANSION_SUPER_SPEED = ITEMS_REGISTRY.register("expansion_super_speed", () -> new ArmorExpansions(25,"expansion_super_speed"));
    public static final Supplier<Item> EXPANSION_DAMAGE_RESISTANCE = ITEMS_REGISTRY.register("expansion_damage_resistance", () -> new ArmorExpansions(45, "expansion_damage_resistance"));
    public static final Supplier<Item> EXPANSION_HIGH_JUMP = ITEMS_REGISTRY.register("expansion_high_jump", () -> new ArmorExpansions(20, "expansion_high_jump"));
    public static final Supplier<Item> EXPANSION_REGENERATION = ITEMS_REGISTRY.register("expansion_regeneration", () -> new ArmorExpansions(30, "expansion_regeneration"));
    public static final Supplier<Item> EXPANSION_FIRE_IMMUNE = ITEMS_REGISTRY.register("expansion_fire_immune", () -> new ArmorExpansions(50,"expansion_fire_immune"));
    public static final Supplier<Item> EXPANSION_DASH = ITEMS_REGISTRY.register("expansion_dash", () -> new ArmorExpansions(20,"expansion_dash"));
    public static final Supplier<Item> EXPANSION_EXPLOSION = ITEMS_REGISTRY.register("expansion_explosion", () -> new ArmorExpansions(35,"expansion_explosion"));
    public static final Supplier<Item> EXPANSION_LAVA_IMMUNE = ITEMS_REGISTRY.register("expansion_lava_immune", () -> new ArmorExpansions(30,"expansion_lava_immune"));
    public static final Supplier<Item> EXPANSION_MAGIC_SHIELD = ITEMS_REGISTRY.register("expansion_magic_shield", () -> new ArmorExpansions(60, "expansion_magic_shield"));

    public static final Supplier<Item> EXPANSION_SOUL_TOTEM_1 = ITEMS_REGISTRY.register("expansion_soul_totem_1", () -> new SoulExpansions(1, 0, 100));
    public static final Supplier<Item> EXPANSION_SOUL_TOTEM_2 = ITEMS_REGISTRY.register("expansion_soul_totem_2", () -> new SoulExpansions(2, 1000, 200));
    public static final Supplier<Item> EXPANSION_SOUL_TOTEM_3 = ITEMS_REGISTRY.register("expansion_soul_totem_3", () -> new SoulExpansions(3, 1000000, 300));

    public static final Supplier<Item> AURELITE_SWORD = ITEMS_REGISTRY.register("aurelite_sword",
            () -> new AureliteSword(Tiers.DIAMOND, new Item.Properties().durability(1000))); // Ejemplo con Tier.DIAMOND y 1000 de energía máxima);

    public static final Supplier<Item> PURIFIER = ITEMS_REGISTRY.register("purifier",
            () -> new PurifierItem(new Item.Properties().stacksTo(1)));

}
