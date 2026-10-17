package aurum.aurum.block.engineering.PortalAurum.AurumPortalInside;

import aurum.aurum.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AurumPortalBlockEntity extends TheEndPortalBlockEntity {

    public AurumPortalBlockEntity(BlockPos pos, BlockState state) {
        // Asegúrate de registrar AURUM_PORTAL_BLOCK_ENTITY en tu ModBlockEntities
        super(ModBlockEntities.AURUM_PORTAL_BLOCK_ENTITY.get(), pos, state);
    }
}