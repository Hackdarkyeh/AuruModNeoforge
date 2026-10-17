package aurum.aurum.block.engineering.PortalAurum.AurumPortalInside;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;



public class AurumPortalBlockRenderer implements BlockEntityRenderer<AurumPortalBlockEntity> {

    private static final ResourceLocation PORTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/entities/other/aurum_portal.png");

    public AurumPortalBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(@NotNull AurumPortalBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        poseStack.pushPose();

        // 1. Centramos en el bloque y lo dejamos a ras del fondo interior
        poseStack.translate(0.5D, 0.05D, 0.5D);

        // 2. Tumbamos el plano horizontalmente para que quede como un suelo/piscina
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));

        // ¡NOTA! Eliminamos cualquier "mulPose" de rotación Z. El plano ahora es 100% estático.

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(PORTAL_TEXTURE));

        // Brillo máximo para que destaque
        int light = 0xF000F0;
        float size = 1.0F;

        // 1. Configuración de fotogramas
        int totalFrames = 3; // La cantidad de cuadrados apilados en tu imagen
        int ticksPerFrame = 2; // El frametime que intentaste usar en el mcmeta

        int currentFrame = 0;
        if (blockEntity.getLevel() != null) {
            long time = blockEntity.getLevel().getGameTime();
            // Calculamos qué fotograma toca dibujar según el tiempo
            currentFrame = (int) ((time / ticksPerFrame) % totalFrames);
        }

        // 2. Cálculo del recorte vertical (V)
        float frameHeight = 1.0f / totalFrames; // Si hay 3 frames, cada uno mide 0.333f

        float uMin = 0.0f;
        float uMax = 1.0f;

        // Recortamos solo la porción de la imagen que corresponde al frame actual
        float vMin = currentFrame * frameHeight;
        float vMax = vMin + frameHeight;

        // Esquina Inferior Izquierda
        vertexConsumer.addVertex(poseStack.last().pose(), -size, -size, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(uMin, vMax)
                .setOverlay(packedOverlay)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);

        // Esquina Inferior Derecha
        vertexConsumer.addVertex(poseStack.last().pose(), size, -size, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(uMax, vMax)
                .setOverlay(packedOverlay)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);

        // Esquina Superior Derecha
        vertexConsumer.addVertex(poseStack.last().pose(), size, size, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(uMax, vMin)
                .setOverlay(packedOverlay)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);

        // Esquina Superior Izquierda
        vertexConsumer.addVertex(poseStack.last().pose(), -size, size, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(uMin, vMin)
                .setOverlay(packedOverlay)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);

        poseStack.popPose();
    }
}