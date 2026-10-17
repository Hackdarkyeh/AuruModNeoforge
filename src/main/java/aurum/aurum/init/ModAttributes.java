package aurum.aurum.init;

import aurum.aurum.Aurum;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Aurum.MODID);

    public static final DeferredHolder<Attribute, Attribute> AURUM_IN_BLOOD =
            ATTRIBUTES.register("aurum_in_blood",
                    () -> new RangedAttribute("attribute.aurum.aurum_in_blood", 1.0D, 0.0D, 100.0D)
                            .setSyncable(true));
}