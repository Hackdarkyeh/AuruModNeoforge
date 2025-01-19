package aurum.aurum.init;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Set;

public class ModBlockEntityTypes<T extends BlockEntity> extends BlockEntityType<BlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Set<Block> validBlocks;
    private final Type<?> dataType;
    private final Holder.Reference<BlockEntityType<?>> builtInRegistryHolder = BuiltInRegistries.BLOCK_ENTITY_TYPE.createIntrusiveHolder(this);
    private final BlockEntityType.BlockEntitySupplier<? extends T> factory;


    @Nullable
    public static ResourceLocation getKey(BlockEntityType<?> pBlockEntityType) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(pBlockEntityType);
    }




    public ModBlockEntityTypes(BlockEntityType.BlockEntitySupplier<? extends T> pFactory, Set<Block> pValidBlocks, Type<?> pDataType) {
        super(pFactory, pValidBlocks, pDataType);
        this.factory = pFactory;
        this.validBlocks = pValidBlocks;
        this.dataType = pDataType;
    }

    @Nullable
    public T create(BlockPos pPos, BlockState pState) {
        return (T)this.factory.create(pPos, pState);
    }

    /**
     * Neo: Add getter for an immutable view of the set of valid blocks.
     */
    public Set<Block> getValidBlocks() {
        return java.util.Collections.unmodifiableSet(this.validBlocks);
    }

    public boolean isValid(BlockState pState) {
        return this.validBlocks.contains(pState.getBlock());
    }

    @Nullable
    public Holder.Reference<BlockEntityType<?>> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    @Nullable
    public T getBlockEntity(BlockGetter pLevel, BlockPos pPos) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        return (T)(blockentity != null && blockentity.getType() == this ? blockentity : null);
    }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pPos, BlockState pState);
    }

    public static final class Builder<T extends BlockEntity> {
        private final BlockEntityType.BlockEntitySupplier<? extends T> factory;
        public final Set<Block> validBlocks;

        private Builder(BlockEntityType.BlockEntitySupplier<? extends T> pFactory, Set<Block> pValidBlocks) {
            this.factory = pFactory;
            this.validBlocks = pValidBlocks;
        }



        public BlockEntityType<T> build(Type<?> pDataType) {
            return new BlockEntityType<>(this.factory, this.validBlocks, pDataType);
        }
    }
}

