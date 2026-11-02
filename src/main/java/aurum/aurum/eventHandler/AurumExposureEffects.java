package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.effectsPlayer.AurumBlight;
import aurum.aurum.effectsPlayer.AurumBlightDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Efectos de la exposición a Aurum sobre el cuerpo del jugador (paso 2):
 * partículas moradas, más hambre y daño periódico de tipo Aurum.
 * El aumento del daño recibido está en ArmorBlightShield.applyReductions.
 * Los valores van indexados por AurumExposureTier: NONE, LOW, MEDIUM, HIGH, CRITICAL.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumExposureEffects {

    // ── Valores ajustables (NONE, LOW, MEDIUM, HIGH, CRITICAL) ───────────────
    /** Agotamiento extra por segundo. 4.0 de agotamiento = 1 punto de hambre, así que 0.04 es ~1 punto cada 100 s. */
    private static final float[] EXTRA_EXHAUSTION_PER_SECOND = {0.00f, 0.04f, 0.06f, 0.08f, 0.10f};
    /** Probabilidad por segundo de que salgan partículas (en el tramo leve son "ocasionales"). */
    private static final float[] PARTICLE_CHANCE = {0.0f, 0.3f, 1.0f, 1.0f, 1.0f};
    private static final int[] PARTICLE_COUNT = {0, 3, 6, 10, 14};
    /** Daño de cada golpe periódico (antes de multiplicadores). El intervalo está en AurumExposureTier. */
    public static final float PERIODIC_DAMAGE = 1.0f;
    /** Si cada golpe también quita los efectos beneficiosos a las entidades cercanas (como hace el efecto Aurum). */
    private static final boolean STRIP_NEARBY_EFFECTS = true;

    private AurumExposureEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (!p.isAlive() || p.isCreative() || p.isSpectator()) {
            return;
        }
        AurumExposureTier tier = AurumExposureHandler.tier(p);
        if (tier == AurumExposureTier.NONE) {
            return;
        }
        int i = tier.ordinal();

        // Una vez por segundo: hambre y partículas
        if (p.tickCount % 20 == 0) {
            p.causeFoodExhaustion(EXTRA_EXHAUSTION_PER_SECOND[i]);

            if (p.level() instanceof ServerLevel level && p.getRandom().nextFloat() < PARTICLE_CHANCE[i]) {
                level.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.0, p.getZ(),
                        PARTICLE_COUNT[i], 0.4, 0.6, 0.4, 0.02);
            }
        }

        // Daño periódico de Aurum (solo desde el tramo alto): mismas consecuencias que un tick del efecto Aurum
        if (tier.damageIntervalTicks > 0 && p.tickCount % tier.damageIntervalTicks == 0) {
            // 1. Se come los efectos del jugador (salvo que la barrera aguante)
            AurumBlight.devourEffects(p);

            // 2. Golpe de Aurum con tu fuente de daño. invulnerableTime = 0 evita que vanilla lo ignore tras otro golpe.
            DamageSource source = AurumBlightDamageSource.createAurumBlightDamageSource(p.level().registryAccess());
            p.invulnerableTime = 0;
            p.hurt(source, PERIODIC_DAMAGE);

            // 3. Quita los efectos beneficiosos a quien esté cerca
            if (STRIP_NEARBY_EFFECTS) {
                AurumBlight.stripNearbyBeneficialEffects(p);
            }
        }
    }
}