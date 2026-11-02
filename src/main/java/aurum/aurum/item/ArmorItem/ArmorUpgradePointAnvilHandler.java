package aurum.aurum.item.ArmorItem;

import aurum.aurum.Aurum;
import aurum.aurum.client.gui.Player.ArmorSkillsData;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

/** Yunque: pieza autorizada + armor_upgrade_point = la misma pieza con 1 punto más. */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorUpgradePointAnvilHandler {

    /**
     * El yunque vanilla NO deja recoger el resultado si el coste es 0 (mayPickup exige cost > 0),
     * así que se cobra 1 nivel y se devuelve en onAnvilRepair.
     */
    private static final int ANVIL_COST = 1;

    private static boolean isPointItem(ItemStack stack) {
        return stack.is(ModItems.ARMOR_UPGRADE_POINT.get());
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!isPointItem(right)) return;
        if (!AurumArmor.is(left)) return; // solo armaduras con ARMOR_AUTHORIZED

        ItemStack output = left.copy();
        if (!addPoint(output)) return;

        event.setOutput(output);
        event.setCost(ANVIL_COST);
        event.setMaterialCost(1); // consume 1 unidad del item
    }

    /** Devuelve el nivel cobrado para que, en la práctica, cueste 0 XP. */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!isPointItem(event.getRight()) || !AurumArmor.is(event.getLeft())) return;

        Player player = event.getEntity();
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(ANVIL_COST);
    }

    /**
     * Añade 1 punto extra a la pieza. Devuelve false si ya tiene todos los puntos que se pueden
     * gastar (así el yunque no se come el item para nada).
     */
    private static boolean addPoint(ItemStack stack) {
        ArmorSkillsData skills = stack.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());

        // Los puntos extra se suman a los 3 del tier: más de 30 - 3 = 27 no se pueden gastar nunca
        if (skills.bonusPoints() >= ArmorSkillsData.MAX_TOTAL_POINTS - ArmorSkillsData.MAX_TIER) return false;

        stack.set(ModDataComponents.ARMOR_SKILLS.get(), skills.withBonusPoints(skills.bonusPoints() + 1));
        return true;
    }
}