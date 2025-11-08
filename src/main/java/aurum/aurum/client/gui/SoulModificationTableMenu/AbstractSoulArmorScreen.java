package aurum.aurum.client.gui.SoulModificationTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSoulArmorScreen <T extends AbstractSoulArmorMenu> extends AbstractContainerScreen<T> {
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    private final ResourceLocation wightProgressSprite;
    private final ResourceLocation burnProgressSprite;
    //private final ResourceLocation energyProgressSprite;


    public AbstractSoulArmorScreen(
            T pMenu,
            //AbstractFurnaceRecipeBookComponent pRecipeBookComponent,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture,
            ResourceLocation pWightProgressSprite,
            ResourceLocation pCorrosionProgressSprite//, ResourceLocation energyProgressSprite
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        //this.recipeBookComponent = pRecipeBookComponent;
        this.texture = pTexture;
        this.wightProgressSprite =pWightProgressSprite;
        this.burnProgressSprite = pCorrosionProgressSprite;
        //this.energyProgressSprite = energyProgressSprite;
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
    public void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {

        int i = this.leftPos;
        int j = this.topPos;
        graphics.blit(this.texture, i, j, 0, 0, this.imageWidth, this.imageHeight);


    }
    /*
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detectar si el jugador hace clic en una habilidad para arrastrarla
        for (ArmorExpansions ability : ExpansionsManager.getAllExpansions()) {
            if (isInsideAbilityList(mouseX, mouseY, ability)) {
                draggingAbility = ability;
                return true;
            }
        }

        // Detectar si el jugador suelta la habilidad en una punta de la estrella
        for (int i = 0; i < 9; i++) {
            if (isInsideStarSlot(mouseX, mouseY, i) && draggingAbility != null) {
                if (soulData.equipAbility(i, draggingAbility, 100)) { // 100 es el alma máxima
                    draggingAbility = null; // Suelta la habilidad
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int[] getSlotPosition(int index) {
        int centerX = this.width / 2 - 100; // Centro de la estrella
        int centerY = this.height / 2 - 100;

        int[][] positions = {
                {centerX + 50, centerY},     // 1
                {centerX + 80, centerY + 20},// 2
                {centerX + 100, centerY + 50},// 3
                {centerX + 80, centerY + 80}, // 4
                {centerX + 50, centerY + 100},// 5
                {centerX + 20, centerY + 80}, // 6
                {centerX, centerY + 50},      // 7
                {centerX + 20, centerY + 20}, // 8
                {centerX + 50, centerY - 20}  // 9 (superior)
        };

        return positions[index];
    }

    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);

    }

}
