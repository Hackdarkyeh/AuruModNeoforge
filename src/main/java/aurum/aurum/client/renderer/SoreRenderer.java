package aurum.aurum.client.renderer;

import aurum.aurum.client.model.ModModelLayers;
import aurum.aurum.client.model.SoreModel;
import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static aurum.aurum.Aurum.MODID;

public class SoreRenderer extends MobRenderer<SoreBossEntity, SoreModel<SoreBossEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entities/sore.png");

    public SoreRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoreModel<>(ctx.bakeLayer(ModModelLayers.SORE_LAYER)), 0.5f);
    }

    @Override
    public void render(SoreBossEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Efectos visuales según la fase
        if (entity.getCurrentPhase() == 1) {
            poseStack.scale(1.2f, 1.2f, 1.2f); // Aumentar tamaño en fase final
        }

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoreBossEntity entity) {
        return TEXTURE;
    }
}
