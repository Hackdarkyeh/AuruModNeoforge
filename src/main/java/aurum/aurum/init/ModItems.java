package aurum.aurum.init;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;


import static net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID;

import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModItems {
    // Crea un nuevo item de comida con el id "aurum:example_id", nutrición 1 y saturación 2
    public static final DeferredRegister.Items ITEMS_REGISTRY = DeferredRegister.createItems(MOD_ID);

    public static final Supplier<Item> EXAMPLE_ITEM = ITEMS_REGISTRY.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(2f).build())));


}
