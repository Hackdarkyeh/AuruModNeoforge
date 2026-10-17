package aurum.aurum.entity;

import aurum.aurum.init.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public interface ICorruptedMob {
    LivingEntity self();

    default double getAurumValue() {
        AttributeInstance inst = self().getAttribute(ModAttributes.AURUM_IN_BLOOD);
        return inst != null ? inst.getValue() : 1.0D;
    }
}
