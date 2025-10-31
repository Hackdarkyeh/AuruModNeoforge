package aurum.aurum.client.gui.ExtractorBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// En una subclase de AbstractContainerScreen (pantalla)
public class ExtractorScreen extends AbstractExtractorScreen<ExtractorMenu> {
    private static final ResourceLocation EXTRACTOR_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "extractor/extractor_progress");
    private static final ResourceLocation ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/energy_progress");
    private static final ResourceLocation DARK_ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "extractor/dark_energy_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/container/extractor.png");

    public ExtractorScreen(ExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, EXTRACTOR_PROGRESS_SPRITE, ENERGY_PROGRESS_SPRITE, DARK_ENERGY_PROGRESS_SPRITE);
    }
}


