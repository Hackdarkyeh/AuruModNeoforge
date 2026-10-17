package aurum.aurum.client.renderer;

import aurum.aurum.Aurum;
import aurum.aurum.entity.EnchantedArmor.DivineArmorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class DivineArmorRenderer extends HumanoidMobRenderer<DivineArmorEntity, HumanoidModel<DivineArmorEntity>> {
    
    // Esta será tu textura 100% invisible
    private static final ResourceLocation INVISIBLE_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/entities/divine_armor_invisible.png");

    public DivineArmorRenderer(EntityRendererProvider.Context ctx) {
        // Usamos el modelo base del Zombi, ya que tiene las animaciones bípedas perfectas para una armadura
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        
        // ¡CRUCIAL! Añadimos la capa que renderiza las armaduras sobre la entidad
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                ctx.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(DivineArmorEntity entity) {
        // Devolvemos siempre la textura transparente para que el "cuerpo" de carne no exista
        return INVISIBLE_TEXTURE;
    }

    @Override
    public void render(DivineArmorEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        // Si tiene un arma en la mano principal, le decimos al modelo que levante el brazo
        if (!entity.getMainHandItem().isEmpty()) {
            this.model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        } else {
            this.model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}