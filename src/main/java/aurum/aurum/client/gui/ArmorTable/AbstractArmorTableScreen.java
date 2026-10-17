package aurum.aurum.client.gui.ArmorTable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractArmorTableScreen<T extends AbstractArmorTableMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;
    private final ResourceLocation energyProgressSprite;

    public AbstractArmorTableScreen(
            T pMenu, Inventory pPlayerInventory, Component pTitle,
            ResourceLocation pTexture, ResourceLocation pListProgressSprite,
            ResourceLocation energyProgressSprite, ResourceLocation durabilityProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = pTexture;
        this.energyProgressSprite = energyProgressSprite;
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;

        // Fondo
        pGuiGraphics.blit(this.texture, i, j, 0, 0, this.imageWidth, this.imageHeight);

        // Barra de energía
        int energyBarHeight = 48;
        int energyBarWidth = 48;
        int energyProgress = Mth.ceil(this.menu.getEnergyProgress() * energyBarHeight);
        pGuiGraphics.blitSprite(
                this.energyProgressSprite,
                energyBarWidth, energyBarHeight,
                0, energyBarHeight - energyProgress,
                i + 124, j + 38 + (energyBarHeight - energyProgress),
                energyBarWidth, energyProgress
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float currentEnergy = this.menu.getCurrenEnergy();
        float maxEnergy = this.menu.getMaxEnergy();

        guiGraphics.drawString(this.font, formatEnergyValue(maxEnergy, "ED"), 15, 18, -12829636, false);
        guiGraphics.drawString(this.font, formatEnergyValue(currentEnergy, "ED"), 17, 65, -12829636, false);

        if (this.menu.hasInsufficientExp()) {
            Component message = Component.translatable("aurum.aurum.insufficient_exp");
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
            guiGraphics.drawString(this.font, message, (int) (2 / 0.75f), (int) (5 / 0.75f), 0xFF5555, false);
            guiGraphics.pose().popPose();
        }
    }

    private String formatEnergyValue(float value, String suffix) {
        if (value >= 1_000_000) return String.format("%.1fM %s", value / 1_000_000.0f, suffix);
        else if (value >= 1_000) return String.format("%.1fk %s", value / 1_000.0f, suffix);
        else return String.format("%.0f %s", value, suffix);
    }
}