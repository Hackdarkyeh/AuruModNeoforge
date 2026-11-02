package aurum.aurum.mixin;

import aurum.aurum.client.LuminescenceState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pasiva Luminiscencia: color del contorno según la amenaza. Va en "client" (el servidor no lo carga). */
@Mixin(Entity.class)
public class EntityTeamColorMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void aurum$luminescenceColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() && LuminescenceState.contains(self.getId())) {
            cir.setReturnValue(LuminescenceState.color(self.getId()));
        }
    }
}