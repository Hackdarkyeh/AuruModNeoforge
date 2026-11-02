package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.eventHandler.ArmorEvents.ArmorBlightShield;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.ArmorExpData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArmorControlScreen extends AbstractContainerScreen<ArmorControlMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/gui/inventory_screen/armor_control_inventory.png");

    private static final String SPRITE_DIR = "hud";
    private static final ResourceLocation XP_BG =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, SPRITE_DIR + "/aurum_bar_background");
    private static final ResourceLocation XP_FILL =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, SPRITE_DIR + "/aurum_bar_progress");
    private static final int XP_TEX_W = 182;
    private static final int XP_TEX_H = 5;

    // Colores
    private static final int COLOR_GOLD        = 0xFFFFD940;
    private static final int COLOR_GREEN       = 0xFF55FF55;
    private static final int COLOR_WHITE       = 0xFFFFFFFF;
    private static final int COLOR_GRAY        = 0xFFAA99BB;
    private static final int COLOR_PURPLE_FILL = 0xFF8A3FCC;
    private static final int COLOR_PURPLE_BG   = 0xFF1A0E2E;
    private static final int COLOR_BORDER      = 0xFF6A3DAA;
    private static final int COLOR_BTN_BG_HOVER = 0xFF4A2574;
    private static final int COLOR_GOLD_DIM     = 0xFF9E7E0D;
    private static final int COLOR_RED          = 0xFFFF5555;

    // Geometría del Panel
    private static final int DETAIL_X = 38;
    private static final int DETAIL_Y = 8;
    private static final int DETAIL_W = 130;
    private static final int DETAIL_H = 68;

    private static final int PIPS_X      = 50;
    private static final int COUNT_RIGHT = 113;
    private static final int BTN_X       = 116;
    private static final int BTN_SIZE    = 9;

    private static final int PASSIVE_Y    = 43;
    private static final int PASSIVE_BOX  = 20;
    private static final int PASSIVE_GAP  = 6;
    private static final int COLOR_PASSIVE_ON_BG  = 0xFF2A1642;
    private static final int COLOR_PASSIVE_OFF_BG = 0xFF140B21;

    private static final List<Holder<MobEffect>> PASSIVE_EFFECTS = List.of(
            MobEffects.HEALTH_BOOST, MobEffects.JUMP, MobEffects.GLOWING, MobEffects.MOVEMENT_SPEED);
    private static final String[] PASSIVE_NAMES =
            {"Aumento de Salud", "Impulso de Salto", "Luminiscencia", "Velocidad"};

    // 2 Puntos extra asignables
    private static final int EXTRA_SET_POINTS = 2;

    // Niveles:
    // Index 0: Salud (Min 1, Max 3)
    // Index 1: Salto (Min 0, Max 2)
    // Index 2: Luminiscencia (Min 0, Max 2)
    // Index 3: Velocidad (Min 0, Max 2)
    private final int[] passiveLevels = {1, 0, 0, 0};
    private final int[] passiveMinLevels = {1, 0, 0, 0};
    private final int[] passiveMaxLevels = {3, 2, 2, 2};

    private int selectedArmorIndex = -1;

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 1000;
        this.titleLabelY     = 1000;

        // Los niveles elegidos viven en el attachment del jugador (el servidor los sincroniza)
        if (this.minecraft != null && this.minecraft.player != null) {
            SetPassivesData data = this.minecraft.player.getData(ModDataComponents.SET_PASSIVES);
            this.passiveLevels[0] = data.health();
            this.passiveLevels[1] = data.jump();
            this.passiveLevels[2] = data.luminescence();
            this.passiveLevels[3] = data.speed();
        }
    }

    public ArmorControlScreen(ArmorControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 1000;
        this.titleLabelY     = 1000;
    }

    private int getSpentSetPoints() {
        int spent = 0;
        for (int i = 0; i < 4; i++) {
            spent += (passiveLevels[i] - passiveMinLevels[i]);
        }
        return spent;
    }

    private int getAvailableSetPoints() {
        return EXTRA_SET_POINTS - getSpentSetPoints();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        g.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 1. Panel Izquierdo: Slots de Armadura y XP
        for (int i = 0; i < 4; i++) {
            int slotX = x + 8;
            int slotY = y + 8 + (i * 18);

            if (i == selectedArmorIndex) {
                g.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x40FFD940);
            }

            int barX = slotX + 19;
            int barY = slotY;
            int barWidth = 4;
            int barHeight = 16;
            g.fill(barX, barY, barX + barWidth, barY + barHeight, COLOR_PURPLE_BG);

            ItemStack armorPiece = menu.slots.get(i).getItem();
            if (!armorPiece.isEmpty() && isArmor(armorPiece)) {
                ArmorExpData expData = armorPiece.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());

                float expProgress = expData.expToNextLevel() > 0
                        ? (float) expData.currentExp() / expData.expToNextLevel()
                        : 1.0F;
                int filledHeight = Mth.ceil(expProgress * barHeight);
                int fillY = barY + (barHeight - filledHeight);

                g.fill(barX, fillY, barX + barWidth, barY + barHeight, COLOR_PURPLE_FILL);
            }
        }

        // 2. Panel Derecho
        int detailX = x + DETAIL_X;
        int detailY = y + DETAIL_Y;
        int detailW = DETAIL_W;
        int detailH = DETAIL_H;

        g.fill(detailX, detailY, detailX + detailW, detailY + detailH, COLOR_PURPLE_BG);
        g.fill(detailX, detailY, detailX + detailW, detailY + 1, COLOR_BORDER);
        g.fill(detailX, detailY + detailH - 1, detailX + detailW, detailY + detailH, COLOR_BORDER);
        g.fill(detailX, detailY, detailX + 1, detailY + detailH, COLOR_BORDER);
        g.fill(detailX + detailW - 1, detailY, detailX + detailW, detailY + detailH, COLOR_BORDER);

        if (selectedArmorIndex == -1) {
            renderSetOverview(g, detailX, detailY, detailW);
        } else {
            renderPiecePillars(g, detailX, detailY, detailW, mouseX, mouseY);
        }
    }

    private void renderSetOverview(GuiGraphics g, int detailX, int detailY, int detailW) {
        g.drawString(this.font, Component.translatable("gui.aurum.set_overview"), detailX + 6, detailY + 4, COLOR_GOLD, false);

        int equippedAurumCount = countEquippedArmor();

        if (equippedAurumCount == 4) {
            int availablePts = getAvailableSetPoints();
            g.drawString(this.font, Component.translatable("gui.aurum.set_points", availablePts, EXTRA_SET_POINTS), detailX + 6, detailY + 16, COLOR_WHITE, false);
        } else {
            g.drawString(this.font, Component.translatable("gui.aurum.pieces_count", equippedAurumCount), detailX + 6, detailY + 16, COLOR_WHITE, false);
        }

        // Barra de corrosión centrada entre la información del set y las pasivas
        int barX = detailX + 6;
        int barY = detailY + 30;
        int barW = detailW - 12;

        int corrosionPercent = ArmorBlightShield.getBarrierPercent(this.minecraft.player);
        int filledW = Math.round(barW * (corrosionPercent / 100.0f));

        drawXpBar(g, barX, barY, barW, filledW);

        // Parte de la barrera perdida por la exposición a Aurum: se pinta de morado en el extremo derecho
        float corrosionLoss = ArmorBlightShield.barrierLoss(this.minecraft.player);
        if (corrosionLoss > 0F) {
            int lostW = Math.round(barW * corrosionLoss);
            g.fill(barX + barW - lostW, barY, barX + barW, barY + XP_TEX_H, 0xFFA040E0);
        }

        if (equippedAurumCount == 4) {
            int startX = passiveStartX();
            int iconY = detailY + PASSIVE_Y;

            boolean passivesOn = ArmorBlightShield.hasBarrier(this.minecraft.player);
            for (int i = 0; i < 4; i++) {
                renderPassiveEffectBox(g, startX + i * (PASSIVE_BOX + PASSIVE_GAP), iconY,
                        PASSIVE_BOX, PASSIVE_EFFECTS.get(i), passiveLevels[i], passivesOn);
            }

        } else {
            float s = 0.7f;
            g.pose().pushPose();
            g.pose().scale(s, s, 1.0f);
            g.drawString(this.font, Component.translatable("gui.aurum.requires_full_set"), (int)((detailX + 12) / s), (int)((detailY + 48) / s), 0xFF888888, false);
            g.pose().popPose();
        }
    }

    private void renderXpTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Tooltip normal de cada pieza (el mismo que en el inventario) + Barrera, que solo existe en esta pantalla
        for (int i = 0; i < 4; i++) {
            Slot slot = menu.slots.get(i);
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                ItemStack piece = slot.getItem();
                if (!piece.isEmpty()) {
                    List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, piece));
                    float capacity = ArmorBlightShield.getCapacity(piece, this.minecraft.player);
                    if (capacity > 0F) {
                        lines.add(Component.translatable("tooltip.aurum.barrier",
                                        Math.round(ArmorBlightShield.getBarrier(piece, this.minecraft.player)), Math.round(capacity))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    g.renderTooltip(this.font, lines, piece.getTooltipImage(), mouseX, mouseY);
                }
            }
        }

        List<Component> tooltipLines = new ArrayList<>();

        // Tooltips de barras de experiencia de armadura (panel izquierdo)
        for (int i = 0; i < 4; i++) {
            int slotX = x + 8;
            int slotY = y + 8 + (i * 18);
            int barX = slotX + 19;

            if (mouseX >= barX && mouseX <= barX + 4 && mouseY >= slotY && mouseY <= slotY + 16) {
                ItemStack armorPiece = menu.slots.get(i).getItem();
                if (!armorPiece.isEmpty() && isArmor(armorPiece)) {
                    ArmorExpData expData = armorPiece.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());
                    int pct = expData.expToNextLevel() > 0 ? (int) (((float) expData.currentExp() / expData.expToNextLevel()) * 100) : 100;

                    tooltipLines.add(Component.translatable("gui.aurum.level", expData.currentLevel()).withStyle(s -> s.withColor(COLOR_GOLD)));
                    tooltipLines.add(Component.translatable("gui.aurum.xp_info", expData.currentExp(), expData.expToNextLevel(), pct).withStyle(s -> s.withColor(COLOR_WHITE)));
                }
            }
        }

        // Tooltips de la vista general (Barrera de Corrosión y Pasivas de Set)
        if (selectedArmorIndex == -1) {
            int detailX = x + DETAIL_X;
            int detailY = y + DETAIL_Y;

            // Tooltip sobre la Barrera de Corrosión
            int barX = detailX + 6;
            int barY = detailY + 30;
            int barW = DETAIL_W - 12;

            if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY - 1 && mouseY <= barY + 6) {
                int corrosionPercent = ArmorBlightShield.getBarrierPercent(this.minecraft.player);

                tooltipLines.add(Component.translatable("gui.aurum.corrosion_barrier").withStyle(s -> s.withColor(COLOR_GOLD)));
                tooltipLines.add(Component.translatable("gui.aurum.corrosion_percent", corrosionPercent).withStyle(s -> s.withColor(COLOR_WHITE)));
                float corrosionLossTip = ArmorBlightShield.barrierLoss(this.minecraft.player);
                if (corrosionLossTip > 0F) {
                    tooltipLines.add(Component.translatable("gui.aurum.corrosion_loss", Math.round(corrosionLossTip * 100))
                            .withStyle(s -> s.withColor(0xAA55FF)));
                }
            }

            // Tooltip sobre los iconos de Pasivas de Set
            if (countEquippedArmor() == 4) {
                int idx = getPassiveIndexAt(mouseX, mouseY);
                if (idx >= 0) {
                    int lvl = passiveLevels[idx];
                    String lvlRoman = switch (lvl) {
                        case 3 -> "III";
                        case 2 -> "II";
                        case 1 -> "I";
                        default -> Component.translatable("gui.aurum.passive.disabled").getString();
                    };

                    tooltipLines.add(Component.literal(PASSIVE_NAMES[idx]).withStyle(s -> s.withColor(COLOR_GOLD)));
                    String reason = ArmorBlightShield.getInactiveReasonKey(this.minecraft.player);
                    if (reason != null) {
                        tooltipLines.add(Component.translatable(reason).withStyle(s -> s.withColor(COLOR_RED)));
                    }
                    if (idx == 0) {
                        tooltipLines.add(Component.translatable("gui.aurum.passive.native_health", lvlRoman)
                                .withStyle(s -> s.withColor(COLOR_GREEN)));
                        tooltipLines.add(Component.translatable("gui.aurum.passive.health_desc")
                                .withStyle(s -> s.withColor(COLOR_GRAY)));
                    } else {
                        Component statusComp = lvl > 0
                                ? Component.translatable("gui.aurum.passive.level_status", lvlRoman)
                                : Component.translatable("gui.aurum.passive.disabled");

                        tooltipLines.add(Component.translatable("gui.aurum.passive.status", statusComp)
                                .withStyle(s -> s.withColor(lvl > 0 ? COLOR_GREEN : COLOR_RED)));
                        if (idx == 2) {
                            tooltipLines.add(Component.translatable("gui.aurum.passive.luminescence_desc")
                                    .withStyle(s -> s.withColor(COLOR_GRAY)));
                            tooltipLines.add(Component.translatable("gui.aurum.passive.luminescence_radius", lvl >= 2 ? 30 : 20)
                                    .withStyle(s -> s.withColor(COLOR_GRAY)));
                        }
                        tooltipLines.add(Component.translatable("gui.aurum.passive.controls")
                                .withStyle(s -> s.withColor(COLOR_GRAY)));
                    }
                }
            }
        }

        // Tooltips de los Pilares de cada pieza individual
        if (selectedArmorIndex != -1) {
            int detailX = x + DETAIL_X;
            int detailY = y + DETAIL_Y;

            ItemStack selectedPiece = menu.slots.get(selectedArmorIndex).getItem();
            if (!selectedPiece.isEmpty() && isArmor(selectedPiece)) {
                ArmorSkillsData skillsData = selectedPiece.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());

                for (int p = 0; p < 4; p++) {
                    int rowY = detailY + 19 + (p * 12);
                    if (mouseX >= detailX + 6 && mouseX < detailX + BTN_X - 1 && mouseY >= rowY && mouseY <= rowY + 10) {
                        switch (p) {
                            case 0 -> {
                                int pct = (int) (skillsData.getAbsorptionBonusMultiplier() * 100);
                                tooltipLines.add(Component.translatable("aurum.skill.absorption.name").withStyle(s -> s.withColor(COLOR_GOLD)));
                                tooltipLines.add(Component.translatable("aurum.skill.absorption.desc", pct).withStyle(s -> s.withColor(COLOR_WHITE)));
                            }
                            case 1 -> {
                                int pct = (int) (skillsData.getRecoveryBonusMultiplier() * 100);
                                tooltipLines.add(Component.translatable("aurum.skill.recovery.name").withStyle(s -> s.withColor(COLOR_GOLD)));
                                tooltipLines.add(Component.translatable("aurum.skill.recovery.desc", pct).withStyle(s -> s.withColor(COLOR_WHITE)));
                            }
                            case 2 -> {
                                int pct = (int) (skillsData.getMagicEnchantmentMultiplier() * 100);
                                tooltipLines.add(Component.translatable("aurum.skill.magic_amp.name").withStyle(s -> s.withColor(COLOR_GOLD)));
                                tooltipLines.add(Component.translatable("aurum.skill.magic_amp.desc", pct).withStyle(s -> s.withColor(COLOR_WHITE)));
                            }
                            case 3 -> {
                                int pct = (int) (skillsData.getResonanceBonusMultiplier() * 100);
                                tooltipLines.add(Component.translatable("aurum.skill.resonance.name").withStyle(s -> s.withColor(COLOR_GOLD)));
                                tooltipLines.add(Component.translatable("aurum.skill.resonance.desc", pct).withStyle(s -> s.withColor(COLOR_WHITE)));
                            }
                        }
                    }
                }
            }
        }

        if (!tooltipLines.isEmpty()) {
            g.renderTooltip(Minecraft.getInstance().font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderPassiveEffectBox(GuiGraphics g, int x, int y, int size, Holder<MobEffect> effect, int level, boolean powered) {
        boolean isActive = level > 0 && powered;
        int bg = isActive ? COLOR_PASSIVE_ON_BG : COLOR_PASSIVE_OFF_BG;
        int border = isActive ? COLOR_GOLD : COLOR_BORDER;

        g.fill(x, y, x + size, y + size, bg);
        g.fill(x, y, x + size, y + 1, border);
        g.fill(x, y + size - 1, x + size, y + size, border);
        g.fill(x, y, x + 1, y + size, border);
        g.fill(x + size - 1, y, x + size, y + size, border);

        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);

        if (!isActive) {
            g.setColor(0.3F, 0.3F, 0.3F, 0.4F);
        }
        g.blit(x + 1, y + 1, 0, 18, 18, sprite);
        if (!isActive) {
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // Etiqueta de Nivel (I, II, III)
        if (level > 0) {
            String lvlTag = switch (level) {
                case 3 -> "III";
                case 2 -> "II";
                default -> "I";
            };
            g.pose().pushPose();
            g.pose().scale(0.6f, 0.6f, 1.0f);
            g.drawString(this.font, lvlTag, (int)((x + 11) / 0.6f), (int)((y + 12) / 0.6f), powered ? COLOR_GOLD : COLOR_GRAY, true);
            g.pose().popPose();
        }
    }

    private int passiveStartX() {
        int totalWidth = (PASSIVE_BOX * 4) + (PASSIVE_GAP * 3);
        return this.leftPos + DETAIL_X + (DETAIL_W - totalWidth) / 2;
    }

    private int getPassiveIndexAt(double mouseX, double mouseY) {
        int startX = passiveStartX();
        int iconY = this.topPos + DETAIL_Y + PASSIVE_Y;

        for (int i = 0; i < 4; i++) {
            int bx = startX + i * (PASSIVE_BOX + PASSIVE_GAP);
            if (mouseX >= bx && mouseX < bx + PASSIVE_BOX && mouseY >= iconY && mouseY < iconY + PASSIVE_BOX) {
                return i;
            }
        }
        return -1;
    }

    private int countEquippedArmor() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty() && isArmor(stack)) count++;
        }
        return count;
    }

    private void renderPiecePillars(GuiGraphics g, int detailX, int detailY, int detailW, int mouseX, int mouseY) {
        ItemStack selectedPiece = menu.slots.get(selectedArmorIndex).getItem();

        if (selectedPiece.isEmpty() || !isArmor(selectedPiece)) {
            g.pose().pushPose();
            g.pose().scale(0.8f, 0.8f, 1.0f);
            g.drawString(this.font, Component.translatable("gui.aurum.slot_empty"), (int)((detailX + 6) / 0.8f), (int)((detailY + 8) / 0.8f), COLOR_GRAY, false);
            g.pose().popPose();
            return;
        }

        ArmorExpData expData = selectedPiece.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());
        ArmorSkillsData skillsData = selectedPiece.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());

        int tier = expData.currentLevel() / 10;
        int availablePoints = skillsData.getAvailablePoints(tier);

        g.pose().pushPose();
        g.pose().scale(0.8f, 0.8f, 1.0f);
        String headerText = "TIER " + tier + "  |  Puntos: " + availablePoints;
        g.drawString(this.font, headerText, (int)((detailX + 6) / 0.8f), (int)((detailY + 5) / 0.8f), COLOR_GOLD, false);
        g.pose().popPose();

        g.fill(detailX + 5, detailY + 15, detailX + detailW - 5, detailY + 16, COLOR_BORDER);

        Component[] pilarNames = {
                Component.translatable("aurum.skill.absorption.name"),
                Component.translatable("aurum.skill.recovery.name"),
                Component.translatable("aurum.skill.magic_amp.name"),
                Component.translatable("aurum.skill.resonance.name")
        };

        int[] pilarValues = {skillsData.absorption(), skillsData.recovery(), skillsData.magicAmp(), skillsData.resonance()};
        int[] pilarMaxes = {ArmorSkillsData.MAX_ABSORPTION, ArmorSkillsData.MAX_RECOVERY, ArmorSkillsData.MAX_MAGIC_AMP, ArmorSkillsData.MAX_RESONANCE};

        for (int p = 0; p < 4; p++) {
            int rowY = detailY + 19 + (p * 12);

            float nameScale = 0.65f;
            g.pose().pushPose();
            g.pose().scale(nameScale, nameScale, 1.0f);
            g.drawString(this.font, pilarNames[p], (int)((detailX + 5) / nameScale), (int)((rowY + 2) / nameScale), COLOR_WHITE, false);
            g.pose().popPose();

            int maxPips = pilarMaxes[p];
            int currentVal = pilarValues[p];
            int barW = 39;
            int filled = Math.round(barW * (float) currentVal / maxPips);

            drawXpBar(g, detailX + PIPS_X, rowY + 2, barW, filled);

            float countScale = 0.65f;
            String countStr = currentVal + "/" + maxPips;
            int countX = detailX + COUNT_RIGHT - Mth.ceil(this.font.width(countStr) * countScale);

            g.pose().pushPose();
            g.pose().scale(countScale, countScale, 1.0f);
            g.drawString(this.font, countStr, (int)(countX / countScale), (int)((rowY + 2) / countScale), COLOR_GRAY, false);
            g.pose().popPose();

            boolean showButton = (availablePoints > 0 && currentVal < maxPips);
            if (showButton) {
                int btnX = detailX + 114;
                int btnY = rowY + 1;
                boolean isHover = mouseX >= btnX && mouseX <= btnX + 9 && mouseY >= btnY && mouseY <= btnY + 9;

                int btnBg        = isHover ? COLOR_BTN_BG_HOVER : COLOR_PURPLE_FILL;
                int btnBorder    = isHover ? COLOR_GOLD : COLOR_GOLD_DIM;
                int btnPlusColor = isHover ? COLOR_WHITE : COLOR_GOLD;

                g.fill(btnX, btnY, btnX + 9, btnY + 9, btnBg);
                g.fill(btnX, btnY, btnX + 9, btnY + 1, btnBorder);
                g.fill(btnX, btnY + 8, btnX + 9, btnY + 9, btnBorder);
                g.fill(btnX, btnY, btnX + 1, btnY + 9, btnBorder);
                g.fill(btnX + 8, btnY, btnX + 9, btnY + 9, btnBorder);

                float scale = 0.7f;
                float textW = this.font.width("+") * scale;

                float targetX = btnX + (9.0f - textW) / 2.0f;
                float targetY = btnY + 1.5f;

                g.pose().pushPose();
                g.pose().scale(scale, scale, 1.0f);
                g.drawString(this.font, "+", targetX / scale, targetY / scale, btnPlusColor, false);
                g.pose().popPose();
            }
        }
    }

    private void drawXpBar(GuiGraphics g, int x, int y, int width, int filled) {
        g.blitSprite(XP_BG, XP_TEX_W, XP_TEX_H, 0, 0, x, y, width - 1, XP_TEX_H);
        g.blitSprite(XP_BG, XP_TEX_W, XP_TEX_H, XP_TEX_W - 1, 0, x + width - 1, y, 1, XP_TEX_H);

        if (filled > 0) {
            int body = Math.min(filled, width - 1);
            g.blitSprite(XP_FILL, XP_TEX_W, XP_TEX_H, 0, 0, x, y, body, XP_TEX_H);
            if (filled >= width) {
                g.blitSprite(XP_FILL, XP_TEX_W, XP_TEX_H, XP_TEX_W - 1, 0, x + width - 1, y, 1, XP_TEX_H);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderXpTooltips(g, mouseX, mouseY);
    }

    /** Vacío a propósito: los tooltips de las piezas los dibuja renderXpTooltips (así no salen duplicados). */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        // Selección de piezas
        for (int i = 0; i < 4; i++) {
            int slotX = x + 8;
            int slotY = y + 8 + (i * 18);

            if (mouseX >= slotX && mouseX <= slotX + 24 && mouseY >= slotY && mouseY <= slotY + 18) {
                this.selectedArmorIndex = (this.selectedArmorIndex == i) ? -1 : i;
                playUiSound(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Clics en Pasivas de Set (Vista General)
        if (selectedArmorIndex == -1 && countEquippedArmor() == 4) {
            int idx = getPassiveIndexAt(mouseX, mouseY);
            if (idx >= 0) {
                if (button == 0) { // Clic Izquierdo: Aumentar Nivel / Invertir Punto
                    if (passiveLevels[idx] < passiveMaxLevels[idx]) {
                        if (getAvailableSetPoints() > 0) {
                            passiveLevels[idx]++;
                            PacketDistributor.sendToServer(new ServerboundSetPassivePacket(idx, passiveLevels[idx]));
                            playUiSound(SoundEvents.PLAYER_LEVELUP);
                            return true;
                        } else {
                            playUiSound(SoundEvents.VILLAGER_NO); // Sin puntos
                            return true;
                        }
                    }
                } else if (button == 1) { // Clic Derecho: Disminuir Nivel / Recuperar Punto
                    if (passiveLevels[idx] > passiveMinLevels[idx]) {
                        passiveLevels[idx]--;
                        PacketDistributor.sendToServer(new ServerboundSetPassivePacket(idx, passiveLevels[idx]));
                        playUiSound(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                }
            }
        }

        // Clics en botones [+] de mejora de piezas
        if (selectedArmorIndex != -1) {
            int detailX = x + DETAIL_X;
            int detailY = y + DETAIL_Y;

            ItemStack selectedPiece = menu.slots.get(selectedArmorIndex).getItem();
            if (!selectedPiece.isEmpty() && isArmor(selectedPiece)) {
                ArmorExpData expData = selectedPiece.getOrDefault(ModDataComponents.ARMOR_EXP.get(), ArmorExpData.defaultData());
                ArmorSkillsData skillsData = selectedPiece.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());

                int tier = expData.currentLevel() / 10;
                int availablePoints = skillsData.getAvailablePoints(tier);

                if (availablePoints > 0) {
                    int[] pilarValues = {skillsData.absorption(), skillsData.recovery(), skillsData.magicAmp(), skillsData.resonance()};
                    int[] pilarMaxes = {ArmorSkillsData.MAX_ABSORPTION, ArmorSkillsData.MAX_RECOVERY, ArmorSkillsData.MAX_MAGIC_AMP, ArmorSkillsData.MAX_RESONANCE};

                    for (int p = 0; p < 4; p++) {
                        if (pilarValues[p] >= pilarMaxes[p]) continue;

                        int rowY = detailY + 19 + (p * 12);
                        int btnX = detailX + BTN_X;
                        int btnY = rowY + 1;

                        if (isOverButton(mouseX, mouseY, btnX, btnY)) {
                            PacketDistributor.sendToServer(new ServerboundUpgradeSkillPacket(selectedArmorIndex, p + 1));
                            playUiSound(SoundEvents.PLAYER_LEVELUP);
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverButton(double mouseX, double mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX <= btnX + BTN_SIZE && mouseY >= btnY && mouseY <= btnY + BTN_SIZE;
    }

    private void playUiSound(net.minecraft.sounds.SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    private boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }
}