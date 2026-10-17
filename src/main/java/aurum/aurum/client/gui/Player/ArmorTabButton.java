package aurum.aurum.client.gui.Player;

import aurum.aurum.init.ModItems; // Asegúrate de que esto apunta a tus ítems
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ArmorTabButton extends AbstractWidget {

    // Puedes reutilizar las texturas de la pestaña del alma si es un fondo genérico, 
    // o crear unas nuevas llamadas armor_tab.png si quieres que tengan otro color.
    // Aquí he puesto "soul_tab.png" asumiendo que es solo la forma del cartón de la pestaña.
    private static final ResourceLocation TAB_NORMAL =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/inventory_screen/soul_tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/inventory_screen/soul_tab_selected.png");

    // Icono que aparece dentro de la pestaña (He puesto tu pechera de Aurelite)
    private static final ItemStack ICON = new ItemStack(ModItems.AURELITE_CHESTPLATE.get());

    private final Runnable onPress;
    private boolean active;

    public ArmorTabButton(int x, int y, boolean active, Runnable onPress) {
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

        // Dibuja el fondo de la pestaña
        guiGraphics.blit(tex, getX(), getY(), 0, 0, getWidth(), getHeight(), 26, 32);

        // Renderiza el icono de la armadura centrado en la pestaña
        guiGraphics.renderItem(ICON, getX() + 5, getY() + 8);

        // Tooltip al pasar el ratón por encima
        if (isHovered()) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("container.aurum.armor_tab"), // ¡Añade esto a tu archivo lang en/es.json!
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