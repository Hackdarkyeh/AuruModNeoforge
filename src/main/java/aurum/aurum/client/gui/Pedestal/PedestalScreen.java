package aurum.aurum.client.gui.Pedestal;

import aurum.aurum.Aurum;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PedestalScreen extends AbstractContainerScreen<PedestalMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/gui/pedestal/pedestal_gui.png");

    public PedestalScreen(PedestalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Asegura que el fondo oscuro genérico se dibuje si es necesario en esta versión
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Dibuja el texto flotante de los ítems (tooltip) al pasar el ratón por encima
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // GuiGraphics ya gestiona internamente los shaders, el color base y el bind de la textura.
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }
}