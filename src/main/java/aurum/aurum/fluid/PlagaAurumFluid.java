package aurum.aurum.fluid;

import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModFluidTypes;
import aurum.aurum.init.ModFluids;
import aurum.aurum.init.ModItems;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class PlagaAurumFluid extends BaseFlowingFluid {
    public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.AURUMROSA_TYPE,
            ModFluids.AURUM_ROSA,
            ModFluids.FLOWING_AURUMROSA)
            .explosionResistance(100f)
            .bucket(ModItems.AURUMROSA_BUCKET)
            .block(() -> (LiquidBlock) ModBlocks.PLAGUE_AURUM_BLOCK.get()

            ); // <- cambiado

    private PlagaAurumFluid() {
        super(PROPERTIES);
    }

    public static class Source extends PlagaAurumFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends PlagaAurumFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}