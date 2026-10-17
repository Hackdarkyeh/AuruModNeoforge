package aurum.aurum.client.gui.Player;

import aurum.aurum.init.ModAttachments;
import aurum.aurum.Soul.SoulData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AurumBarOverlay implements LayeredDraw.Layer {
    public static boolean visible = true;

    private static final int COLOR_PURPLE = 0xFF4B0082;   // índigo oscuro
    private static final int COLOR_GOLD   = 0xFFFFD700;   // dorado
    private static final int AURUM_PER_BAR = 972;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        Minecraft mc = Minecraft.getInstance();
        // Respetar las mismas condiciones que tus corazones (ocultar en creativo/espectador)
        if (!visible || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;

        // 1. Obtener datos
        int totalAurum = mc.player.getData(ModAttachments.AURUM_BLOOD);
        int maxAurum = calculateMaxAurum(mc.player);

        // Calcular vueltas/capas de la barra
        int barrasCompletas = totalAurum / AURUM_PER_BAR;
        int aurumEnBarraActual = totalAurum % AURUM_PER_BAR;

        if (totalAurum >= maxAurum) {
            totalAurum = maxAurum;
            aurumEnBarraActual = maxAurum % AURUM_PER_BAR;
            barrasCompletas = maxAurum / AURUM_PER_BAR;
            if (aurumEnBarraActual == 0 && totalAurum > 0) {
                aurumEnBarraActual = AURUM_PER_BAR;
                barrasCompletas -= 1;
            }
        }

        float fill = (float) aurumEnBarraActual / AURUM_PER_BAR;

        // 2. POSICIONAMIENTO DINÁMICO (Compatible con el HUD central)
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Alineación en el lado derecho (Simétrico a donde empiezan tus corazones a la izquierda)
        // xStart de tus corazones es: screenWidth / 2 - 90
        // Para el lado derecho (muslitos/armadura), el HUD nativo termina en: screenWidth / 2 + 90
        // Creamos una barra compacta de 81 píxeles de ancho para que encaje estéticamente sobre la comida
        int barWidth = 81;
        int x = screenWidth / 2 + 90 - barWidth;

        // Colocar la barra justo arriba del HUD de comida/oxígeno estándar (Y = screenHeight - 48)
        int y = screenHeight - 49;

        // 3. Colores de las capas
        int colorRelleno = interpolateColor(COLOR_PURPLE, COLOR_GOLD, fill);
        int colorFondo = 0x80333333;

        if (barrasCompletas > 0) {
            colorFondo = 0xFF000000 | COLOR_GOLD;
        }

        int barHeight = 5; // Una barra de 5 píxeles de alto es más estilizada para el HUD inferior

        // Fondo de la barra
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, colorFondo);

        // Relleno de la barra
        int fillWidth = (int)(barWidth * fill);
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + barHeight, colorRelleno);
        }

        // 4. Texto informativo centrado sobre la barra o a un lado
        String text = "Aurum: " + totalAurum;
        if (barrasCompletas > 0) {
            text += " (x" + (barrasCompletas + 1) + ")";
        }

        // Renderizar el texto centrado justo encima de tu barra de Aurum
        int textWidth = mc.font.width(text);
        int textX = x + (barWidth / 2) - (textWidth / 2);
        int textY = y - 9; // 9 píxeles arriba de la barra

        guiGraphics.drawString(mc.font, text, textX, textY, 0xFFFFFFFF, true);
    }

    private static int calculateMaxAurum(Player player) {
        var soulContainer = player.getData(ModAttachments.ACTIVE_SOUL);
        ItemStack soulStack = soulContainer.soulStack();

        if (soulStack.isEmpty()) {
            return player.getData(ModAttachments.AURUM_BLOOD);
        }

        SoulData soulData = soulContainer.activeSoul();
        int maxHonor = soulData.maxHonor();
        int tier = soulData.idTier();
        int maxHealth = (int) player.getMaxHealth();

        return (maxHonor + maxHealth) * tier;
    }

    private int interpolateColor(int c1, int c2, float ratio) {
        float r = ((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * ratio;
        float g = ((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * ratio;
        float b = (c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * ratio;
        return (0xFF << 24) | (Math.round(r) << 16) | (Math.round(g) << 8) | Math.round(b);
    }
}