package aurum.aurum.block.engineering.ExtractorBlock;


import aurum.aurum.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

public class ExtractorBlock extends AbstractExtractorBlock {

    public static final MapCodec<ExtractorBlock> CODEC = simpleCodec(ExtractorBlock::new);


    @Override
    public MapCodec<ExtractorBlock> codec() {
        return CODEC;
    }

    public ExtractorBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13))
                .noOcclusion()); // <--- AÑADIR ESTO
    }

    public ExtractorBlock(Properties p_53627_) {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .lightLevel(litBlockEmission(13))
                .noOcclusion()); // <--- AÑADIR ESTO
    }


    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return p_50763_ -> p_50763_.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ExtractorBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createFurnaceTicker(pLevel, pBlockEntityType, ModBlockEntities.EXTRACTOR_BLOCK_ENTITY.get());
    }

    /**
     * Called to open this furnace's container.
     *
     * @see #// AbstractFurnaceBlockEntity#createMenu(int, net.minecraft.world.entity.player.Inventory, net.minecraft.world.entity.player.Player)
     */
    @Override
    protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof ExtractorBlockEntity) {
            pPlayer.openMenu((MenuProvider)blockentity);
        }
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LIT)) {
            double d0 = (double)pPos.getX() + 0.5;
            double d1 = (double)pPos.getY();
            double d2 = (double)pPos.getZ() + 0.5;

            // Sonido de máquina trabajando (Beacon o Respawn Anchor)
            // Sonido de máquina trabajando (Ambiente de Respawn Anchor o Beacon)
            if (pRandom.nextDouble() < 0.1) {
                pLevel.playLocalSound(d0, d1, d2, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.2F, false);
            }

            Direction direction = pState.getValue(FACING);
            Direction.Axis direction$axis = direction.getAxis();
            double d3 = 0.52;
            double d4 = pRandom.nextDouble() * 0.6 - 0.3;
            double d5 = direction$axis == Direction.Axis.X ? (double)direction.getStepX() * 0.52 : d4;
            double d6 = pRandom.nextDouble() * 6.0 / 16.0;
            double d7 = direction$axis == Direction.Axis.Z ? (double)direction.getStepZ() * 0.52 : d4;

            // Humo de trabajo
            pLevel.addParticle(ParticleTypes.SMOKE, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);

            // Mezcla de Energía Limpia (Soul Fire) y Energía Oscura (Portal)
            if (pRandom.nextBoolean()) {
                pLevel.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
            } else {
                pLevel.addParticle(ParticleTypes.PORTAL, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.or(box(1, 12, 2, 15, 14, 14), box(4, 0, 4, 12, 2, 12), box(3, 2, 3, 13, 4, 13), box(3, 4, 3, 13, 6, 13), box(3, 6, 3, 13, 8, 13), box(3, 8, 3, 13, 10, 13), box(3, 10, 3, 13, 12, 13));
    }

}

