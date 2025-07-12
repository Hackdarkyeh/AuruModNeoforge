package aurum.aurum.client.model;// Made with Blockbench 4.12.5
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import aurum.aurum.client.animations.CooperGolemAnimationDefinitions;
import aurum.aurum.client.animations.SoreAnimationDefinitions;
import aurum.aurum.entity.CooperGolemEntity;
import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import static aurum.aurum.Aurum.MODID;

public class SoreModel<T extends Entity> extends HierarchicalModel<T> {
	private final ModelPart sore;
	private final ModelPart head;
	private final ModelPart rightarm;
	private final ModelPart leftarm;
	private final ModelPart leftleg;
	private final ModelPart rightleg;
	private final ModelPart body;

	public SoreModel(ModelPart root) {
		this.sore = root.getChild("sore");
		this.head = this.sore.getChild("head");
		this.rightarm = this.sore.getChild("rightarm");
		this.leftarm = this.sore.getChild("leftarm");
		this.leftleg = this.sore.getChild("leftleg");
		this.rightleg = this.sore.getChild("rightleg");
		this.body = this.sore.getChild("body");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition sore = partdefinition.addOrReplaceChild("sore", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = sore.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -28.0F, -1.0F));

		PartDefinition rightarm = sore.addOrReplaceChild("rightarm", CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -22.0F, 0.0F));

		PartDefinition leftarm = sore.addOrReplaceChild("leftarm", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -22.0F, 0.0F));

		PartDefinition leftleg = sore.addOrReplaceChild("leftleg", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -10.0F, 0.0F));

		PartDefinition rightleg = sore.addOrReplaceChild("rightleg", CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -10.0F, 0.0F));

		PartDefinition body = sore.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

		this.animateWalk(SoreAnimationDefinitions.andar, limbSwing, limbSwingAmount, 2f, 2.5f);
		this.animate(((SoreBossEntity) entity).invokeStart,SoreAnimationDefinitions.invoke_start, ageInTicks, 1f);
		this.animate(((SoreBossEntity) entity).invoke,SoreAnimationDefinitions.invoke, ageInTicks, 1f);
		this.animate(((SoreBossEntity) entity).invokeEnd,SoreAnimationDefinitions.invoke_end, ageInTicks, 1f);
	}



	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		sore.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	@Override
	public ModelPart root() {
		return sore;
	}

	private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks){
		pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30, 30);
		pHeadPitch = Mth.clamp(pHeadPitch, -25, 45);
		this.head.xRot = pHeadPitch * ((float)Math.PI / 180F);
		this.head.yRot = pNetHeadYaw * ((float)Math.PI / 180F);
	}
}