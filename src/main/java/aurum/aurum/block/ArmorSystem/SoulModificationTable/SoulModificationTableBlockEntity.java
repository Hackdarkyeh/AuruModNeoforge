package aurum.aurum.block.ArmorSystem.SoulModificationTable;

import aurum.aurum.client.gui.SoulModificationTableMenu.SoulModificationTableMenu;
import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SoulModificationTableBlockEntity extends AbstractSoulModificationTableBlockEntity{
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 9;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                // Si cambió el slot 0 (alma) sincronizar/limpiar pedestales inmediatamente
                if (slot == 0) {
                    ItemStack stack = this.getStackInSlot(0);
                    if (stack.isEmpty()) {
                        clearNearbyPedestals(level, worldPosition);
                    } else {
                        syncPedestalsFromSoul(level, worldPosition, stack);
                    }
                }
            }
        }
    };
    private float rotation;

    public SoulModificationTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOUL_MODIFICATION_TABLE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public float getRenderingRotation() {
        rotation += 0.5f;
        if (rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mesa");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.soul_modification_table_block");
    }

    @Nullable
    public @NotNull AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new SoulModificationTableMenu(pId, pPlayer, this, this.dataAccess);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}