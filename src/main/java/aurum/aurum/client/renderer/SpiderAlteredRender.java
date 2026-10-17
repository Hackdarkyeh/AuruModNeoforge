package aurum.aurum.client.renderer;

import aurum.aurum.client.model.AAModModelLayers;
import aurum.aurum.client.model.SpiderCorruptedModel;
import aurum.aurum.entity.SpiderCorrupted.SpiderCorruptedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static aurum.aurum.Aurum.MODID;

public class SpiderAlteredRender extends MobRenderer<SpiderCorruptedEntity, SpiderCorruptedModel<SpiderCorruptedEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entities/spider_corrupted.png");

    public SpiderAlteredRender(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiderCorruptedModel<>(ctx.bakeLayer(AAModModelLayers.SPIDER_CORRUPTED_LAYER)), 0.5f);
    }

    @Override
    public void render(SpiderCorruptedEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Efectos visuales según la fase
        //if (entity.getCurrentPhase() == 1) {
        //    poseStack.scale(1.2f, 1.2f, 1.2f); // Aumentar tamaño en fase final
        //}

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiderCorruptedEntity entity) {
        return TEXTURE;
    }
}

