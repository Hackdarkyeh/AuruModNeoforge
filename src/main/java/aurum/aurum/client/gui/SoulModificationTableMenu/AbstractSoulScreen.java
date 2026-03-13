package aurum.aurum.client.gui.SoulModificationTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSoulScreen<T extends AbstractSoulMenu> extends AbstractContainerScreen<T> {
    private boolean widthTooNarrow;
    private final ResourceLocation texture;
    //private final ResourceLocation energyProgressSprite;


    public AbstractSoulScreen(
            T pMenu,
            //AbstractFurnaceRecipeBookComponent pRecipeBookComponent,
            Inventory pPlayerInventory,
            Component pTitle,
            ResourceLocation pTexture
    ) {
        super(pMenu, pPlayerInventory, pTitle);
        //this.recipeBookComponent = pRecipeBookComponent;
        this.texture = pTexture;
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

    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);

    }

}
