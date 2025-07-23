package aurum.aurum.init;

import aurum.aurum.Aurum;
import aurum.aurum.fluid.types.PlagaAurumFluidType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluidTypes {
    public static final DeferredRegister<FluidType> REGISTRY_FLUID_TYPE = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Aurum.MODID);

    public static final DeferredHolder<FluidType, FluidType> AURUMROSA_TYPE = REGISTRY_FLUID_TYPE.register("plague_aurum_fluid_type", () -> new PlagaAurumFluidType());

}
