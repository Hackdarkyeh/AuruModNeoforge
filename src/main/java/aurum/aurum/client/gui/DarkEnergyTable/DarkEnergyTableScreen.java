package aurum.aurum.client.gui.DarkEnergyTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DarkEnergyTableScreen extends AbstractDarkEnergyTableScreen<DarkEnergyTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("aurum:textures/gui/container/dark_energy_table_gui.png"); // TODO: Crear esta textura
    private static final ResourceLocation DARK_ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "extractor/dark_energy_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/burn_progress");
    private static final ResourceLocation ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/energy_progress");
    private static final ResourceLocation CLEAN_ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "dark_energy_table/clean_energy_progress");


    public DarkEnergyTableScreen(DarkEnergyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, TEXTURE, DARK_ENERGY_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE, ENERGY_PROGRESS_SPRITE, CLEAN_ENERGY_PROGRESS_SPRITE);
    }


}

