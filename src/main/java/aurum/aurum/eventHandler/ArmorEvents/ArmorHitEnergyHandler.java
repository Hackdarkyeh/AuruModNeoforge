package aurum.aurum.eventHandler.ArmorEvents;

import aurum.aurum.Aurum;
import aurum.aurum.item.ArmorItem.AurumArmor;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * La armadura del mod SOLO pierde energía cuando una entidad o un jugador golpea a quien la lleva puesta,
 * según el daño recibido (daño que llega x ModArmorItem.ENERGY_LOSS_PER_DAMAGE), repartido a partes
 * iguales entre las piezas que aún tienen energía. El daño del entorno y el que ignora la armadura no gasta.
 * El daño de Aurum lo gestiona ArmorBlightShield.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorHitEnergyHandler {

    @SubscribeEvent
    public static void onEntityDamaged(LivingDamageEvent.Post event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide()) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() == null || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return; // no lo ha golpeado una entidad, o es un daño que ignora la armadura
        }

        // Piezas del mod puestas que todavía tienen energía
        List<ItemStack> pieces = new ArrayList<>();
        for (ItemStack stack : wearer.getArmorSlots()) {
            if (AurumArmor.is(stack) && AurumArmor.hasProtectionPower(stack)) {
                pieces.add(stack);
            }
        }
        if (pieces.isEmpty()) {
            return;
        }

        int totalCost = Math.max(1, Math.round(event.getOriginalDamage() * ModArmorItem.ENERGY_LOSS_PER_DAMAGE));
        int perPiece = (totalCost + pieces.size() - 1) / pieces.size(); // reparto redondeando hacia arriba

        for (ItemStack stack : pieces) {
            AurumArmor.consume(stack, perPiece);
        }
    }
}