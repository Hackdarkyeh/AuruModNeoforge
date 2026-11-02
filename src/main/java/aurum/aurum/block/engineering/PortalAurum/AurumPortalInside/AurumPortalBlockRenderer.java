package aurum.aurum.block.engineering.PortalAurum.AurumPortalInside;

import aurum.aurum.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Dibuja UN ÚNICO remolino que cubre todo el suelo del portal.
 *
 * Cada bloque del portal pinta solo su casilla de 1x1, tomando de la textura el trozo que le
 * corresponde según su posición respecto al centro del portal (se detecta solo, mirando los
 * bloques de portal vecinos). La textura (512x512) cubre 6x6 bloques: radio 3 alrededor del centro.
 *
 * El movimiento NO sale de fotogramas apilados: el remolino se hace girar rotando las
 * coordenadas UV, con una pequeña onda radial y un latido de brillo. Así la textura pesa
 * una fracción de lo que pesaba y la animación es fluida a cualquier FPS.
 */
public class AurumPortalBlockRenderer implements BlockEntityRenderer<AurumPortalBlockEntity> {

    private static final ResourceLocation PORTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/entities/other/aurum_portal.png");

    /** Bloques que abarca la textura de lado (debe coincidir con el generador: 6x6, radio 3). */
    private static final float TEXTURE_SPAN = 6.0F;
    /** Altura del plano sobre la base del bloque. */
    private static final float SURFACE_Y = 0.05F;
    /** Subdivisión de cada bloque (4x4 quads) para que la onda de distorsión se vea suave. */
    private static final int SUBDIV = 4;
    /** Radianes por tick (0.015 = una vuelta cada ~21 s). */
    private static final float ROTATION_PER_TICK = 0.015F;
    private static final int FULL_BRIGHT = 0xF000F0;

    public AurumPortalBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(@NotNull AurumPortalBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        updateCenter(blockEntity, level, pos);

        double centerX = blockEntity.cachedCenterX;
        double centerZ = blockEntity.cachedCenterZ;

        float time = level.getGameTime() + partialTick;
        float baseAngle = time * ROTATION_PER_TICK;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(PORTAL_TEXTURE));
        Matrix4f mat = poseStack.last().pose();

        // Rejilla de (SUBDIV+1)^2 vértices con su UV ya calculado
        int n = SUBDIV + 1;
        float[] u = new float[n * n];
        float[] v = new float[n * n];
        int[] shade = new int[n * n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float lx = i / (float) SUBDIV;
                float lz = j / (float) SUBDIV;

                // Posición respecto al centro del portal, en bloques
                double px = pos.getX() + lx - centerX;
                double pz = pos.getZ() + lz - centerZ;
                double r = Math.sqrt(px * px + pz * pz);

                // Giro rígido + una onda radial pequeña y acotada (no se enrolla con el tiempo)
                double a = baseAngle + 0.10D * Math.sin(r * 2.6D - time * 0.06D);
                double c = Math.cos(a);
                double s = Math.sin(a);
                double qx = px * c - pz * s;
                double qz = px * s + pz * c;

                int idx = i * n + j;
                u[idx] = Mth.clamp((float) (qx / TEXTURE_SPAN + 0.5D), 0.002F, 0.998F);
                v[idx] = Mth.clamp((float) (qz / TEXTURE_SPAN + 0.5D), 0.002F, 0.998F);

                // Latido de brillo que viaja hacia el centro
                shade[idx] = Mth.clamp((int) (238 + 17 * Math.sin(time * 0.07D + r * 1.8D)), 0, 255);
            }
        }

        for (int i = 0; i < SUBDIV; i++) {
            for (int j = 0; j < SUBDIV; j++) {
                float x0 = i / (float) SUBDIV;
                float x1 = (i + 1) / (float) SUBDIV;
                float z0 = j / (float) SUBDIV;
                float z1 = (j + 1) / (float) SUBDIV;

                // Cara superior, sentido antihorario visto desde arriba
                vertex(vc, mat, x0, z0, u[i * n + j], v[i * n + j], shade[i * n + j]);
                vertex(vc, mat, x0, z1, u[i * n + j + 1], v[i * n + j + 1], shade[i * n + j + 1]);
                vertex(vc, mat, x1, z1, u[(i + 1) * n + j + 1], v[(i + 1) * n + j + 1], shade[(i + 1) * n + j + 1]);
                vertex(vc, mat, x1, z0, u[(i + 1) * n + j], v[(i + 1) * n + j], shade[(i + 1) * n + j]);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float z, float u, float v, int shade) {
        vc.addVertex(mat, x, SURFACE_Y, z)
                .setColor(shade, shade, shade, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    /**
     * El centro del portal es el punto medio de la caja que forman los bloques de portal
     * cercanos (la forma es simétrica). Se recalcula cada segundo por si cambian los vecinos
     * o aún no estaban cargados.
     */
    private static void updateCenter(AurumPortalBlockEntity be, Level level, BlockPos pos) {
        long now = level.getGameTime();
        if (be.cachedCenterTime != Long.MIN_VALUE && now - be.cachedCenterTime < 20) {
            return;
        }
        be.cachedCenterTime = now;

        int minX = pos.getX(), maxX = pos.getX();
        int minZ = pos.getZ(), maxZ = pos.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                m.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (level.getBlockState(m).is(ModBlocks.AURUM_PORTAL_BLOCK.get())) {
                    minX = Math.min(minX, m.getX());
                    maxX = Math.max(maxX, m.getX());
                    minZ = Math.min(minZ, m.getZ());
                    maxZ = Math.max(maxZ, m.getZ());
                }
            }
        }
        be.cachedCenterX = (minX + maxX) / 2.0D + 0.5D;
        be.cachedCenterZ = (minZ + maxZ) / 2.0D + 0.5D;
    }
}