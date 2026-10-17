package aurum.aurum.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;

public abstract class AurumHumanoidModel<T extends Mob> extends HumanoidModel<T> {

    protected AurumHumanoidModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Aquí heredas TODA la animación humanoide estándar (brazos, piernas, cabeza, ataque)
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Punto de extensión común para tus mobs corruptos:
        // por ejemplo, un balanceo de brazos "agresivo" tipo zombie, aplicable a cualquier subtipo
        if (isAggressiveCorrupted(entity)) {
            applyAggressiveArmSwing();
        }
    }

    protected boolean isAggressiveCorrupted(T entity) {
        return false; // override en subclases si aplica
    }

    protected void applyAggressiveArmSwing() {
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        // aquí replicas AnimationUtils.animateZombieArms si quieres ese estilo por defecto,
        // o defines tu propio "tic" visual de corrupción (temblor, brazos rígidos, etc.)
    }
}
