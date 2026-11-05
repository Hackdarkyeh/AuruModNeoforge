package aurum.aurum.init;

import aurum.aurum.structures.structures.BridgeArmFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class STFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "aurum");

    public static final DeferredHolder<Feature<?>, BridgeArmFeature> BRIDGE_ARMS =
            FEATURES.register("bridge_arms", BridgeArmFeature::new);
}
