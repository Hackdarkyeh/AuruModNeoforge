package aurum.aurum.client.renderer;

import aurum.aurum.Aurum;
import aurum.aurum.client.model.ModModelLayers;
import aurum.aurum.client.model.ModelCooperGolem;
import aurum.aurum.entity.CooperGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;


public class CooperGolemRenderer extends MobRenderer<CooperGolemEntity, ModelCooperGolem<CooperGolemEntity>> {
    public CooperGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelCooperGolem<>(context.bakeLayer(ModModelLayers.COOPER_GOLEM_LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CooperGolemEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Aurum.MODID,"textures/entities/cooper_golom.png");
    }
    @Override
    public void render(CooperGolemEntity entity, float entityYaw, float partialTicks,
                       PoseStack matrixStack, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entity, entityYaw, partialTicks, matrixStack, bufferIn, packedLightIn);

        if (entity.isAggressive()) {
            matrixStack.pushPose();
            matrixStack.scale(1.0F, 1.0F, 1.0F);
            matrixStack.popPose();
        }
    }



}

