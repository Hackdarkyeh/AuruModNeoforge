package aurum.aurum.client;

import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;

/**
 * Cielo de dim1 (solo cliente): estrellas y luna según el estado de la noche.
 * Se llama desde AurumDimensionEffects.renderSky.
 */
public final class AurumSkyRenderer {

    private static final ResourceLocation MOON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png"); // [COMPROBAR] ruta en 1.21.1

    /** Inclinación de la luna sobre el horizonte (la hora es fija, así que no se mueve). 180 = justo encima. */
    private static final float MOON_ANGLE_DEG = 205.0f;
    private static final float MOON_SIZE_NORMAL = 20.0f;
    private static final float MOON_SIZE_PURPLE = 32.0f;
    private static final int STAR_COUNT = 1500;

    /** Fracción que se acerca al objetivo por tick de juego (igual que color y niebla). */
    private static final float FADE_PER_TICK = 0.02f;

    private static VertexBuffer starBuffer = null;

    // Valores actuales (suavizados)
    private static float moonAlpha = 1.0f;
    private static float purpleMix = 0.0f;
    private static float starBrightness = 0.9f;
    private static long lastGameTime = Long.MIN_VALUE;

    private AurumSkyRenderer() {}

    public static void render(ClientLevel level, Matrix4f modelView, Matrix4f projection,
                              Camera camera, boolean isFoggy, Runnable setupFog) {
        // Bajo el agua, en lava o con efectos que tapan el cielo no se dibuja nada (como en vanilla)
        if (isFoggy || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }

        updateSmoothing(level.getGameTime());

        FogRenderer.setupNoFog(); // [COMPROBAR] existe en 1.21.1
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        drawStars(modelView, projection);
        drawMoon(level, modelView);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        setupFog.run();
    }

    private static void updateSmoothing(long now) {
        AurumWeatherState state = AurumSkyClient.getState();
        float targetMoon = state == AurumWeatherState.NO_MOON ? 0.0f : 1.0f;
        float targetPurple = state == AurumWeatherState.PURPLE_MOON ? 1.0f : 0.0f;
        float targetStars = starTarget(state);

        if (lastGameTime == Long.MIN_VALUE || now < lastGameTime) {
            // primera vez o mundo nuevo (el reloj va hacia atrás): sin transición
            moonAlpha = targetMoon;
            purpleMix = targetPurple;
            starBrightness = targetStars;
        } else if (now > lastGameTime) {
            float k = Math.min(1.0f, (now - lastGameTime) * FADE_PER_TICK);
            moonAlpha += (targetMoon - moonAlpha) * k;
            purpleMix += (targetPurple - purpleMix) * k;
            starBrightness += (targetStars - starBrightness) * k;
        }
        lastGameTime = now;
    }

    private static float starTarget(AurumWeatherState state) {
        if (state == null) {
            return 0.9f;
        }
        switch (state) {
            case DARK_NIGHT:  return 0.60f;
            case NO_MOON:     return 1.00f;
            case PURPLE_MOON: return 0.70f;
            case AURUM_RAIN:  return 0.25f;
            default:          return 0.90f;
        }
    }

    private static void drawStars(Matrix4f modelView, Matrix4f projection) {
        if (starBrightness <= 0.01f) {
            return;
        }
        if (starBuffer == null) {
            starBuffer = createStars();
        }
        RenderSystem.setShaderColor(
                Mth.lerp(purpleMix, 1.0f, 0.85f),
                Mth.lerp(purpleMix, 1.0f, 0.55f),
                1.0f,
                starBrightness);
        starBuffer.bind();
        starBuffer.drawWithShader(modelView, projection, GameRenderer.getPositionShader()); // [COMPROBAR] firma
        VertexBuffer.unbind();
    }

    private static void drawMoon(ClientLevel level, Matrix4f modelView) {
        if (moonAlpha <= 0.01f) {
            return;
        }
        Matrix4f m = new Matrix4f(modelView);
        m.rotateY((float) Math.toRadians(-90.0));
        m.rotateX((float) Math.toRadians(MOON_ANGLE_DEG));

        float size = Mth.lerp(purpleMix, MOON_SIZE_NORMAL, MOON_SIZE_PURPLE);
        int phase = purpleMix > 0.5f ? 0 : level.getMoonPhase(); // la luna morada siempre llena
        int col = phase % 4;
        int row = phase / 4 % 2;
        float u0 = col / 4.0f;
        float v0 = row / 2.0f;
        float u1 = (col + 1) / 4.0f;
        float v1 = (row + 1) / 2.0f;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON_TEXTURE);
        RenderSystem.setShaderColor(
                Mth.lerp(purpleMix, 1.0f, 0.75f),
                Mth.lerp(purpleMix, 1.0f, 0.25f),
                1.0f,
                moonAlpha);

        // Mezcla aditiva como en vanilla: el negro de la textura no suma nada y desaparece
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -size, -100.0f, size).setUv(u1, v1);
        bb.addVertex(m, size, -100.0f, size).setUv(u0, v1);
        bb.addVertex(m, size, -100.0f, -size).setUv(u0, v0);
        bb.addVertex(m, -size, -100.0f, -size).setUv(u1, v0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    /** Estrellas repartidas en una esfera de radio 100: cada una es un cuadradito orientado hacia el centro. */
    private static VertexBuffer createStars() {
        RandomSource random = RandomSource.create(10842L);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int i = 0; i < STAR_COUNT; i++) {
            float x = random.nextFloat() * 2.0f - 1.0f;
            float y = random.nextFloat() * 2.0f - 1.0f;
            float z = random.nextFloat() * 2.0f - 1.0f;
            float len2 = x * x + y * y + z * z;
            if (len2 >= 1.0f || len2 <= 0.01f) {
                continue; // descarta puntos fuera de la esfera unidad o demasiado cerca del centro
            }
            float inv = (float) (1.0 / Math.sqrt(len2));
            x *= inv;
            y *= inv;
            z *= inv;

            float size = 0.15f + random.nextFloat() * 0.1f;

            // Base ortonormal perpendicular a la dirección (x, y, z)
            float ux = 0.0f, uy = 1.0f, uz = 0.0f;
            if (Math.abs(y) > 0.99f) {
                ux = 1.0f;
                uy = 0.0f;
            }
            float rx = y * uz - z * uy;
            float ry = z * ux - x * uz;
            float rz = x * uy - y * ux;
            float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
            rx /= rl;
            ry /= rl;
            rz /= rl;
            float vx = y * rz - z * ry;
            float vy = z * rx - x * rz;
            float vz = x * ry - y * rx;

            // Giro aleatorio del cuadradito sobre sí mismo
            float roll = random.nextFloat() * Mth.TWO_PI;
            float cr = Mth.cos(roll);
            float sr = Mth.sin(roll);
            float ax = (rx * cr + vx * sr) * size;
            float ay = (ry * cr + vy * sr) * size;
            float az = (rz * cr + vz * sr) * size;
            float bx = (-rx * sr + vx * cr) * size;
            float by = (-ry * sr + vy * cr) * size;
            float bz = (-rz * sr + vz * cr) * size;

            float cx = x * 100.0f;
            float cy = y * 100.0f;
            float cz = z * 100.0f;

            bb.addVertex(cx - ax - bx, cy - ay - by, cz - az - bz);
            bb.addVertex(cx + ax - bx, cy + ay - by, cz + az - bz);
            bb.addVertex(cx + ax + bx, cy + ay + by, cz + az + bz);
            bb.addVertex(cx - ax + bx, cy - ay + by, cz - az + bz);
        }

        MeshData mesh = bb.buildOrThrow();
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(mesh);
        VertexBuffer.unbind();
        return vb;
    }
}