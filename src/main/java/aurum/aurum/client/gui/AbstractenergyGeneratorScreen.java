package aurum.aurum.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractenergyGeneratorScreen <T extends AbstractEnergyGeneratorMenu> extends AbstractContainerScreen<T> {
    //public final AbstractFurnaceRecipeBookComponent recipeBookComponent;
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    private final ResourceLocation litProgressSprite;
    private final ResourceLocation burnProgressSprite;
    private final ResourceLocation energyProgressSprite;

    public AbstractenergyGeneratorScreen(
            T pMenu,
            //AbstractFurnaceRecipeBookComponent pRecipeBookComponent,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture,
            ResourceLocation pListProgressSprite,
            ResourceLocation pBurnProgressSprite, ResourceLocation energyProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        //this.recipeBookComponent = pRecipeBookComponent;
        this.texture = pTexture;
        this.litProgressSprite = pListProgressSprite;
        this.burnProgressSprite = pBurnProgressSprite;
        this.energyProgressSprite = energyProgressSprite;
    }

    @Override
    public void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        // this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        //this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        /*
        this.addRenderableWidget(new ImageButton(this.leftPos + 20, this.height / 2 - 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, p_313431_ -> {
           // this.recipeBookComponent.toggleVisibility();
           // this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            p_313431_.setPosition(this.leftPos + 20, this.height / 2 - 49);
        }));*/
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
            pGuiGraphics.blitSprite(this.litProgressSprite, 14, 14, 0, 14 - l, i + 56, j + 36 + 14 - l, 14, l);
        }

        int i1 = 24;
        int j1 = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        pGuiGraphics.blitSprite(this.burnProgressSprite, 24, 16, 0, 0, i + 79, j + 34, j1, 16);

        // Dibujar barra de energía en el lado izquierdo (crece de abajo hacia arriba)
        int energyBarHeight = 48; // Altura máxima de la barra en píxeles
        int energyBarWidth = 48;  // Ancho de la barra en píxeles

        int energyProgress = Mth.ceil(this.menu.getEnergyProgress() * energyBarHeight); // Calcular progreso en píxeles
        pGuiGraphics.blitSprite(
                this.energyProgressSprite,          // Sprite de la barra de energía
                energyBarWidth, energyBarHeight,    // Dimensiones completas del sprite
                0, energyBarHeight - energyProgress, // Coordenadas para dibujar desde abajo hacia arriba
                i + 3, j + 21 + (energyBarHeight - energyProgress), // Posición en la GUI
                energyBarWidth, energyProgress       // Dimensiones visibles (ancho fijo, altura variable)
        );

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título o etiqueta de la GUI
        //guiGraphics.drawString(this.font, Component.translatable("gui.asherah.generador.label_energy"), 8, 19, -12829636, false);

        // Obtener energía actual y máxima desde el menú
        int currentEnergy = this.menu.getCurrenEnergy(); // Índice de energía actual
        int maxEnergy = this.menu.getMaxEnergy();     // Índice de capacidad máxima

        // Mostrar [energía actual] / [capacidad máxima]
        String energyText = currentEnergy + "/" + maxEnergy;
        guiGraphics.drawString(this.font, energyText, 8, 15, -12829636, false); // Posición y color ajustables
    }




    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        //this.recipeBookComponent.slotClicked(pSlot);
    }
}
