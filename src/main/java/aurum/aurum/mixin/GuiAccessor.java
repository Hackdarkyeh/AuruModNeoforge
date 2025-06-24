package aurum.aurum.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Invoker("renderArmor")
    void invokeRenderArmor(GuiGraphics guiGraphics, Player player, int y, int something, int somethingElse, int l);
}
