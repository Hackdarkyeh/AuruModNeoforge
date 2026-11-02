package aurum.aurum.eventHandler.ArmorEvents;

import aurum.aurum.Aurum;
import aurum.aurum.client.gui.Player.ArmorSkillsData;
import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor.EnergyType;
import aurum.aurum.eventHandler.AurumExposureTier;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModDamageTypes;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.AurumArmor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Barrera de Corrosión: solo contiene el daño de Aurum. Funciona con cualquier armadura autorizada (AurumArmor.is). */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorBlightShield {

    public static final float BARRIER_PER_ENERGY = EnergyConfig.Barrier.PER_ENERGY;
    public static final float DARK_ENERGY_QUALITY = EnergyConfig.Barrier.DARK_ENERGY_QUALITY;
    public static final float CLEAN_ENERGY_QUALITY = EnergyConfig.Barrier.CLEAN_ENERGY_QUALITY;
    public static final float VITAL_ENERGY_FRACTION = EnergyConfig.Barrier.VITAL_ENERGY_FRACTION;
    public static final int RECHARGE_DELAY = EnergyConfig.Barrier.RECHARGE_DELAY;
    public static final int RECHARGE_INTERVAL = EnergyConfig.Barrier.RECHARGE_INTERVAL;
    public static final float BASE_RECHARGE_PER_INTERVAL = EnergyConfig.Barrier.RECHARGE_POINTS;
    public static final float RECHARGE_ENERGY_PER_POINT = EnergyConfig.Barrier.RECHARGE_ENERGY_PER_POINT;

    private static final String LAST_HIT_KEY = "AurumBarrierLastHit";
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // ── Exposición a Aurum (funciona en cliente y servidor: el attachment está sincronizado) ──────

    /** Tramo de exposición del que lleva puesta la armadura (solo los jugadores se exponen). */
    private static AurumExposureTier exposureTier(LivingEntity e) {
        if (e instanceof Player p) {
            return AurumExposureTier.fromExposure(p.getData(ModAttachments.AURUM_EXPOSURE));
        }
        return AurumExposureTier.NONE;
    }

    /** Fracción de la capacidad de la barrera que se pierde por la exposición (0.15 = 15 %). */
    public static float barrierLoss(LivingEntity e) {
        return exposureTier(e).barrierLoss;
    }

    // ── Consultas (pantalla, pasivas) ────────────────────────────────────────

    private static ArmorSkillsData skills(ItemStack s) {
        return s.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());
    }

    private static float energyQuality(EnergyType type) {
        return switch (type) {
            case DARK_ENERGY -> DARK_ENERGY_QUALITY;
            case CLEAN_ENERGY -> CLEAN_ENERGY_QUALITY;
            default -> 0F;
        };
    }

    /** Capacidad base de la pieza, sin tener en cuenta la exposición de quien la lleva. */
    public static float getCapacity(ItemStack stack) {
        if (!AurumArmor.is(stack)) return 0F;
        return AurumArmor.getMaxEnergy(stack) * BARRIER_PER_ENERGY
                * energyQuality(AurumArmor.getType(stack)) * AurumArmor.materialQuality(stack)
                * (1F + skills(stack).getAbsorptionBonusMultiplier());
    }

    /** Capacidad efectiva: la base menos la parte que se pierde por la exposición del portador. */
    public static float getCapacity(ItemStack stack, LivingEntity wearer) {
        return getCapacity(stack) * (1F - barrierLoss(wearer));
    }

    public static int getVitalEnergy(ItemStack stack) {
        return Math.round(AurumArmor.getMaxEnergy(stack) * VITAL_ENERGY_FRACTION);
    }

    public static float getBarrier(ItemStack stack) {
        return Math.min(stack.getOrDefault(ModDataComponents.ARMOR_BARRIER.get(), 0F), getCapacity(stack));
    }

    /** Barrera efectiva de la pieza: nunca más que la capacidad efectiva con la exposición actual. */
    public static float getBarrier(ItemStack stack, LivingEntity wearer) {
        return Math.min(stack.getOrDefault(ModDataComponents.ARMOR_BARRIER.get(), 0F), getCapacity(stack, wearer));
    }

    private static void setBarrier(ItemStack stack, float value) {
        stack.set(ModDataComponents.ARMOR_BARRIER.get(), Math.max(0F, value));
    }

    /** Las 4 ranuras con armadura autorizada y con energía (una pieza muerta cuenta como no llevarla). */
    public static boolean isBarrierActive(LivingEntity e) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack s = e.getItemBySlot(slot);
            if (!AurumArmor.is(s) || !AurumArmor.hasProtectionPower(s)) return false;
        }
        return true;
    }

    public static boolean hasBarrier(LivingEntity e) {
        return isBarrierActive(e) && totalBarrier(e) > 0.01F;
    }

    private static float totalBarrier(LivingEntity e) {
        float sum = 0F;
        for (ItemStack s : e.getArmorSlots()) sum += getBarrier(s, e);
        return sum;
    }

    /**
     * Porcentaje de barrera respecto a la capacidad BASE: con exposición, el máximo que se puede llegar a ver
     * es 100 - pérdida (la parte perdida se pinta de morado en la pantalla de armadura).
     */
    public static int getBarrierPercent(LivingEntity e) {
        if (!isBarrierActive(e)) return 0;
        float cap = 0F;
        for (ItemStack s : e.getArmorSlots()) cap += getCapacity(s);
        return cap <= 0F ? 0 : Math.round(100F * totalBarrier(e) / cap);
    }

    /** Barrera que se gasta por punto de daño de Aurum (la Resonancia lo reduce). */
    private static float barrierRatio(ItemStack s) {
        return Math.max(0.01F, EnergyConfig.Barrier.BARRIER_LOSS_PER_DAMAGE * (1F - skills(s).getResonanceBonusMultiplier()));
    }

    /** Energía que pierde la pieza por punto de daño de Aurum (la Resonancia lo reduce). */
    private static float energyRatio(ItemStack s) {
        return Math.max(0.01F, EnergyConfig.Barrier.ENERGY_WEAR_PER_DAMAGE * (1F - skills(s).getResonanceBonusMultiplier()));
    }

    /**
     * Aumenta el daño de Aurum según el tramo de exposición del jugador.
     * Sigue habiendo hueco para la resistencia al aurum (encantamiento) y mejoras de tier.
     */
    private static float applyReductions(LivingEntity wearer, float damage) {
        return damage * (1F + exposureTier(wearer).damageBonus);
    }

    // ── Daño ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(ModDamageTypes.AURUM_BLIGHT_DAMAGE)) return;
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide()) return;

        wearer.getPersistentData().putLong(LAST_HIT_KEY, wearer.level().getGameTime());

        float damage = applyReductions(wearer, event.getAmount());
        float passthrough = isBarrierActive(wearer) ? absorb(wearer, damage) : damage;
        wearArmor(wearer, damage); // la armadura sufre haya barrera o no

        if (passthrough <= 0.01F) event.setCanceled(true);
        else event.setAmount(passthrough);
    }

    /** La barrera absorbe a partes iguales entre piezas; devuelve el daño que no pudo absorber. */
    private static float absorb(LivingEntity wearer, float damage) {
        List<ItemStack> pieces = new ArrayList<>();
        for (ItemStack s : wearer.getArmorSlots()) if (getBarrier(s, wearer) > 0.01F) pieces.add(s);

        float remaining = damage;
        while (remaining > 0.001F && !pieces.isEmpty()) {
            float share = remaining / pieces.size();
            for (Iterator<ItemStack> it = pieces.iterator(); it.hasNext(); ) {
                ItemStack s = it.next();
                float ratio = barrierRatio(s);
                float cur = getBarrier(s, wearer);
                float absorbed = Math.min(share, cur / ratio);
                setBarrier(s, cur - absorbed * ratio);
                remaining -= absorbed;
                if (getBarrier(s, wearer) <= 0.01F) it.remove();
            }
        }
        return Math.max(0F, remaining);
    }

    /** Autorizadas: gastan energía (sin energía, son indestructibles y no pierden nada). Sin autorizar: durabilidad x2. */
    private static void wearArmor(LivingEntity wearer, float damage) {
        List<EquipmentSlot> slots = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (wearer.getItemBySlot(slot).getItem() instanceof ArmorItem) slots.add(slot);
        }
        if (slots.isEmpty()) return;
        float perPiece = damage / slots.size();

        for (EquipmentSlot slot : slots) {
            ItemStack s = wearer.getItemBySlot(slot);
            if (AurumArmor.is(s)) {
                if (AurumArmor.hasProtectionPower(s)) {
                    AurumArmor.consume(s, Math.max(1, Math.round(perPiece * energyRatio(s))));
                }
            } else if (s.isDamageableItem()) {
                s.hurtAndBreak(Math.max(1, Math.round(perPiece * 2F)), wearer, slot);
            }
        }
    }

    // ── Recarga ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide() || p.tickCount % RECHARGE_INTERVAL != 0) return;
        if (!isBarrierActive(p)) return;
        if (p.level().getGameTime() - p.getPersistentData().getLong(LAST_HIT_KEY) < RECHARGE_DELAY) return;

        for (ItemStack s : p.getArmorSlots()) {
            float cur = getBarrier(s, p);
            float missing = getCapacity(s, p) - cur; // solo se recarga hasta la capacidad efectiva
            if (missing <= 0.01F) continue;

            float add = Math.min(BASE_RECHARGE_PER_INTERVAL * (1F + skills(s).getRecoveryBonusMultiplier()), missing);

            // Solo se gasta lo que sobra por encima de la energía mínima vital
            int spendable = AurumArmor.getEnergy(s) - getVitalEnergy(s);
            if (spendable <= 0) continue;
            add = Math.min(add, spendable / RECHARGE_ENERGY_PER_POINT);
            if (add <= 0F) continue;

            setBarrier(s, cur + add);
            AurumArmor.consume(s, (int) Math.ceil(add * RECHARGE_ENERGY_PER_POINT));
        }
    }

    /** Motivo por el que la barrera no está activa (clave de idioma), o null si lo está. Lo usa la pantalla. */
    public static String getInactiveReasonKey(LivingEntity e) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!AurumArmor.is(e.getItemBySlot(slot))) return "gui.aurum.passive.inactive.not_authorized";
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!AurumArmor.hasProtectionPower(e.getItemBySlot(slot))) return "gui.aurum.passive.inactive.no_energy";
        }
        if (totalBarrier(e) <= 0.01F) {
            for (ItemStack s : e.getArmorSlots()) {
                if (AurumArmor.getEnergy(s) > getVitalEnergy(s)) return "gui.aurum.passive.inactive.empty";
            }
            return "gui.aurum.passive.inactive.below_vital";
        }
        return null;
    }
    public static float aurumSoak(LivingEntity e) {
        if (!isBarrierActive(e)) return 0F;
        float sum = 0F;
        for (ItemStack s : e.getArmorSlots()) sum += getBarrier(s, e) / barrierRatio(s);
        return sum;
    }
    public static float aurumDamageMultiplier(LivingEntity e) {
        return 1F + exposureTier(e).damageBonus;
    }
}