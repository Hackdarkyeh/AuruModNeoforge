package aurum.aurum.client;

import aurum.aurum.Aurum;
import aurum.aurum.eventHandler.AurumExposureHandler;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Densidad de la niebla de dim1 (solo cliente): depende del estado de la noche y de la exposición del jugador.
 * Cada fila de estado: {multiplicador del final de la niebla, inicio de la niebla como fracción del final}.
 * Multiplicador 1.0 = niebla de terreno normal (prácticamente sin niebla); más bajo = niebla más cercana y densa.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AurumFogClient {

    private static final double[] STANDARD_NIGHT = {1.00, 0.90}; // sin niebla
    private static final double[] DARK_NIGHT     = {0.55, 0.20}; // parecida a la de antes
    private static final double[] NO_MOON        = {0.35, 0.10}; // la más cerrada
    private static final double[] PURPLE_MOON    = {0.80, 0.40}; // niebla ligera
    private static final double[] AURUM_RAIN     = {0.50, 0.15};

    /** Fracción de la exposición máxima a partir de la cual la niebla empieza a cerrarse (0.45 = tramo medio). */
    private static final double EXPOSURE_FOG_START = 0.45;
    /** Con la exposición al máximo, el final de la niebla se queda en esta fracción de su distancia normal. */
    private static final double EXPOSURE_FOG_MIN_FACTOR = 0.50;

    /** Fracción que se acerca al objetivo por tick de juego (igual que el color de la niebla). */
    private static final double FADE_PER_TICK = 0.02;

    private static final double[] current = {STANDARD_NIGHT[0], STANDARD_NIGHT[1]};
    private static double currentExposureFactor = 1.0;
    private static long lastGameTime = Long.MIN_VALUE;

    private AurumFogClient() {}

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!AurumSkyClient.isInAurum()) {
            return;
        }
        // Solo la niebla de terreno al aire libre (no agua, lava ni nieve en polvo)
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }

        double[] target = target(AurumSkyClient.getState());
        double targetExposureFactor = exposureFactor();

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            long now = level.getGameTime();
            if (lastGameTime == Long.MIN_VALUE || now < lastGameTime) {
                // primera vez o mundo nuevo (el reloj va hacia atrás): sin transición
                current[0] = target[0];
                current[1] = target[1];
                currentExposureFactor = targetExposureFactor;
            } else if (now > lastGameTime) {
                double k = Math.min(1.0, (now - lastGameTime) * FADE_PER_TICK);
                current[0] += (target[0] - current[0]) * k;
                current[1] += (target[1] - current[1]) * k;
                currentExposureFactor += (targetExposureFactor - currentExposureFactor) * k;
            }
            lastGameTime = now;
        }

        // Nunca alejamos la niebla más de lo que ya estaba (respeta, p. ej., el efecto de ceguera)
        float far = (float) Math.min(event.getFarPlaneDistance(),
                event.getFarPlaneDistance() * current[0] * currentExposureFactor);
        float near = (float) Math.min(event.getNearPlaneDistance(), far * current[1]);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(near);
        event.setCanceled(true); // [COMPROBAR] en NeoForge los valores solo se aplican si se cancela el evento
    }

    /** Factor de niebla por exposición: 1.0 hasta el 45 % y baja de forma continua hasta EXPOSURE_FOG_MIN_FACTOR al 100 %. */
    private static double exposureFactor() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 1.0;
        }
        double fraction = player.getData(ModAttachments.AURUM_EXPOSURE) / (double) AurumExposureHandler.MAX_EXPOSURE;
        double t = Mth.clamp((fraction - EXPOSURE_FOG_START) / (1.0 - EXPOSURE_FOG_START), 0.0, 1.0);
        return Mth.lerp(t, 1.0, EXPOSURE_FOG_MIN_FACTOR);
    }

    private static double[] target(AurumWeatherState state) {
        if (state == null) {
            return STANDARD_NIGHT;
        }
        switch (state) {
            case DARK_NIGHT:  return DARK_NIGHT;
            case NO_MOON:     return NO_MOON;
            case PURPLE_MOON: return PURPLE_MOON;
            case AURUM_RAIN:  return AURUM_RAIN;
            default:          return STANDARD_NIGHT;
        }
    }
}