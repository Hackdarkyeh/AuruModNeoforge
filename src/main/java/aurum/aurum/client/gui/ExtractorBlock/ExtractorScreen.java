package aurum.aurum.client.gui.ExtractorBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// En una subclase de AbstractContainerScreen (pantalla)
public class ExtractorScreen extends AbstractExtractorScreen<ExtractorMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/burn_progress");
    private static final ResourceLocation ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/energy_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/container/energy_generator.png");

    public ExtractorScreen(ExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE, ENERGY_PROGRESS_SPRITE);
    }
}


