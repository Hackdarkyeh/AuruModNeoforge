package aurum.aurum.client.renderer;

import aurum.aurum.entity.EvokeAltered.EvokerFangsAltered;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerFangsAlteredRenderer extends EntityRenderer<EvokerFangsAltered> {
    // Textura vanilla por ahora; cámbiala por una de aurum:textures/entity/... cuando tengas la tuya
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
    private final EvokerFangsModel<EvokerFangsAltered> model;

    public EvokerFangsAlteredRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EvokerFangsModel<>(context.bakeLayer(ModelLayers.EVOKER_FANGS));
    }

    @Override
    public void render(EvokerFangsAltered entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float progress = entity.getAnimationProgress(partialTicks);
        if (progress != 0.0F) {
            float scale = 2.0F;
            if (progress > 0.9F) {
                scale *= (1.0F - progress) / 0.1F;
            }

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));
            poseStack.scale(-scale, -scale, scale);
            poseStack.translate(0.0, -0.626, 0.0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            this.model.setupAnim(entity, progress, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
            VertexConsumer vertexconsumer = buffer.getBuffer(this.model.renderType(TEXTURE_LOCATION));
            this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EvokerFangsAltered entity) {
        return TEXTURE_LOCATION;
    }
}