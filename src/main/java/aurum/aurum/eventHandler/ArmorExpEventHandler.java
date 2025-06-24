package aurum.aurum.eventHandler;

import aurum.aurum.init.ModArmorMaterials;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.init.ModComponents;
import aurum.aurum.item.ArmorItem.ArmorTierData;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;


public class ArmorExpEventHandler {
    private static final int[] EXP_REQUIREMENTS = {100, 200, 300}; // EXP base por tier
    private static final int[] MAX_LEVELS = {10, 15, 20}; // Niveles máximos por tier

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        // Verificaciones básicas
        if (!(event.getEntity() instanceof Player player) ||
                !player.isAlive() ||
                event.getNewDamage() <= 0) {
            return;
        }

        // Procesar EXP para cada pieza de armadura
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof ModArmorItem) {
                processArmorExp(armor, (int)event.getNewDamage(), player);
            }
        }
    }

    private static void processArmorExp(ItemStack armor, int damageReceived, Player player) {
        // Obtener tier actual (con valor por defecto si es null)
        ArmorTierData tierData = armor.getOrDefault(
                ModComponents.ARMOR_TIER.get(),
                ArmorTierData.defaultData()
        );

        // Obtener EXP actual (con valor por defecto según tier)
        ArmorExpData expData = armor.getOrDefault(
                ModComponents.ARMOR_EXP.get(),
                new ArmorExpData(0, 0, EXP_REQUIREMENTS[tierData.tierLevel() - 1])
        );

        // Calcular nuevo EXP (considerando el tier)
        ArmorExpData newData = expData.addExp(damageReceived, tierData);
        armor.set(ModComponents.ARMOR_EXP.get(), newData);

        // Notificar subida de nivel
        if (newData.currentLevel() > expData.currentLevel()) {
            player.displayClientMessage(
                    Component.literal("¡Armadura nivel " + newData.currentLevel() + "!")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
        }
    }
}