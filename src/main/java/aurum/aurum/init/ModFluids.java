package aurum.aurum.init;

import aurum.aurum.Aurum;
import aurum.aurum.fluid.PlagaAurumFluid;
import aurum.aurum.fluid.SoulFluid;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFluids {
    public static final DeferredRegister<Fluid> REGISTRY_FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Aurum.MODID);

    public static final DeferredHolder<Fluid, FlowingFluid> AURUM_ROSA = REGISTRY_FLUIDS.register("plague_aurum_fluid", PlagaAurumFluid.Source::new);
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_AURUMROSA = REGISTRY_FLUIDS.register("flowing_plague_aurum_fluid", PlagaAurumFluid.Flowing::new);

    public static final DeferredHolder<Fluid, FlowingFluid> SOUL_FLUID = REGISTRY_FLUIDS.register("soul_fluid", SoulFluid.Source::new);
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SOUL_FLUID = REGISTRY_FLUIDS.register("flowing_soul_fluid", SoulFluid.Flowing::new);

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class FluidsClientSideHandler {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ItemBlockRenderTypes.setRenderLayer(AURUM_ROSA.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(FLOWING_AURUMROSA.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(SOUL_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FLOWING_SOUL_FLUID.get(), RenderType.translucent());
        }
    }
}
