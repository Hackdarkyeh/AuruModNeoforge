package aurum.aurum.client.gui.ArmorTable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractArmorTableScreen<T extends AbstractArmorTableMenu> extends AbstractContainerScreen<T> {
    private static final int SPRITE_SIZE = 48;
    private static final int CONTENT_Y = 6;   // primera fila opaca del sprite
    private static final int CONTENT_H = 36;  // alto útil del sprite

    private final ResourceLocation texture;
    private final ResourceLocation energySprite;
    private final ResourceLocation darkEnergySprite;

    public AbstractArmorTableScreen(T pMenu, Inventory pInv, Component pTitle,
                                    ResourceLocation texture,
                                    ResourceLocation energySprite,
                                    ResourceLocation darkEnergySprite) {
        super(pMenu, pInv, pTitle);
        this.texture = texture;
        this.energySprite = energySprite;
        this.darkEnergySprite = darkEnergySprite;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);      // ya llama a renderBackground
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int i = this.leftPos, j = this.topPos;
        g.blit(this.texture, i, j, 0, 0, this.imageWidth, this.imageHeight);

        drawBar(g, energySprite,     i + 124, j + 38, menu.getEnergyProgress());      // marco derecho
        drawBar(g, darkEnergySprite, i + 62,  j + 38, menu.getDarkEnergyProgress());  // marco izquierdo
    }

    private void drawBar(GuiGraphics g, ResourceLocation sprite, int x, int y, float progress) {
        int filled = Mth.ceil(Mth.clamp(progress, 0f, 1f) * CONTENT_H);
        if (filled <= 0) return;
        int v = CONTENT_Y + (CONTENT_H - filled);
        g.blitSprite(sprite, SPRITE_SIZE, SPRITE_SIZE, 0, v, x, y + v, SPRITE_SIZE, filled);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        super.renderTooltip(g, mx, my);
        if (isHovering(136 - 0, 44, 24, 36, mx, my)) {
            g.renderTooltip(this.font, Component.literal(
                    fmt(menu.getCurrenEnergy()) + " / " + fmt(menu.getMaxEnergy()) + " EC"), mx, my);
        } else if (isHovering(74, 44, 24, 36, mx, my)) {
            g.renderTooltip(this.font, Component.literal(
                    fmt(menu.getCurrenDarkEnergy()) + " / " + fmt(menu.getMaxDarkEnergy()) + " ED"), mx, my);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my); // título e "Inventario"
        if (menu.hasInsufficientExp()) { /* tu mensaje actual */ }
    }

    private static String fmt(float v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000);
        return String.format("%.0f", v);
    }
}