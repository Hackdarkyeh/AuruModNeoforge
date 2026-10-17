package aurum.aurum.client.renderer;

import aurum.aurum.client.model.AAModModelLayers;
import aurum.aurum.client.model.ZombieAlteredModel;
import aurum.aurum.entity.ZombieAltered.ZombieAlteredEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static aurum.aurum.Aurum.MODID;

public class ZombieAlteredRender extends MobRenderer<ZombieAlteredEntity, ZombieAlteredModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entities/zombie_corrupted.png");

    public ZombieAlteredRender(EntityRendererProvider.Context ctx) {
        super(ctx, new ZombieAlteredModel(ctx.bakeLayer(AAModModelLayers.ZOMBIE_ALTERED_LAYER)), 0.5f);
    }

    @Override
    public void render(ZombieAlteredEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Efectos visuales según la fase
        //if (entity.getCurrentPhase() == 1) {
        //    poseStack.scale(1.2f, 1.2f, 1.2f); // Aumentar tamaño en fase final
        //}

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieAlteredEntity entity) {
        return TEXTURE;
    }
}

