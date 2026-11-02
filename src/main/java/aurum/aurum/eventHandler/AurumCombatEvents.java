package aurum.aurum.eventHandler;

import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.effectsPlayer.AurumBlight;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class AurumCombatEvents {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        Entity attacker = event.getContainer().getSource().getEntity();

        if (attacker instanceof ICorruptedMob corruptedAttacker) {
            double aurum = corruptedAttacker.getAurumValue();

            if (aurum > 0) {
                // 1. Filtrar el Aurum a través de la armadura antes de infectar
                double remainingAurum = mitigateAurumWithArmor(target, aurum);

                // 2. Solo aplicamos la plaga si la armadura no logró absorberlo todo
                if (remainingAurum > 0) {
                    AurumBlight.applyCorruptedBlight(target, remainingAurum);
                }
            }
        }
    }

    private static double mitigateAurumWithArmor(LivingEntity target, double incomingAurum) {
        double remainingAurum = incomingAurum;

        // Multiplicador de daño a la armadura (Ej: 3 de durabilidad perdida por cada punto de Aurum bloqueado)
        int durabilityDamage = (int) Math.ceil(incomingAurum * 3.0D);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorItem = target.getItemBySlot(slot);

                if (!armorItem.isEmpty() && isAntiAurumArmor(armorItem)) {
                    // Cada pieza bloquea una cantidad de Aurum (Ej: 2.5 puntos por pieza)
                    // Si lleva el set completo (4 piezas), bloquea hasta 10.0 de Aurum por golpe
                    double blockedByPiece = Math.min(remainingAurum, 2.5D);
                    remainingAurum -= blockedByPiece;

                    // Aplicar el daño de corrosión a la durabilidad de la armadura
                    armorItem.hurtAndBreak(durabilityDamage, target, slot);

                    // Si la armadura se rompe por el daño de Aurum, la eliminamos
                    if (armorItem.isEmpty()) {
                        target.setItemSlot(slot, ItemStack.EMPTY);
                    }

                    // Si ya se ha bloqueado todo el Aurum, salimos del bucle
                    if (remainingAurum <= 0) {
                        break;
                    }
                }
            }
        }

        return remainingAurum;
    }

    private static boolean isAntiAurumArmor(ItemStack stack) {
        // SUSTITUYE ESTO por tu ítem específico o Tag. Ejemplos:
        // Opción A (Por Tag, recomendado): return stack.is(ItemTags.create(new ResourceLocation("aurum", "anti_aurum_armor")));
        // Opción B (Por Ítem específico): return stack.is(ModItems.AURELITE_CHESTPLATE.get());

        return false;
    }
}