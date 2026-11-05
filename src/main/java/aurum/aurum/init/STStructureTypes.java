package aurum.aurum.init;

import aurum.aurum.structures.structures.BridgePlatformStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class STStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, "aurum");   // usa tu constante MODID si la tienes

    public static final DeferredHolder<StructureType<?>, StructureType<BridgePlatformStructure>> BRIDGE_PLATFORM =
            STRUCTURE_TYPES.register("bridge_platform", () -> () -> BridgePlatformStructure.CODEC);
}
