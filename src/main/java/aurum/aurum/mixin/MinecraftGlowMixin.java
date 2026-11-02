package aurum.aurum.mixin;

import aurum.aurum.client.LuminescenceState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pasiva Luminiscencia: fuerza el contorno brillante en las entidades que el servidor ha marcado. Va en "client". */
@Mixin(Minecraft.class)
public class MinecraftGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void aurum$luminescenceGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (LuminescenceState.contains(entity.getId())) cir.setReturnValue(true);
    }
}