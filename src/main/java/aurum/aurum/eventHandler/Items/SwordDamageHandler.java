package aurum.aurum.eventHandler.Items;

// SwordDamageHandler.java

import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.item.Swords.AureliteSword;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
public class SwordDamageHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();

            if (weapon.getItem() instanceof AureliteSword sword) {
                AureliteSword.EnergyData data = AureliteSword.getEnergyData(weapon);

                float energyBasedDamage = switch(data.energyType()) {
                    case DARK_ENERGY -> EnergyConfig.AureliteSword.DARK_ENERGY_DAMAGE;
                    case CLEAN_ENERGY -> EnergyConfig.AureliteSword.CLEAN_ENERGY_DAMAGE;
                    default -> EnergyConfig.AureliteSword.BASE_DAMAGE;
                };

                float damageMultiplier = energyBasedDamage / EnergyConfig.AureliteSword.BASE_DAMAGE;

                // DEBUG DETALLADO
                System.out.println("=== DEBUG DAÑO ESPADA ===");
                System.out.println("Tipo de energía: " + data.energyType());
                System.out.println("Daño base config: " + EnergyConfig.AureliteSword.BASE_DAMAGE);
                System.out.println("Daño por energía: " + energyBasedDamage);
                System.out.println("Multiplicador: " + damageMultiplier);
                System.out.println("Daño original: " + event.getAmount());

                float newDamage = event.getAmount() * damageMultiplier;
                event.setAmount(newDamage);

                System.out.println("Nuevo daño: " + newDamage);
                System.out.println("======================");

                sword.consumeEnergyForAttack(weapon);
            }
        }
    }
}