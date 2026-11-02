package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.client.gui.Player.ClientboundSetPassivesPacket;
import aurum.aurum.client.gui.Player.SetPassivesData;
import aurum.aurum.eventHandler.ArmorEvents.ArmorBlightShield;
import aurum.aurum.eventHandler.luminescence.LuminescenceScanner;
import aurum.aurum.init.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Las pasivas del set solo existen mientras la barrera tiene valor. Los niveles elegidos se conservan en el attachment. */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SetPassivesHandler {

    public static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "set_passive_health");
    private static final int INTERVAL = 20; // una vez por segundo

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        if (p.tickCount % INTERVAL == 0) apply(p);
        // La luminiscencia no es un efecto: el servidor calcula la amenaza y avisa al cliente (ver LuminescenceScanner)
        if (p instanceof ServerPlayer sp && p.tickCount % LuminescenceScanner.SCAN_INTERVAL == 0) {
            LuminescenceScanner.scan(sp);
        }
    }

    /** Idempotente: solo toca lo que difiere del estado deseado. */
    public static void apply(Player p) {
        SetPassivesData d = p.getData(ModDataComponents.SET_PASSIVES);
        boolean on = ArmorBlightShield.hasBarrier(p);

        syncHealth(p.getAttribute(Attributes.MAX_HEALTH), on ? d.health() : 0);
        syncEffect(p, MobEffects.JUMP, on ? d.jump() : 0, false);
        syncEffect(p, MobEffects.MOVEMENT_SPEED, on ? d.speed() : 0, false);

        // LIMPIEZA de la antigua pasiva de visión nocturna: a quien aún tenga nuestro efecto infinito se lo quita
        // (nunca una poción normal). Se puede borrar esta línea cuando ya no queden jugadores con el efecto antiguo.
        syncEffect(p, MobEffects.NIGHT_VISION, 0, true);
    }

    private static void syncHealth(AttributeInstance hp, int level) {
        if (hp == null) return;
        AttributeModifier cur = hp.getModifier(HEALTH_MODIFIER_ID);
        double want = level * 20.0D;
        if (level <= 0) {
            if (cur != null) hp.removeModifier(HEALTH_MODIFIER_ID);
        } else if (cur == null || cur.amount() != want) {
            hp.removeModifier(HEALTH_MODIFIER_ID);
            hp.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, want, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void syncEffect(Player p, Holder<MobEffect> effect, int level, boolean binary) {
        MobEffectInstance cur = p.getEffect(effect);
        if (level <= 0) {
            // solo quitamos la nuestra (infinita), nunca una poción del jugador
            if (cur != null && cur.isInfiniteDuration()) p.removeEffect(effect);
            return;
        }
        int amp = binary ? 0 : level - 1;
        if (cur == null || cur.getAmplifier() != amp || !cur.isInfiniteDuration()) {
            p.addEffect(new MobEffectInstance(effect, -1, amp, false, false, true));
        }
    }

    // ── Sincronización con el cliente (la pantalla lee el attachment) ────────

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundSetPassivesPacket(player.getData(ModDataComponents.SET_PASSIVES)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) LuminescenceScanner.forget(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
            LuminescenceScanner.reset(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
            LuminescenceScanner.reset(sp); // las ids de entidad son por dimensión
        }
    }
}