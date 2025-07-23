package aurum.aurum.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HideHealthBarMixin {

    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true)
    private void cancelRenderHealthLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel(); // Cancela completamente el método para evitar que la barra de vida se procese
    }


        @Inject(method = "renderArmorLevel", at = @At("HEAD"), cancellable = true)
        private void modifyArmorBarPosition(GuiGraphics guiGraphics, CallbackInfo ci) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            int l = guiGraphics.guiWidth() / 2 - 91;
            Minecraft.getInstance().getProfiler().push("armor");
            Gui guiInstance = (Gui) (Object) this;

            // Subimos la barra de armadura 10 píxeles más arriba
            int newY = guiGraphics.guiHeight() - guiInstance.leftHeight;

            // Llamar al método original con la nueva posición
            ((GuiAccessor) guiInstance).invokeRenderArmor(guiGraphics, player, newY, 1, 0, l);

            Minecraft.getInstance().getProfiler().pop();

            if (player.getArmorValue() > 0) {
                guiInstance.leftHeight += 10; // Mantiene la alineación del resto de elementos
            }

            ci.cancel(); // Cancela el método original para evitar que la armadura se dibuje en la posición incorrecta
        }


}

