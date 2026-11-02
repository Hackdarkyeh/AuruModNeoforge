package aurum.aurum.eventHandler.WeaponEvents;

import aurum.aurum.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.*;

@EventBusSubscriber(modid = "aurum")
public class TridentEchoEvents {
    private static final double RADIUS = 8.0;
    private static final float DAMAGE_MULT = 0.75F;
    private static final double SPEED = 2.5 * 1.1; // 2.5 es la velocidad de lanzamiento vanilla

    private static class EchoState {
        int bouncesLeft;
        boolean bounced;
        LivingEntity pending;
        final Set<UUID> hit = new HashSet<>();
        EchoState(int bounces) { this.bouncesLeft = bounces; }
    }

    private static final Map<ThrownTrident, EchoState> STATES = new WeakHashMap<>();

    // 75 % del daño en los rebotes
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getSource().getDirectEntity() instanceof ThrownTrident t) {
            EchoState st = STATES.get(t);
            if (st != null && st.bounced) e.setAmount(e.getAmount() * DAMAGE_MULT);
        }
    }

    // Tras un impacto: registrar la víctima y elegir el siguiente objetivo
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post e) {
        if (!(e.getSource().getDirectEntity() instanceof ThrownTrident t)) return;
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        ItemStack weapon = t.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;

        var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int lvl = EnchantmentHelper.getItemEnchantmentLevel(
                enchants.getOrThrow(ModEnchantments.TRIDENT_ECHO), weapon);
        if (lvl <= 0) return;

        EchoState st = STATES.computeIfAbsent(t, k -> new EchoState(lvl));
        st.hit.add(victim.getUUID());
        if (st.bouncesLeft <= 0) return;

        Entity owner = t.getOwner();
        LivingEntity next = level.getEntitiesOfClass(LivingEntity.class,
                        victim.getBoundingBox().inflate(RADIUS), c -> valid(c, owner, st, t))
                .stream().min(Comparator.comparingDouble(victim::distanceToSqr)).orElse(null);
        if (next == null) return;

        st.bouncesLeft--;
        st.pending = next;
    }

    // Al final del tick del tridente, redirigirlo (después de que vanilla lo "rebote")
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof ThrownTrident t) || t.level().isClientSide()) return;
        EchoState st = STATES.get(t);
        if (st == null || st.pending == null) return;

        LivingEntity target = st.pending;
        st.pending = null;
        if (!target.isAlive()) return;

        t.dealtDamage = false;           // gracias al AT
        st.bounced = true;
        Vec3 dir = target.getBoundingBox().getCenter().subtract(t.position()).normalize();
        t.setDeltaMovement(dir.scale(SPEED));
        t.hurtMarked = true;             // sincroniza el movimiento con el cliente
    }

    private static boolean valid(LivingEntity c, Entity owner, EchoState st, ThrownTrident t) {
        if (!c.isAlive() || c == owner || st.hit.contains(c.getUUID())) return false;
        if (!(c instanceof Enemy || c instanceof Player)) return false;       // solo hostiles y jugadores
        if (c instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (owner != null && owner.isAlliedTo(c)) return false;               // equipos
        if (owner instanceof Player op && c instanceof Player tp && !op.canHarmPlayer(tp)) return false; // PvP
        Vec3 from = t.position(), to = c.getBoundingBox().getCenter();
        return t.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, t)).getType() == HitResult.Type.MISS;   // sin paredes de por medio
    }
}