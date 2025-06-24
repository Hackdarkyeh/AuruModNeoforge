package aurum.aurum.client.gui.ArmorTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// En una subclase de AbstractContainerScreen (pantalla)
public class ArmorTableScreen extends AbstractArmorTableScreen<ArmorTableMenu> {
    private static final ResourceLocation EXP_PROGRESS_SPRITE = ResourceLocation.
            fromNamespaceAndPath("aurum", "extractor/extractor_progress");
    private static final ResourceLocation DURABILITY_PROGRESS_SPRITE = ResourceLocation.
            fromNamespaceAndPath("aurum", "extractor/extractor_progress");
    private static final ResourceLocation ENERGY_PROGRESS_SPRITE = ResourceLocation.
            fromNamespaceAndPath("aurum", "armor_table/energy_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.
            fromNamespaceAndPath("aurum", "textures/gui/container/armor_table_block.png");

    public ArmorTableScreen(ArmorTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, EXP_PROGRESS_SPRITE, ENERGY_PROGRESS_SPRITE,DURABILITY_PROGRESS_SPRITE);
    }
}


