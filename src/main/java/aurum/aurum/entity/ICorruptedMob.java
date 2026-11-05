package aurum.aurum.entity;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public interface ICorruptedMob {
    ResourceLocation AURUM_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_health_bonus");
    ResourceLocation AURUM_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_damage_bonus");

    LivingEntity self();

    default double getAurumValue() {
        AttributeInstance inst = self().getAttribute(ModAttributes.AURUM_IN_BLOOD);
        return inst != null ? inst.getValue() : 1.0D;
    }

    /** Bonus de vida base por punto de aurum sobre 1. Sobrescríbelo si la entidad escala distinto. */
    default double healthPerAurum() {
        return 0.25D;
    }

    /** Bonus de daño base por punto de aurum sobre 1. Sobrescríbelo si la entidad escala distinto. */
    default double damagePerAurum() {
        return 0.15D;
    }

    /** Fija el aurum en sangre y recalcula los bonus. Úsalo desde raids, comandos, etc. */
    default void applyAurumBlood(float aurum) {
        LivingEntity e = self();
        AttributeInstance blood = e.getAttribute(ModAttributes.AURUM_IN_BLOOD);
        if (blood == null) return;
        blood.setBaseValue(aurum);
        applyAurumModifiers(aurum);
        e.setHealth(e.getMaxHealth());
    }

    /** Sorteo normal (spawn natural): 1–100. */
    default void rollAurumBlood() {
        applyAurumBlood(1.0F + self().getRandom().nextFloat() * 99.0F);
    }

    /** Sorteo con rango propio, p. ej. raids: rollAurumBlood(5, 25). */
    default void rollAurumBlood(float min, float max) {
        applyAurumBlood(min + self().getRandom().nextFloat() * (max - min));
    }

    default void applyAurumModifiers(double aurum) {
        LivingEntity e = self();
        AttributeInstance hp = e.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.addOrReplacePermanentModifier(new AttributeModifier(
                    AURUM_HEALTH_ID, (aurum - 1.0) * healthPerAurum(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance dmg = e.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.addOrReplacePermanentModifier(new AttributeModifier(
                    AURUM_DAMAGE_ID, (aurum - 1.0) * damagePerAurum(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}