package aurum.aurum.client.gui.DarkEnergyTable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractDarkEnergyTableScreen<T extends AbstractDarkEnergyTableMenu> extends AbstractContainerScreen<T> {
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    private final ResourceLocation litProgressSprite;
    private final ResourceLocation burnProgressSprite;
    private final ResourceLocation energyProgressSprite;

    public AbstractDarkEnergyTableScreen(
            T pMenu,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture,
            ResourceLocation pListProgressSprite,
            ResourceLocation pBurnProgressSprite, ResourceLocation energyProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = pTexture;
        this.litProgressSprite = pListProgressSprite;
        this.burnProgressSprite = pBurnProgressSprite;
        this.energyProgressSprite = energyProgressSprite;
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
        //this.recipeBookComponent.tick();
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        pGuiGraphics.blit(this.texture, i, j, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isLit()) {
            int k = 14;
            int l = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            pGuiGraphics.blitSprite(this.litProgressSprite, 14, 14, 0, 14 - l, i + 87, j + 34 + 14 - l, 14, l);
        }

        int i1 = 24;
        int j1 = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        //pGuiGraphics.blitSprite(this.burnProgressSprite, 24, 16, 0, 0, i + 111, j + 49, j1, 16);

        // Dibujar barra de energía en el lado izquierdo (crece de abajo hacia arriba)
        int energyBarHeight = 48; // Altura máxima de la barra en píxeles
        int energyBarWidth = 48;  // Ancho de la barra en píxeles

        int energyProgress = Mth.ceil(this.menu.getEnergyProgress() * energyBarHeight); // Calcular progreso en píxeles
        pGuiGraphics.blitSprite(
                this.energyProgressSprite,          // Sprite de la barra de energía
                energyBarWidth, energyBarHeight,    // Dimensiones completas del sprite
                0, energyBarHeight - energyProgress, // Coordenadas para dibujar desde abajo hacia arriba
                i - 2 , j + 20 + (energyBarHeight - energyProgress), // Posición en la GUI
                energyBarWidth, energyProgress       // Dimensiones visibles (ancho fijo, altura variable)
        );
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título o etiqueta de la GUI
        //guiGraphics.drawString(this.font, Component.translatable("gui.asherah.generador.label_energy"), 8, 19, -12829636, false);

        // Obtener energía actual y máxima desde el menú
        float currentEnergy = this.menu.getCurrenEnergy(); // Índice de energía actual
        float maxEnergy = this.menu.getMaxEnergy();     // Índice de capacidad máxima

        // Mostrar [energía actual] / [capacidad máxima]
        String energyText = formatEnergyValue(currentEnergy) + "/" + formatEnergyValue(maxEnergy);
        guiGraphics.drawString(this.font, energyText, 8, 15, -12829636, false); // Posición y color ajustables
    }

    private String formatEnergyValue(float value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0f); // Formato con 1 decimal
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0f); // Formato con 1 decimal
        } else {
            return String.format("%.0f", value); // Número sin decimales
        }
    }





    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        //this.recipeBookComponent.slotClicked(pSlot);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(guiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(guiGraphics, pMouseX, pMouseY);

        // Tooltip para la energía oscura
        if (isHovering(10, 17, 12, 50, pMouseX, pMouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Energía Oscura: " + menu.getDarkEnergyStored() + " / " + menu.getMaxDarkEnergyStored()), pMouseX, pMouseY);
        }
        // Tooltip para la energía limpia
        if (isHovering(154, 17, 12, 50, pMouseX, pMouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Energía Limpia: " + menu.getCleanEnergyStored() + " / " + menu.getMaxCleanEnergyStored()), pMouseX, pMouseY);
        }
    }
}
