package aurum.aurum.client.gui.ExtractorBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractExtractorScreen<T extends AbstractExtractorMenu> extends AbstractContainerScreen<T> {
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    private final ResourceLocation extractorProgressSprite;
    private final ResourceLocation energyProgressSprite;
    private final ResourceLocation darkEnergyProgressSprite;

    public AbstractExtractorScreen(
            T pMenu,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture,
            ResourceLocation pListProgressSprite,
            ResourceLocation energyProgressSprite,
            ResourceLocation darkEnergyProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = pTexture;
        this.extractorProgressSprite = pListProgressSprite;
        this.energyProgressSprite = energyProgressSprite;
        this.darkEnergyProgressSprite = darkEnergyProgressSprite;
    }

    @Override
    public void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        pGuiGraphics.blit(this.texture, i, j, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.isLit()) {
            int progressBarWidth = 16;
            int progressBarHeight = 16;

            int extractorProgress = Mth.ceil(this.menu.getExtractorProgress() * progressBarHeight);

            pGuiGraphics.blitSprite(
                    this.extractorProgressSprite,
                    progressBarWidth, progressBarHeight,
                    0, 0,
                    i + 80, j + 30,
                    progressBarWidth, extractorProgress
            );
        }

        // --- BARRA DE ENERGÍA LIMPIA (Izquierda) ---
        int energyBarHeight = 48;
        int energyBarWidth = 48; // Ancho habitual de la barra en la textura (ajusta si es distinto, ej: 16 o 48)

        int energyProgress = Mth.ceil(this.menu.getEnergyProgress() * energyBarHeight);
        pGuiGraphics.blitSprite(
                this.energyProgressSprite,
                energyBarWidth, energyBarHeight,
                0, energyBarHeight - energyProgress,
                i + 3, j + 21 + (energyBarHeight - energyProgress), // Coordenada X del tanque izquierdo (ej: i + 9)
                energyBarWidth, energyProgress
        );

        // --- BARRA DE ENERGÍA OSCURA (Derecha) ---
        int darkEnergyBarHeight = 48;
        int darkEnergyBarWidth = 48; // Ancho habitual de la barra (ej: 16)



        float currentDarkEnergy = this.menu.getCurrenDarkEnergy();
        float maxDarkEnergy = this.menu.getMaxDarkEnergy();
        float darkRatio = maxDarkEnergy > 0 ? Math.min(currentDarkEnergy / maxDarkEnergy, 1.0f) : 0.0f;

        int darkEnergyProgress = Mth.ceil(darkRatio * darkEnergyBarHeight);
        pGuiGraphics.blitSprite(
                this.darkEnergyProgressSprite,
                darkEnergyBarWidth, darkEnergyBarHeight,
                0, darkEnergyBarHeight - darkEnergyProgress,
                i + 125, j + 21 + (darkEnergyBarHeight - darkEnergyProgress), // Coordenada X exacta del tanque derecho (coincide con el isHovering de la derecha: 131)
                darkEnergyBarWidth, darkEnergyProgress
        );
        /*
        pGuiGraphics.blit(
                this.darkEnergyProgressSprite,
                i + 131, j + 21 + (darkEnergyBarHeight - darkEnergyProgress), // Coordenada X e Y ajustada al marco derecho
                0, darkEnergyBarHeight - darkEnergyProgress,                 // Recorte desde abajo hacia arriba
                darkEnergyBarWidth, darkEnergyProgress,                      // Ancho y alto actual
                darkEnergyBarWidth, darkEnergyBarHeight                      // Tamaño total de la textura
        )*/
    }



    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Obtener energía actual y máxima desde el menú
        float currentEnergy = this.menu.getCurrenEnergy();
        float maxEnergy = this.menu.getMaxEnergy();
        float currentDarkEnergy = this.menu.getCurrenDarkEnergy();
        float maxDarkEnergy = this.menu.getMaxDarkEnergy();

        // Mostrar valores formateados (Ej: 2,0M / 20,0k)
        String energyText = formatEnergyValue(maxEnergy);
        guiGraphics.drawString(this.font, energyText, 15, 18, -12829636, false);

        String energyText2 = formatEnergyValue(currentEnergy);
        guiGraphics.drawString(this.font, energyText2, 15, 65, -12829636, false);

        String darkMaxEnergyText = formatEnergyValue(maxDarkEnergy);
        guiGraphics.drawString(this.font, darkMaxEnergyText, 136, 18, -12829636, false);

        String darkEnergyText = formatEnergyValue(currentDarkEnergy);
        guiGraphics.drawString(this.font, darkEnergyText, 136, 65, -12829636, false);

        if (this.menu.hasInsufficientExp()) {
            Component message = Component.translatable("aurum.aurum.insufficient_exp");

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
            guiGraphics.drawString(this.font, message, (int) (2 / 0.75f), (int) (5 / 0.75f), 0xFF5555, false);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip al pasar el ratón sobre el tanque de Energía Limpia (Izquierda)
        if (this.isHovering(9, 21, 30, 48, mouseX, mouseY)) {
            Component tooltipText = Component.literal((int) this.menu.getCurrenEnergy() + " / " + (int) this.menu.getMaxEnergy() + " EC");
            guiGraphics.renderTooltip(this.font, tooltipText, mouseX, mouseY);
        }

        // Tooltip al pasar el ratón sobre el tanque de Energía Oscura (Derecha)
        if (this.isHovering(131, 21, 30, 48, mouseX, mouseY)) {
            Component tooltipText = Component.literal((int) this.menu.getCurrenDarkEnergy() + " / " + (int) this.menu.getMaxDarkEnergy() + " ED");
            guiGraphics.renderTooltip(this.font, tooltipText, mouseX, mouseY);
        }

        // Tooltip al pasar el ratón sobre el triángulo de progreso del extractor
        // (misma zona donde se dibuja extractorProgressSprite: x 80, y 30, 16x16)
        if (this.isHovering(80, 30, 16, 16, mouseX, mouseY)) {
            // El progreso va en ticks (20 = 1 s): lo mostramos en segundos, con el total y el porcentaje
            int seconds = this.menu.getExtractorTime() / 20;
            int totalSeconds = this.menu.getExtractorTotalTime() / 20;
            int percent = Mth.floor(this.menu.getExtractorProgress() * 100.0F);
            Component tooltipText = Component.translatable("aurum.aurum.extractor_progress", seconds, totalSeconds, percent);
            guiGraphics.renderTooltip(this.font, tooltipText, mouseX, mouseY);
        }
    }

    private String formatEnergyValue(float value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0f);
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0f);
        } else {
            return String.format("%.0f", value);
        }
    }

    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
    }
}