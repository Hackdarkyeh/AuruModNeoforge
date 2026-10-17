package aurum.aurum.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HideHealthBarMixin {

    private static final int MAX_ROWS = 10;
    private static final int HEARTS_PER_ROW = 10;

    // Tus texturas personalizadas externas de Aurum (7x7)
    private static final ResourceLocation[] FULL_HEARTS_TEXTURES = new ResourceLocation[MAX_ROWS];
    private static final ResourceLocation[] HALF_HEARTS_TEXTURES = new ResourceLocation[MAX_ROWS];
    private static final ResourceLocation AURUM_BACKGROUND = ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/player_screens/hearts_background.png");

    // Identificadores de Sprites nativos del Atlas de Minecraft
    private static final ResourceLocation CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation CONTAINER_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container_blinking");

    static {
        for (int i = 0; i < MAX_ROWS; i++) {
            FULL_HEARTS_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/player_screens/extra_hearts_" + (i + 1) + ".png");
            HALF_HEARTS_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath("aurum", "textures/gui/player_screens/extra_hearts_half_" + (i + 1) + ".png");
        }
    }

    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true)
    private void overrideHealthBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        int maxHealth = (int) player.getAttributeValue(Attributes.MAX_HEALTH);
        int currentHealth = (int) Math.ceil(player.getHealth());
        int absorption = (int) Math.ceil(player.getAbsorptionAmount());

        int xStart = guiGraphics.guiWidth() / 2 - 91;
        int yStart = guiGraphics.guiHeight() - 39;

        // Quitamos RenderSystem.enableBlend(), GuiGraphics ya usa blending para blit/blitSprite

        boolean hasWither = player.hasEffect(MobEffects.WITHER);
        boolean hasPoison = player.hasEffect(MobEffects.POISON);

        Gui guiInstance = (Gui) (Object) this;
        boolean isBlinking = guiInstance.getGuiTicks() < 6L && (guiInstance.getGuiTicks() % 2L == 1L);

        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        boolean animating = regeneration != null;

        long currentTime = System.currentTimeMillis();
        int animationSpeed = 50;
        int currentHeartIndex = (int) ((currentTime / animationSpeed) % HEARTS_PER_ROW);

        // 1. RENDERIZAR LA FILA ÚNICA DE VIDA COMPRIMIDA (CON TIERS)
        for (int i = 0; i < HEARTS_PER_ROW; i++) {
            if (i * 2 >= maxHealth) break;

            int x = xStart + i * 8;
            int y = yStart;

            int yOffset = (animating && i == currentHeartIndex) ? -1 : 0;

            int hpPerFila = HEARTS_PER_ROW * 2;
            int tierActual = (currentHealth - 1) / hpPerFila;
            int tierMaximo = (maxHealth - 1) / hpPerFila;

            tierActual = Mth.clamp(tierActual, 0, MAX_ROWS - 1);
            tierMaximo = Mth.clamp(tierMaximo, 0, MAX_ROWS - 1);

            int vidaRestanteEnTier = currentHealth % hpPerFila;
            if (vidaRestanteEnTier == 0 && currentHealth > 0) vidaRestanteEnTier = hpPerFila;

            int valorParaLleno = (i * 2) + 2;
            int valorParaMedio = (i * 2) + 1;

            boolean isHalf = (vidaRestanteEnTier == valorParaMedio);
            boolean isFull = (vidaRestanteEnTier >= valorParaLleno);

            // PASADA A: Dibujar el contenedor de fondo
            if ((hasWither || hasPoison) && isBlinking) {
                guiGraphics.blitSprite(CONTAINER_BLINKING_SPRITE, x, y + yOffset, 9, 9);
            } else {
                // Ya NO usamos RenderSystem aquí. GuiGraphics lo hace solo.
                guiGraphics.blit(AURUM_BACKGROUND, x, y + yOffset, 0, 0, 9, 9, 9, 9);
            }

            // PASADA B: Renderizar la base del Tier de fondo
            if (tierActual > 0) {
                if (vidaRestanteEnTier < valorParaLleno) {
                    ResourceLocation capaFondoAnterior = FULL_HEARTS_TEXTURES[tierActual - 1];
                    guiGraphics.blit(capaFondoAnterior, x + 1, y + yOffset + 1, 0, 0, 7, 7, 7, 7);
                }
            } else if (tierMaximo > tierActual && (i * 2) < maxHealth) {
                if (vidaRestanteEnTier < valorParaLleno && tierMaximo > 0) {
                    ResourceLocation capaFondoAnterior = FULL_HEARTS_TEXTURES[Math.min(tierMaximo - 1, MAX_ROWS - 1)];
                    guiGraphics.blit(capaFondoAnterior, x + 1, y + yOffset + 1, 0, 0, 7, 7, 7, 7);
                }
            }

            // PASADA C: Interiores principales
            if (hasWither || hasPoison) {
                if (isFull || isHalf) {
                    String path = hasWither ? "withered_" : "poisoned_";
                    path += isHalf ? "half" : "full";
                    if (hasWither && isBlinking) path += "_blinking";

                    ResourceLocation spriteFinal = ResourceLocation.withDefaultNamespace("hud/heart/" + path);
                    guiGraphics.blitSprite(spriteFinal, x, y + yOffset, 9, 9);
                }
            } else {
                if (isFull || isHalf) {
                    ResourceLocation customHeartTex = isHalf ? HALF_HEARTS_TEXTURES[tierActual] : FULL_HEARTS_TEXTURES[tierActual];
                    guiGraphics.blit(customHeartTex, x + 1, y + yOffset + 1, 0, 0, 7, 7, 7, 7);
                }
            }
        }

        // 2. RENDERIZAR LA FILA DE ABSORCIÓN (MANZANAS DORADAS VANILLA)
        int absorptionRows = 0;
        if (absorption > 0) {
            int totalAbsorptionHearts = (int) Math.ceil((double) absorption / 2);
            for (int i = 0; i < totalAbsorptionHearts; i++) {
                int col = i % HEARTS_PER_ROW;
                int row = i / HEARTS_PER_ROW;

                int x = xStart + col * 8;
                int y = (yStart - 10) - (row * 10);

                absorptionRows = Math.max(absorptionRows, row + 1);
                boolean isAbsorpHalf = (absorption - (i * 2) == 1);

                // Paso 1: Fondo personalizado de Aurum (Sin RenderSystem)
                guiGraphics.blit(AURUM_BACKGROUND, x, y, 0, 0, 9, 9, 9, 9);

                // Paso 2: Usar blitSprite para el relleno dorado oficial
                String typeAbs = isAbsorpHalf ? "absorbing_half" : "absorbing_full";
                ResourceLocation spriteAbs = ResourceLocation.withDefaultNamespace("hud/heart/" + typeAbs);
                guiGraphics.blitSprite(spriteAbs, x, y, 9, 9);
            }
        }

        // Quitamos RenderSystem.disableBlend()

        // 3. ACTUALIZAR POSICIÓN DE LA BARRA DE ARMADURA
        guiInstance.leftHeight = 39 + 10 + (absorptionRows * 10);
    }
}