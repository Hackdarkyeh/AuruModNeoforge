package aurum.aurum.mixin;

import aurum.aurum.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndDragonFight.class)
public class EndPortalAutoSpawnMixin {

    @Shadow @Final private ServerLevel level;
    @Shadow private BlockPos portalLocation;

    @Inject(method = "tick", at = @At("HEAD"))
    private void injectPortalBlockOnTick(CallbackInfo ci) {
        if (this.level != null && !this.level.isClientSide()) {
            BlockPos targetPos = (this.portalLocation != null) ? this.portalLocation : new BlockPos(0, 64, 0);

            // IMPORTANTE: Solo actuamos si el bloque ES BEDROCK.
            // Si el jugador lo rompe, el bloque será Aire u otro distinto, por lo que el mixin NUNCA lo volverá a poner (evitando duplicaciones).
            if (this.level.getBlockState(targetPos).is(Blocks.BEDROCK)) {
                this.level.setBlock(targetPos, ModBlocks.INOCULATED_OBSIDIAN.get().defaultBlockState(), 3);
            }
        }
    }
}