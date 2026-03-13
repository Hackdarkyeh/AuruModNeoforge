package aurum.aurum.client.gui.SoulModificationTableMenu;

import aurum.aurum.Aurum;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// En una subclase de AbstractContainerScreen (pantalla)
public class SoulModificationTableScreen extends AbstractSoulScreen<SoulModificationTableMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/gui/container/soul_modification_table_block2.png");

    public SoulModificationTableScreen(SoulModificationTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_TEXTURE);
    }
}


