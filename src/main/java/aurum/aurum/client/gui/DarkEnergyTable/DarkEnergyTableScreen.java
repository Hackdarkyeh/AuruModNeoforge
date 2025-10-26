package aurum.aurum.client.gui.DarkEnergyTable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DarkEnergyTableScreen extends AbstractDarkEnergyTableScreen<DarkEnergyTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("aurum:textures/gui/container/dark_energy_table_gui.png"); // TODO: Crear esta textura
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/burn_progress");
    private static final ResourceLocation ENERGY_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath("aurum", "energy_generator/energy_progress");

    public DarkEnergyTableScreen(DarkEnergyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE, ENERGY_PROGRESS_SPRITE);
    }


}

