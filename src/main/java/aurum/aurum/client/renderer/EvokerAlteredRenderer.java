package aurum.aurum.client.renderer;

import aurum.aurum.Aurum;
import aurum.aurum.entity.EvokeAltered.EvokerAlteredEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerAlteredRenderer extends IllagerRenderer<EvokerAlteredEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/entities/evoker_altered.png");

    public EvokerAlteredRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.EVOKER)), 0.5F);
        this.addLayer(
                new ItemInHandLayer<EvokerAlteredEntity, IllagerModel<EvokerAlteredEntity>>(this, context.getItemInHandRenderer()) {
                    @Override
                    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                       EvokerAlteredEntity entity, float limbSwing, float limbSwingAmount,
                                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                        if (entity.isCastingSpell()) {
                            super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                                    partialTick, ageInTicks, netHeadYaw, headPitch);
                        }
                    }
                }
        );
    }

    @Override
    public ResourceLocation getTextureLocation(EvokerAlteredEntity entity) {
        return TEXTURE;
    }
}