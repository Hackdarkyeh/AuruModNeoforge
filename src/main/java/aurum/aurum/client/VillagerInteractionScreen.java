package aurum.aurum.client;

import aurum.aurum.Red.VillagerActionPayload;
import aurum.aurum.Red.VillagerMenuStatePayload;
import aurum.aurum.entity.AurumVillager.AurumVillagerEntity;
import aurum.aurum.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Menú de interacción de un aldeano contratable (aspecto de menú de vanilla).
 * Los botones envían la acción al servidor, que la valida y devuelve el estado actualizado.
 */
public class VillagerInteractionScreen extends Screen {

    private static final int PANEL_W = 190;
    private static final int PANEL_H = 142;
    private static final int TEXT_COLOR = 0x404040;
    private static final int TICKS_PER_MINUTE = 1200;

    private VillagerMenuStatePayload state;
    private long receivedAtGameTime;

    private Button hireButton;
    private Button followButton;
    private Button dismissButton;

    public VillagerInteractionScreen(VillagerMenuStatePayload state) {
        super(Component.translatable("aurum.villager_menu.title"));
        this.state = state;
        this.receivedAtGameTime = currentGameTime();
    }

    public int entityId() {
        return state.entityId();
    }

    public void update(VillagerMenuStatePayload newState) {
        this.state = newState;
        this.receivedAtGameTime = currentGameTime();
        refreshButtons();
    }

    private static long currentGameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int buttonX = left + 12;
        int buttonW = PANEL_W - 24;

        hireButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> send(VillagerActionPayload.ACTION_HIRE))
                .bounds(buttonX, top + 64, buttonW, 20).build());
        followButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> send(VillagerActionPayload.ACTION_FOLLOW))
                .bounds(buttonX, top + 88, buttonW, 20).build());
        dismissButton = addRenderableWidget(Button.builder(Component.translatable("aurum.villager_menu.dismiss"),
                        b -> send(VillagerActionPayload.ACTION_DISMISS))
                .bounds(buttonX, top + 112, buttonW, 20).build());
        refreshButtons();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new VillagerActionPayload(state.entityId(), action));
    }

    private void refreshButtons() {
        if (hireButton == null) {
            return;
        }
        boolean mine = state.status() == VillagerMenuStatePayload.STATUS_MINE;
        boolean other = state.status() == VillagerMenuStatePayload.STATUS_OTHER;

        hireButton.setMessage(Component.translatable(mine ? "aurum.villager_menu.extend" : "aurum.villager_menu.hire"));
        hireButton.active = !other && state.canAddDay() && (state.creative() || state.coins() >= state.price());

        followButton.setMessage(Component.translatable(
                state.follow() ? "aurum.villager_menu.follow.on" : "aurum.villager_menu.follow.off"));
        followButton.active = mine;

        dismissButton.active = mine;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        // Panel con aspecto de menú de vanilla: borde negro, biselado claro/oscuro y fondo gris
        graphics.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFF000000);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFC6C6C6);
        graphics.fill(left, top, left + PANEL_W - 1, top + 1, 0xFFFFFFFF);
        graphics.fill(left, top, left + 1, top + PANEL_H - 1, 0xFFFFFFFF);
        graphics.fill(left + 1, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF555555);
        graphics.fill(left + PANEL_W - 1, top + 1, left + PANEL_W, top + PANEL_H, 0xFF555555);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int centerX = this.width / 2;

        Component profession = Component.translatable("aurum.villager_menu.profession." + professionId());
        graphics.drawString(this.font, profession, centerX - this.font.width(profession) / 2, top + 8, TEXT_COLOR, false);

        Component status = switch (state.status()) {
            case VillagerMenuStatePayload.STATUS_MINE ->
                    Component.translatable("aurum.villager_menu.status.mine", minutesLeft());
            case VillagerMenuStatePayload.STATUS_OTHER -> Component.translatable("aurum.villager_menu.status.other");
            default -> Component.translatable("aurum.villager_menu.status.none");
        };
        graphics.drawString(this.font, status, centerX - this.font.width(status) / 2, top + 24, TEXT_COLOR, false);

        // Precio de un día y yanyitos que lleva, con el icono de la moneda
        graphics.renderItem(new ItemStack(ModItems.YANYITO.get()), left + 12, top + 40);
        Component cost = Component.translatable("aurum.villager_menu.cost",
                state.creative() ? 0 : state.price(), state.coins());
        graphics.drawString(this.font, cost, left + 32, top + 44, TEXT_COLOR, false);
    }

    private String professionId() {
        AurumVillagerEntity.Profession[] values = AurumVillagerEntity.Profession.values();
        int index = state.profession();
        return (index >= 0 && index < values.length ? values[index] : AurumVillagerEntity.Profession.NINI).id();
    }

    /** Minutos que le quedan al contrato, contando lo transcurrido desde el último estado recibido. */
    private long minutesLeft() {
        long elapsed = Math.max(0L, currentGameTime() - receivedAtGameTime);
        long left = Math.max(0L, state.ticksRemaining() - elapsed);
        return (left + TICKS_PER_MINUTE - 1) / TICKS_PER_MINUTE;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
