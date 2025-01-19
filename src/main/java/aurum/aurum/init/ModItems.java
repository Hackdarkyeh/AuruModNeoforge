package aurum.aurum.init;

import aurum.aurum.item.AurumHealing;
import aurum.aurum.item.AurumOpenPortal;
import aurum.aurum.item.AurumBucketItem;
import aurum.aurum.item.BatteryItem;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier1;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier2;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier3;
import aurum.aurum.item.EnergyGeneratorTier.EnergyGeneratorUpdaterTier4;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;


import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;


public class ModItems {
    // Crea un nuevo item de comida con el id "aurum:example_id", nutrición 1 y saturación 2
    public static final DeferredRegister.Items ITEMS_REGISTRY = DeferredRegister.createItems(MODID);

    public static final Supplier<Item> EXAMPLE_ITEM = ITEMS_REGISTRY.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(2f).build())));

    public static final Supplier<Item> AURUM_HEALING_ITEM = ITEMS_REGISTRY.register("aurum_healing_item", AurumHealing::new);

    public static final Supplier<Item> AURUM_OPEN_PORTAL_ITEM = ITEMS_REGISTRY.register("aurum_open_portal_item", AurumOpenPortal::new);

    public static final Supplier<Item> AURUMROSA_BUCKET = ITEMS_REGISTRY.register("plague_aurum_bucket", AurumBucketItem::new);

    public static final Supplier<Item> BATTERY_ITEM = ITEMS_REGISTRY.register("battery_item", BatteryItem::new);

    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER1 = ITEMS_REGISTRY.register("energy_generator_updater_tier1", EnergyGeneratorUpdaterTier1::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER2 = ITEMS_REGISTRY.register("energy_generator_updater_tier2", EnergyGeneratorUpdaterTier2::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER3 = ITEMS_REGISTRY.register("energy_generator_updater_tier3", EnergyGeneratorUpdaterTier3::new);
    public static final Supplier<Item> ENERGY_GENERATOR_UPDATER_TIER4 = ITEMS_REGISTRY.register("energy_generator_updater_tier4", EnergyGeneratorUpdaterTier4::new);

}
