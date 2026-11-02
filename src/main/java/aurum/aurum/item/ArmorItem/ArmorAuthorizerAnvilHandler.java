package aurum.aurum.item.ArmorItem;

import aurum.aurum.client.gui.Player.ArmorSkillsData;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class ArmorAuthorizerAnvilHandler {

    /** Calidad (armadura + dureza) a partir de la cual el Purifier se consume entero. */
    public static final int FULL_CONSUMPTION_QUALITY = 11;

    private static boolean isAuthorizerItem(ItemStack stack) {
        return stack.getItem() == ModItems.PURIFIER.get();
    }

    private static boolean isAuthorized(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARMOR_AUTHORIZED.get(), false);
    }

    /** Durabilidad del Purifier que cuesta autorizar esta armadura (entre 1 y su durabilidad máxima). */
    public static int getDurabilityCost(ItemStack purifier, ArmorItem armor) {
        int max = purifier.getMaxDamage();
        return Mth.clamp(Math.round(max * AurumArmor.quality(armor) / FULL_CONSUMPTION_QUALITY), 1, max);
    }

    /** Si la armadura cuesta más de lo que le queda al Purifier, no se puede autorizar. */
    private static boolean canAfford(ItemStack purifier, int cost) {
        return cost <= purifier.getMaxDamage() - purifier.getDamageValue();
    }

    /** Convierte la pieza en "una más del mod": todos los componentes de tier, energía, habilidades y barrera. */
    public static void authorize(ItemStack stack) {
        stack.set(ModDataComponents.ARMOR_AUTHORIZED.get(), true);
        if (!stack.has(ModDataComponents.ARMOR_TIER.get()))
            stack.set(ModDataComponents.ARMOR_TIER.get(), new ArmorTierData(1, true));
        if (!stack.has(ModDataComponents.ARMOR_ENERGY.get()))
            stack.set(ModDataComponents.ARMOR_ENERGY.get(), ArmorEnergyData.defaultData());
        if (!stack.has(ModDataComponents.ARMOR_EXP.get()))
            stack.set(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());
        if (!stack.has(ModDataComponents.ARMOR_SKILLS.get()))
            stack.set(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());
        // Las armaduras autorizadas son indestructibles (sin mostrar "Irrompible" en el tooltip)
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!isAuthorizerItem(right)) return;
        if (!(left.getItem() instanceof ArmorItem armor)) return;
        if (isAuthorized(left)) return;

        if (!canAfford(right, getDurabilityCost(right, armor))) return;

        ItemStack output = left.copy();
        authorize(output);

        event.setOutput(output);
        event.setCost(5);          // niveles de experiencia
        event.setMaterialCost(1);  // se consume; la durabilidad restante se devuelve en onAnvilRepair
    }

    /** El yunque solo puede consumir el Purifier, no dañarlo: devolvemos una copia con la durabilidad ya gastada. */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack right = event.getRight();
        ItemStack left = event.getLeft();
        if (!isAuthorizerItem(right) || !(left.getItem() instanceof ArmorItem armor)) return;
        if (isAuthorized(left) || !isAuthorized(event.getOutput())) return; // no es una autorización

        ItemStack remaining = right.copy();
        remaining.setCount(1);
        remaining.setDamageValue(remaining.getDamageValue() + getDurabilityCost(right, armor));
        if (remaining.getDamageValue() >= remaining.getMaxDamage()) return; // se gasta del todo

        Player player = event.getEntity();
        if (!player.getInventory().add(remaining)) player.drop(remaining, false);
    }
}