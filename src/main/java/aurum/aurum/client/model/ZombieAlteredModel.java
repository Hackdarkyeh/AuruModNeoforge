package aurum.aurum.client.model;

import aurum.aurum.Aurum;
import aurum.aurum.entity.ZombieAltered.ZombieAlteredEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;



public class ZombieAlteredModel extends AurumHumanoidModel<ZombieAlteredEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "spider_corrupted_layer"), "main");

    public ZombieAlteredModel(ModelPart root) {
        super(root); // esto rellena head, body, rightArm, leftArm, rightLeg, leftLeg, hat automáticamente
    }

    @Override
    protected boolean isAggressiveCorrupted(ZombieAlteredEntity entity) {
        return true; // tu zombie corrupto siempre en pose agresiva, como querías con isAggressive() = true
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}