package aurum.aurum.client.gui.ArmorTable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractArmorTableScreen<T extends AbstractArmorTableMenu> extends AbstractContainerScreen<T> {
    //public final AbstractFurnaceRecipeBookComponent recipeBookComponent;
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    private final ResourceLocation extractorProgressSprite;
    private final ResourceLocation energyProgressSprite;
    private final ResourceLocation durabilityProgressSprite;

    public AbstractArmorTableScreen(
            T pMenu,
            //AbstractFurnaceRecipeBookComponent pRecipeBookComponent,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture,
            ResourceLocation pListProgressSprite,
            ResourceLocation energyProgressSprite,
            ResourceLocation durabilityProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        //this.recipeBookComponent = pRecipeBookComponent;
        this.texture = pTexture;
        this.extractorProgressSprite = pListProgressSprite;
        this.energyProgressSprite = energyProgressSprite;
        this.durabilityProgressSprite = durabilityProgressSprite;
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

        /*
        if (this.menu.isLit()) {
            int progressBarWidth = 16;  // Ancho de la barra de progreso del extractor
            int progressBarHeight = 16; // Altura de la barra de progreso del extractor

            // Calcular el progreso en píxeles (de 0 a 13, más 1 extra para alineación)
            int extractorProgress = Mth.ceil(this.menu.getExtractorProgress() * progressBarHeight);

            // Dibujar la barra de progreso desde arriba hacia abajo
            pGuiGraphics.blitSprite(
                    this.extractorProgressSprite,  // Sprite de la barra de progreso del extractor
                    progressBarWidth, progressBarHeight, // Dimensiones visibles (ancho fijo, altura variable)
                    0, 0 , // Coordenadas de inicio del sprite (desde arriba)
                    i + 80, j + 30, // Posición en la GUI
                    progressBarWidth, extractorProgress // Dimensiones visibles (ancho fijo, altura creciente hacia abajo)
            );
        }
        */



        // Dibujar barra de energía en el lado izquierdo (crece de abajo hacia arriba)
        int energyBarHeight = 48; // Altura máxima de la barra en píxeles
        int energyBarWidth = 48;  // Ancho de la barra en píxeles
        System.out.println("Energia: " + this.menu.getEnergyProgress());
        int energyProgress = Mth.ceil(this.menu.getEnergyProgress() * energyBarHeight); // Calcular progreso en píxeles
        pGuiGraphics.blitSprite(
                this.energyProgressSprite,          // Sprite de la barra de energía
                energyBarWidth, energyBarHeight,    // Dimensiones completas del sprite
                0, energyBarHeight - energyProgress, // Coordenadas para dibujar desde abajo hacia arriba
                i + 124, j + 38 + (energyBarHeight - energyProgress), // Posición en la GUI
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
        String energyText = formatEnergyValue(maxEnergy);
        guiGraphics.drawString(this.font, energyText, 15, 18, -12829636, false); // Posición y color ajustables

        String energyText2 = formatEnergyValue(currentEnergy);
        guiGraphics.drawString(this.font, energyText2, 17, 65, -12829636, false); // Posición y color ajustables

        if (this.menu.hasInsufficientExp()) {
            Component message = Component.translatable("aurum.aurum.insufficient_exp");

            // Guardar la transformación original
            guiGraphics.pose().pushPose();

            // Aplicar escala (0.75f reduce el tamaño un 25%)
            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);

            // Dibujar el texto en una posición ajustada por la escala
            guiGraphics.drawString(this.font, message, (int) (2 / 0.75f), (int) (5 / 0.75f), 0xFF5555, false);

            // Restaurar la transformación original
            guiGraphics.pose().popPose();
        }



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
    }
}
