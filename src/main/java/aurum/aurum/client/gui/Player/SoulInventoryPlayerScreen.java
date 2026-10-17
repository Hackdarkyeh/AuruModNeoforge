package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.Soul.SoulData;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.init.ModDataComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SoulInventoryPlayerScreen extends AbstractContainerScreen<SoulInventoryPlayerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/gui/inventory_screen/soul_inventory.png");

    // ── COLORES ───────────────────────────────────────────────────────────────
    // Honor bar
    private static final int COLOR_HONOR_BG    = 0xFF1A0E2E;  // fondo barra
    private static final int COLOR_HONOR_FILL  = 0xFF8A3FCC;  // relleno púrpura
    private static final int COLOR_HONOR_GLOW  = 0xFFBB6AFF;  // borde brillante
    // Textos
    private static final int COLOR_GOLD        = 0xFFFFD940;
    private static final int COLOR_WHITE       = 0xFFFFFFFF;
    private static final int COLOR_GRAY        = 0xFFAA99BB;
    // Nodos
    private static final int COLOR_NODE_EMPTY  = 0xFF2A1A42;
    private static final int COLOR_NODE_BORDER = 0xFF6A3DAA;
    private static final int COLOR_NODE_ACTIVE = 0xFF9B5FDD;

    public SoulInventoryPlayerScreen(SoulInventoryPlayerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        //this.imageWidth  = 176;
        //this.imageHeight = 200;  // un poco más alto que el inventario normal para tener espacio de info
        this.inventoryLabelY = 1000;
        this.titleLabelY     = 1000;
    }

    @Override
    protected void init() {
        super.init();
    }

    // ── RENDER PRINCIPAL ──────────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // GuiGraphics asigna el color base (blanco) y el shader correcto automáticamente en 1.21.1
        g.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Leer datos del alma
        ItemStack soulStack = menu.getSoulItemStack();

        // ── ESTADO SIN ALMA ───────────────────────────────────────────────────
        if (soulStack.isEmpty() || !soulStack.has(ModDataComponents.SOUL_COMPONENT.get())) {
            drawNoSoulState(g, x, y);
            return;
        }

        SoulData data = soulStack.get(ModDataComponents.SOUL_COMPONENT.get());
        if (data == null) return;

        // ── TIER ──────────────────────────────────────────────────────────────
        drawTier(g, x, y, data.idTier());

        // ── BARRA DE HONOR ────────────────────────────────────────────────────
        drawHonorBar(g, x, y, data.honor(), data.idTier());

        // ── PESO USADO / MÁXIMO ───────────────────────────────────────────────
        drawWeight(g, x, y, data);

        // ── HABILIDADES ───────────────────────────────────────────────────────
        drawAbilities(g, x, y, data, mouseX, mouseY);
    }

    // ── RENDERIZADO DE LABELS ─────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE DIBUJO
    // ─────────────────────────────────────────────────────────────────────────

    /** Mensaje cuando no hay alma equipada en el slot. */
    private void drawNoSoulState(GuiGraphics g, int x, int y) {
        String msg  = "No soul equipped";
        int tx = x + imageWidth/2 - Minecraft.getInstance().font.width(msg)/2;
        g.drawString(Minecraft.getInstance().font, msg, tx, y + 42, COLOR_GRAY, false);
    }

    /** Dibuja "TIER X" con estilo dorado encima del slot del alma. */
    private void drawTier(GuiGraphics g, int x, int y, int tier) {
        //String tierLabel = "TIER " + tier;
        //String tierName  = getTierName(tier);

        /* Etiqueta "TIER X"
        int tx = x + imageWidth/2 - Minecraft.getInstance().font.width(tierLabel)/2;
        g.drawString(Minecraft.getInstance().font, tierLabel, tx, y + 10, COLOR_GOLD, false);

        // Nombre del tier debajo, más pequeño y en gris
        int nx = x + imageWidth/2 - Minecraft.getInstance().font.width(tierName)/2;
        g.drawString(Minecraft.getInstance().font, tierName, nx, y + 20, COLOR_GRAY, false);*/
    }

    /** Barra de honor con fondo, relleno degradado y texto. */
    private void drawHonorBar(GuiGraphics g, int x, int y, int honor, int tier) {
        int barX  = x + 10;
        int barY  = y + 66;
        int barW  = imageWidth - 20;
        int barH  = 9;

        int maxHonor = getMaxHonorForTier(tier, honor);
        float pct = maxHonor > 0 ? Math.min(1f, (float) honor / maxHonor) : 0f;

        // Fondo
        g.fill(barX, barY, barX + barW, barY + barH, COLOR_HONOR_BG);

        // Relleno
        int fillW = (int)(barW * pct);
        if (fillW > 0) {
            // Degradado manual: dos mitades de color
            g.fill(barX,            barY, barX + fillW/2,  barY + barH, COLOR_HONOR_FILL);
            g.fill(barX + fillW/2,  barY, barX + fillW,    barY + barH, COLOR_HONOR_GLOW);
        }

        // Borde
        g.fill(barX,          barY,         barX + barW, barY + 1,     COLOR_NODE_BORDER); // top
        g.fill(barX,          barY + barH-1,barX + barW, barY + barH,  COLOR_NODE_BORDER); // bottom
        g.fill(barX,          barY,         barX + 1,    barY + barH,  COLOR_NODE_BORDER); // left
        g.fill(barX + barW-1, barY,         barX + barW, barY + barH,  COLOR_NODE_BORDER); // right

        // Texto de honor centrado sobre la barra
        String honorText = honor + " / " + maxHonor + " Honor";
        int tx = x + imageWidth/2 - Minecraft.getInstance().font.width(honorText)/2;
        g.drawString(Minecraft.getInstance().font, honorText, tx, barY - 15, COLOR_GOLD, false);
    }

    /** Peso total usado vs máximo. */
    private void drawWeight(GuiGraphics g, int x, int y, SoulData data) {
        int usedWeight = data.abilities().stream()
                .mapToInt(SoulAbilityData::weight)
                .sum();

        String weightText = "Weight: " + usedWeight + " / " + data.maxWeight();
        int tx = x + imageWidth/2 - Minecraft.getInstance().font.width(weightText)/2;
        g.drawString(Minecraft.getInstance().font, weightText, tx, y + 67, COLOR_GRAY, false);
    }

    /**
     * Dibuja las habilidades del alma.
     * Cada ability que tenga abilityId muestra el icono del ítem correspondiente (16x16).
     * Las que están vacías (abilityId == null) muestran un slot vacío.
     * El layout es una cuadrícula automática si no hay offset, o posición por offset si lo tiene.
     */
    private void drawAbilities(GuiGraphics g, int x, int y, SoulData data, int mouseX, int mouseY) {
        // Título sección
        //String abTitle = "— Soul Abilities —";
        //int tx = x + imageWidth/2 - Minecraft.getInstance().font.width(abTitle)/2;
        //g.drawString(Minecraft.getInstance().font, abTitle, tx, y + 80, COLOR_GOLD, false);

        List<SoulAbilityData> abilities = data.abilities();
        int slotSize = 20;  // 16px icono + 2px padding cada lado

        // Distribuir en grid centrado si no tienen offset propio
        int cols = 4;
        int gridW = cols * slotSize;
        int startX = x + imageWidth/2 - gridW/2;
        int startY = y + 20;

        // Tooltip acumulado para hover
        List<Component> tooltipLines = new ArrayList<>();
        int tooltipX = mouseX, tooltipY = mouseY;
        boolean showTooltip = false;

        for (int i = 0; i < abilities.size(); i++) {
            SoulAbilityData ability = abilities.get(i);

            int slotX, slotY;
// Centro del slot del alma en coordenadas de pantalla
            int soulCX = x + 80 + 9; // 80 = posición X del slot, +9 = centro del slot 18x18
            int soulCY = y + 35 + 9; // 35 = posición Y del slot, +9 = centro

            if (ability.hasOffset() && ability.offset() != null) {
                // Posición personalizada relativa al slot del alma (80,35)

                if (i < 4) {
                    // Grupo IZQUIERDO — 2 columnas x 2 filas a la izquierda
                    int col = i % 2;       // 0 o 1 (columna dentro del grupo)
                    int row = i / 2;       // 0 o 1 (fila: 0=arriba, 1=abajo)
                    slotX = soulCX - 18 - 4 - (2 - col) * slotSize; // crece hacia la izquierda
                    slotY = soulCY - slotSize + row * slotSize - 14;       // centrado vertical
                } else {
                    // Grupo DERECHO — 2 columnas x 2 filas a la derecha
                    int j   = i - 4;
                    int col = j % 2;
                    int row = j / 2;
                    slotX = soulCX + 18 + 4 + col * slotSize;        // crece hacia la derecha
                    slotY = soulCY - slotSize + row * slotSize - 14 ;       // centrado vertical
                }
                // ── Fondo del slot ────────────────────────────────────────────────
                boolean hasAbility = ability.hasAbility();
                int borderColor = hasAbility ? COLOR_NODE_ACTIVE : COLOR_NODE_BORDER;
                int bgColor     = hasAbility ? 0xFF1E0E36        : COLOR_NODE_EMPTY;

                g.fill(slotX,     slotY,     slotX+18, slotY+18, bgColor);
                // Borde
                g.fill(slotX,     slotY,     slotX+18, slotY+1,  borderColor);
                g.fill(slotX,     slotY+17,  slotX+18, slotY+18, borderColor);
                g.fill(slotX,     slotY,     slotX+1,  slotY+18, borderColor);
                g.fill(slotX+17,  slotY,     slotX+18, slotY+18, borderColor);

                // ── Icono del ítem ────────────────────────────────────────────────
                if (hasAbility) {
                    ItemStack iconStack = getItemStackForAbility(ability.abilityId());
                    if (!iconStack.isEmpty()) {
                        g.renderItem(iconStack, slotX + 1, slotY + 1);
                    } else {
                        // Fallback: cuadrado de color si no se encuentra el item
                        g.fill(slotX+3, slotY+3, slotX+15, slotY+15, COLOR_NODE_ACTIVE);
                    }

                    // ── Hover tooltip ─────────────────────────────────────────────
                    if (mouseX >= slotX && mouseX < slotX+18 && mouseY >= slotY && mouseY < slotY+18) {
                        showTooltip = true;
                        tooltipX = mouseX;
                        tooltipY = mouseY;
                        tooltipLines.clear();
                        // Nombre de la habilidad
                        if (ability.name() != null) {
                            tooltipLines.add(Component.literal(ability.name()).withStyle(
                                    style -> style.withColor(0xBB6AFF)));
                        }
                        // ID
                        tooltipLines.add(Component.literal("ID: " + ability.abilityId().getPath())
                                .withStyle(style -> style.withColor(0xAA99BB)));
                        // Peso
                        tooltipLines.add(Component.literal("Weight: " + ability.weight())
                                .withStyle(style -> style.withColor(0xFFD940)));
                    }
                }
            }

            // Dibujar tooltip si hay hover
            if (showTooltip && !tooltipLines.isEmpty()) {
                g.renderTooltip(Minecraft.getInstance().font, tooltipLines, Optional.empty(), tooltipX, tooltipY);
            }
        }
            // Siempre grid automático — ignora offset por ahora



    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca el ítem del registro de Minecraft usando el ResourceLocation de la habilidad.
     * El abilityId debe coincidir con el registry name del ítem (ej: "aurum:expansion_super_speed").
     */
    private ItemStack getItemStackForAbility(ResourceLocation abilityId) {
        if (abilityId == null) return ItemStack.EMPTY;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(abilityId);
        return item.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private String getTierName(int tier) {
        return switch (tier) {
            case 1 -> "Awakened Soul";
            case 2 -> "Empowered Soul";
            case 3 -> "Ascended Soul";
            default -> "Unknown";
        };
    }

    private int getMaxHonorForTier(int tier, int honor) {
        return switch (tier) {
            case 1 -> 750;
            case 2 -> 1500;
            case 3 -> 3000;
            default -> honor;
        };
    }
}