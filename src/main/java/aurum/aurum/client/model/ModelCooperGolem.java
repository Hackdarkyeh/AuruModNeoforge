package aurum.aurum.client.model;

// Made with Blockbench 4.10.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import aurum.aurum.Aurum;
import aurum.aurum.client.animations.ModAnimationDefinitions;
import aurum.aurum.entity.CooperGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ModelCooperGolem<T extends Entity> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	private final ModelPart golemcooper;
	private final ModelPart cabeza;
	private final ModelPart antena;
	private final ModelPart cuerpo;
	private final ModelPart brazos;
	private final ModelPart brazoderecho;
	private final ModelPart brazoizquierdo;
	private final ModelPart piernas;
	private final ModelPart piernaizquierda;
	private final ModelPart piernaderecha;

	public ModelCooperGolem(ModelPart root) {
		this.golemcooper = root.getChild("golemcooper");
		this.cabeza = golemcooper.getChild("cabeza");
		this.antena = cabeza.getChild("antena");
		this.cuerpo = golemcooper.getChild("cuerpo");
		this.brazos = golemcooper.getChild("brazos");
		this.brazoderecho = brazos.getChild("brazoderecho");
		this.brazoizquierdo = brazos.getChild("brazoizquierdo");
		this.piernas = golemcooper.getChild("piernas");
		this.piernaizquierda = piernas.getChild("piernaizquierda");
		this.piernaderecha = piernas.getChild("piernaderecha");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition golemcooper = partdefinition.addOrReplaceChild("golemcooper", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cabeza = golemcooper.addOrReplaceChild("cabeza", CubeListBuilder.create().texOffs(29, 0).addBox(-1.0F, -3.5F, 0.125F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(28, 28).addBox(-1.5F, 2.5F, -3.875F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.5F, -0.5F, -2.875F, 11.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -15.5F, 0.875F));

		PartDefinition antena = cabeza.addOrReplaceChild("antena", CubeListBuilder.create().texOffs(12, 24).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.5F, 1.125F));

		PartDefinition cuerpo = golemcooper.addOrReplaceChild("cuerpo", CubeListBuilder.create().texOffs(32, 13).addBox(-1.0F, -10.0F, 4.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 13).addBox(-5.0F, -10.0F, -1.0F, 11.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition brazos = golemcooper.addOrReplaceChild("brazos", CubeListBuilder.create(), PartPose.offset(7.5F, -10.0F, 1.5F));

		PartDefinition brazoderecho = brazos.addOrReplaceChild("brazoderecho", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-14.0F, 0.0F, 0.0F));

		PartDefinition brazoizquierdo = brazos.addOrReplaceChild("brazoizquierdo", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition piernas = golemcooper.addOrReplaceChild("piernas", CubeListBuilder.create(), PartPose.offset(0.5F, -2.0F, 1.5F));

		PartDefinition piernaizquierda = piernas.addOrReplaceChild("piernaizquierda", CubeListBuilder.create().texOffs(28, 20).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -1.0F, 0.0F));

		PartDefinition piernaderecha = piernas.addOrReplaceChild("piernaderecha", CubeListBuilder.create().texOffs(28, 20).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -1.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int nose) {
		golemcooper.render(poseStack, vertexConsumer, packedLight, packedOverlay);
	}

	@Override
	public ModelPart root() {
		return golemcooper;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

		this.animateWalk(ModAnimationDefinitions.walk, limbSwing, limbSwingAmount, 2f, 2.5f);
		this.animate(((CooperGolemEntity) entity).idleAnimationState, ModAnimationDefinitions.idle, ageInTicks, 1f);
		this.animate(((CooperGolemEntity) entity).attackAnimationState, ModAnimationDefinitions.attack, ageInTicks, 1f);
		this.animate(((CooperGolemEntity) entity).channelraysAnimationState, ModAnimationDefinitions.channelrays, ageInTicks, 1f);
	}

	private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks){
		pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30, 30);
		pHeadPitch = Mth.clamp(pHeadPitch, -25, 45);
		this.cabeza.xRot = pHeadPitch * ((float)Math.PI / 180F);
		this.cabeza.yRot = pNetHeadYaw * ((float)Math.PI / 180F);
	}





}