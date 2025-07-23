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

public abstract class PlagaAurumFluid extends BaseFlowingFluid {
    public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> ModFluidTypes.AURUMROSA_TYPE.get(), () ->
            ModFluids.AURUM_ROSA.get(), () -> ModFluids.FLOWING_AURUMROSA.get())
            .explosionResistance(100f).bucket(() -> ModItems.AURUMROSA_BUCKET.get()).block(() -> (LiquidBlock) ModBlocks.PLAGUE_AURUM_BLOCK.get());

    private PlagaAurumFluid() {
        super(PROPERTIES);
    }

    public static class Source extends PlagaAurumFluid {
        public int getAmount(FluidState state) {
            return 8;
        }

        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends PlagaAurumFluid {
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

