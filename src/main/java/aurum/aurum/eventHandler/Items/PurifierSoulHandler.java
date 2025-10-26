package aurum.aurum.eventHandler.Items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import aurum.aurum.item.PurifierItem;

@EventBusSubscriber
public class PurifierSoulHandler {

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        // Verificamos que el atacante sea un jugador
        if (event.getSource().getEntity() instanceof Player player) {

            // Verificamos que la víctima sea un mob (no jugador, ni item, etc.)
            if (event.getEntity() instanceof Mob mob) {

                // Obtenemos el ítem que el jugador tiene en la mano secundaria (offhand)
                ItemStack offhandItem = player.getOffhandItem();

                // Verificamos que sea el PurifierItem
                if (offhandItem.getItem() instanceof PurifierItem) {

                    // Calculamos cuánta "energía" obtenemos del mob
                    int recharge = calculateSoulValue(mob);

                    // Restauramos durabilidad (reparamos el ítem)
                    int currentDamage = offhandItem.getDamageValue();
                    int newDamage = Math.max(currentDamage - recharge, 0); // No puede pasar de 0 (máxima durabilidad)
                    offhandItem.setDamageValue(newDamage);

                    // (Opcional) mostrar efecto visual o sonido al absorber alma
                    mob.level().levelEvent(2005, mob.blockPosition(), 0); // partículas verdes tipo "enchant"
                }
            }
        }
    }

    /**
     * Calcula el valor del alma del mob basándose en su vida y daño.
     * Mientras más fuerte el mob, más energía otorga.
     */
    private static int calculateSoulValue(Mob mob) {
        // Vida máxima del mob
        float health = mob.getMaxHealth();

        // Daño base promedio (si tiene el atributo ATTACK_DAMAGE)
        double attackDamage = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                ? mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue()
                : 1.0;

        // Valor base de alma, escalado según poder del mob
        // Limitamos a un máximo para evitar exploits con bosses
        return (int) Math.min(health + attackDamage * 2, 50);
    }
}
