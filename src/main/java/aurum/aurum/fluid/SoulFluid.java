package aurum.aurum.fluid;

import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModFluidTypes;
import aurum.aurum.init.ModFluids;
import aurum.aurum.init.ModItems;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class SoulFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(() -> ModFluidTypes.SOUL_TYPE.get(), () ->
            ModFluids.SOUL_FLUID.get(), () -> ModFluids.FLOWING_SOUL_FLUID.get())
            .explosionResistance(100f).bucket(() -> ModItems.SOUL_BUCKET.get()).block(() -> (LiquidBlock) ModBlocks.SOUL_FLUID_BLOCK.get());

    private SoulFluid() {
        super(PROPERTIES);
    }

    public static class Source extends SoulFluid {
        public int getAmount(FluidState state) {
            return 8;
        }

        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends SoulFluid {
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        public boolean isSource(FluidState state) {
            return false;
        }
    }
}

