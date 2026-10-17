package aurum.aurum.client.gui.Player;

import aurum.aurum.init.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SoulTabButton extends AbstractWidget {

    // Textura de las pestañas del inventario creativo — la reutilizamos
    private static final ResourceLocation TAB_NORMAL =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/inventory_screen/soul_tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/inventory_screen/soul_tab_selected.png");

    // Icono que aparece dentro de la pestaña
    private static final ItemStack ICON = new ItemStack(ModItems.EXPANSION_SOUL_TOTEM_1.get());

    private final Runnable onPress;

    // true = esta pestaña está actualmente activa (la pantalla de alma está abierta)
    private boolean active;

    public SoulTabButton(int x, int y, boolean active, Runnable onPress) {
        // 26x32 es el tamaño estándar de una pestaña lateral del inventario creativo
        super(x, y, 26, 32, Component.empty());
        this.active = active;
        this.onPress = onPress;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation tex = active ? TAB_SELECTED : TAB_NORMAL;

        // Las pestañas laterales izquierdas del inventario creativo están en:
        // U=0, V=0  → pestaña inactiva izquierda  (26x32)
        // U=0, V=32 → pestaña activa izquierda    (26x32)
        //int vOffset = active ? 32 : 0;

        guiGraphics.blit(tex, getX(), getY(), 0, 0, getWidth(), getHeight(), 26, 32);

        // Renderizar el icono del alma centrado en la pestaña
        guiGraphics.renderItem(ICON, getX() + 5, getY() + 8);

        // Tooltip al hacer hover
        if (isHovered()) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("container.aurum.soul_tab"),
                    mouseX, mouseY
            );
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
