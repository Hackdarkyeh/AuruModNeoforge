package aurum.aurum.client.gui.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "aurum", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class InventoryTabHandler {

    // ── 1. INVENTARIO VANILLA ──────────────────────────────────────────────────
    @SubscribeEvent
    public static void onGuiInitVanilla(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen gui)) return;

        // Restaurar posición si venimos de cambiar de pestaña
        if (TabMouseHelper.hasSavedPosition()) {
            TabMouseHelper.restorePosition();
        }

        int left = gui.getGuiLeft();
        int top  = gui.getGuiTop();

        SoulTabButton soulTab = new SoulTabButton(
                left - 26, top + 8, false,
                () -> {
                    TabMouseHelper.savePosition();
                    PacketDistributor.sendToServer(new OpenSoulMenuPacket());
                }
        );

        ArmorTabButton armorTab = new ArmorTabButton(
                left - 26, top + 37, false,
                () -> {
                    TabMouseHelper.savePosition();
                    PacketDistributor.sendToServer(new OpenArmorMenuPacket());
                }
        );

        event.addListener(soulTab);
        event.addListener(armorTab);
    }

    // ── 2. PANTALLA DEL ALMA ───────────────────────────────────────────────────
    @SubscribeEvent
    public static void onGuiInitSoulScreen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SoulInventoryPlayerScreen gui)) return;

        if (TabMouseHelper.hasSavedPosition()) {
            TabMouseHelper.restorePosition();
        }

        int left = gui.getGuiLeft();
        int top  = gui.getGuiTop();

        SoulTabButton soulTab = new SoulTabButton(
                left - 26, top + 8, true,
                () -> {
                    TabMouseHelper.savePosition();
                    Minecraft.getInstance().setScreen(new InventoryScreen(gui.getMinecraft().player));
                }
        );

        ArmorTabButton armorTab = new ArmorTabButton(
                left - 26, top + 37, false,
                () -> {
                    TabMouseHelper.savePosition();
                    PacketDistributor.sendToServer(new OpenArmorMenuPacket());
                }
        );

        event.addListener(soulTab);
        event.addListener(armorTab);
    }

    // ── 3. PANTALLA DE ARMADURA ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onGuiInitArmorScreen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ArmorControlScreen gui)) return;

        if (TabMouseHelper.hasSavedPosition()) {
            TabMouseHelper.restorePosition();
        }

        int left = gui.getGuiLeft();
        int top  = gui.getGuiTop();

        SoulTabButton soulTab = new SoulTabButton(
                left - 26, top + 8, false,
                () -> {
                    TabMouseHelper.savePosition();
                    PacketDistributor.sendToServer(new OpenSoulMenuPacket());
                }
        );

        ArmorTabButton armorTab = new ArmorTabButton(
                left - 26, top + 37, true,
                () -> {
                    TabMouseHelper.savePosition();
                    Minecraft.getInstance().setScreen(new InventoryScreen(gui.getMinecraft().player));
                }
        );

        event.addListener(soulTab);
        event.addListener(armorTab);
    }
}