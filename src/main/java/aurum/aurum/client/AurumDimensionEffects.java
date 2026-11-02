package aurum.aurum.client;

import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class AurumDimensionEffects extends DimensionSpecialEffects {

    // Colores objetivo (RGB 0..1) de la niebla y del fondo por estado.
    private static final double[] STANDARD_NIGHT = {0.35, 0.05, 0.45}; // el morado de siempre
    private static final double[] DARK_NIGHT     = {0.17, 0.02, 0.22};
    private static final double[] NO_MOON        = {0.10, 0.01, 0.14};
    private static final double[] PURPLE_MOON    = {0.60, 0.10, 0.75};
    private static final double[] AURUM_RAIN     = {0.25, 0.10, 0.30};

    /** Fracción que se acerca al objetivo por tick de juego (0.02 = unos 3 s para casi completar). */
    private static final double FADE_PER_TICK = 0.02;

    private final double[] current = {STANDARD_NIGHT[0], STANDARD_NIGHT[1], STANDARD_NIGHT[2]};
    private long lastGameTime = Long.MIN_VALUE;

    public AurumDimensionEffects() {
        // cloudLevel, forceBrightLightmap, skyType, forceDarkLightmap, constantAmbientLight
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float daylight) {
        double[] target = targetColor(AurumSkyClient.getState());

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            long now = level.getGameTime();
            if (lastGameTime == Long.MIN_VALUE || now < lastGameTime) {
                // primera llamada o mundo nuevo (el reloj va hacia atrás): sin transición
                System.arraycopy(target, 0, current, 0, 3);
            } else if (now > lastGameTime) {
                double k = Math.min(1.0, (now - lastGameTime) * FADE_PER_TICK);
                for (int i = 0; i < 3; i++) {
                    current[i] += (target[i] - current[i]) * k;
                }
            }
            lastGameTime = now;
        }
        return new Vec3(current[0], current[1], current[2]);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false; // la densidad de la niebla la controla AurumFogClient según el estado de la noche
    }

    /**
     * Cielo propio de dim1. Devuelve true para que vanilla no dibuje el suyo.
     * [COMPROBAR] firma de renderSky en NeoForge 1.21.1: si el compilador dice que no sobrescribe nada,
     * pégame el error y la firma que te sugiera el IDE.
     */
    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        AurumSkyRenderer.render(level, modelViewMatrix, projectionMatrix, camera, isFoggy, setupFog);
        return true;
    }

    private static double[] targetColor(AurumWeatherState state) {
        if (state == null) {
            return STANDARD_NIGHT; // fuera de dim1 o sin datos aún
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