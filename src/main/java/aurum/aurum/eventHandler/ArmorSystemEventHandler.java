package aurum.aurum.eventHandler;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import aurum.aurum.item.ArmorItem.ArmorTierData;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class ArmorSystemEventHandler {
    private static final int[] EXP_REQUIREMENTS = {100, 200, 300};

    // --- LÓGICA DE EXPERIENCIA (Tu código original) ---
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || !player.isAlive() || event.getNewDamage() <= 0) return;

        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof ModArmorItem) {
                processArmorExp(armor, (int)event.getNewDamage(), player);
            }
        }
    }

    // --- LÓGICA DE HABILIDADES (La nueva pieza del puzzle) ---
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        // Solo recalculamos si el cambio fue en un slot de armadura
        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            updateActiveAbilities(player);
        }
    }

    private static void updateActiveAbilities(Player player) {
// Ahora recibes el contenedor, no el SoulData directamente
        SoulData.PlayerSoulContainer container = player.getData(ModAttachments.ACTIVE_SOUL);
        ItemStack stack = container.soulStack();
        SoulData soul = stack.get(ModDataComponents.SOUL_COMPONENT.get());
        boolean hasFullSet = hasFullModSet(player);

        // 1. Limpiar efectos anteriores para evitar duplicados o residuos
        removeAbilityEffects(player);

        if (!hasFullSet) return;

        if (soul == null || soul.idTier() <= 0) return; // Alma vacía

        int currentWeight = soul.abilities().stream().mapToInt(SoulAbilityData::weight).sum();

        if (currentWeight <= soul.maxWeight()) {
            for (SoulAbilityData ability : soul.abilities()) {
                applySpecificAbility(player, ability, soul.honor(), soul.idTier());
            }
        } else {
            // Opcional: Notificar al jugador que su alma está sobrecargada
        }
    }
    private static void applySpecificAbility(Player player, SoulAbilityData ability, int honor, int tier) {
        // Aquí implementas la lógica real.
        // Puedes usar el 'honor' para escalar la potencia del efecto.
        // Ejemplo:
        // float power = ability.basePower() * (1 + (honor / 100f));
    }

    private static boolean hasFullModSet(Player player) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ModArmorItem)) return false;
        }
        return true;
    }

    // --- MÉTODOS DE APOYO ---

    private static void processArmorExp(ItemStack armor, int damageReceived, Player player) {
        ArmorTierData tierData = armor.getOrDefault(ModDataComponents.ARMOR_TIER.get(), ArmorTierData.defaultData());
        ArmorExpData expData = armor.getOrDefault(ModDataComponents.ARMOR_EXP.get(),
                new ArmorExpData(0, 0, EXP_REQUIREMENTS[Math.max(0, tierData.tierLevel() - 1)]));

        ArmorExpData newData = expData.addExp(damageReceived, tierData);
        armor.set(ModDataComponents.ARMOR_EXP.get(), newData);

        if (newData.currentLevel() > expData.currentLevel()) {
            player.displayClientMessage(Component.literal("¡Armadura nivel " + newData.currentLevel() + "!")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    private static void applyAbilityEffects(Player player, ItemStack skillItem) {
        // Aquí disparas la lógica de la habilidad (ej. añadir un AttributeModifier o efecto)
        // Por ejemplo: si el skillItem es un "Amuleto de Fuerza", dar Efecto de Fuerza.
    }

    private static void removeAbilityEffects(Player player) {
        // Limpiar los efectos cuando se quita la armadura o la habilidad
    }
}