package aurum.aurum.init;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> AURUM_DEW = key("aurum_dew");
    public static final ResourceKey<Enchantment> ENERGY_SIPHON = key("energy_siphon");
    public static final ResourceKey<Enchantment> TRIDENT_ECHO = key("trident_echo");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("aurum", name));
    }

    public static void bootstrap(BootstrapContext<Enchantment> ctx) {
        HolderGetter<Item> items = ctx.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchants = ctx.lookup(Registries.ENCHANTMENT);
        HolderSet<Item> weapons = items.getOrThrow(ModTags.AURUM_WEAPONS);
        HolderSet<Item> tridents = items.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE);

        ctx.register(AURUM_DEW, Enchantment.enchantment(Enchantment.definition(
                weapons, 2, 5,
                Enchantment.dynamicCost(10, 8), Enchantment.dynamicCost(40, 8),
                4, EquipmentSlotGroup.MAINHAND)).build(AURUM_DEW.location()));

        ctx.register(ENERGY_SIPHON, Enchantment.enchantment(Enchantment.definition(
                weapons, 2, 3,
                Enchantment.dynamicCost(15, 10), Enchantment.dynamicCost(45, 10),
                4, EquipmentSlotGroup.MAINHAND)).build(ENERGY_SIPHON.location()));

        ctx.register(TRIDENT_ECHO, Enchantment.enchantment(Enchantment.definition(
                        tridents, 2, 3,
                        Enchantment.dynamicCost(15, 10), Enchantment.dynamicCost(45, 10),
                        4, EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(HolderSet.direct(enchants.getOrThrow(Enchantments.RIPTIDE)))
                .build(TRIDENT_ECHO.location()));
    }
}