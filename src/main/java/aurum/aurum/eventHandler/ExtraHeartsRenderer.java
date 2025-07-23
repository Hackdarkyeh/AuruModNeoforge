package aurum.aurum.eventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber
public class ExtraHeartsRenderer {
    private static final int MAX_ROWS = 10; // Máximo de filas de corazones
    private static final int HEARTS_PER_ROW = 10; // Número de corazones por fila

    // Arrays de texturas para corazones completos y medios corazones
    private static final ResourceLocation[] FULL_HEARTS_TEXTURES = new ResourceLocation[MAX_ROWS];
    private static final ResourceLocation[] HALF_HEARTS_TEXTURES = new ResourceLocation[MAX_ROWS];
    private static final ResourceLocation backgroundTexture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/player_screens/hearts_background.png");


    private static boolean animating = false;

    static {
        for (int i = 0; i < MAX_ROWS; i++) { // Ahora incluye la fila 0
            FULL_HEARTS_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/player_screens/extra_hearts_" + (i + 1) + ".png");
            HALF_HEARTS_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/player_screens/extra_hearts_half_" + (i + 1) + ".png");
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || player.isCreative() || player.isSpectator()) return; // No renderizar corazones si el jugador está en creativo

        int maxHealth = (int) player.getAttributeValue(Attributes.MAX_HEALTH);

        int currentHealth = (int) Math.ceil(player.getHealth());
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int xStart = screenWidth / 2 - 90;
        int yStart = screenHeight - 38;

        RenderSystem.enableBlend();

        int totalHearts = maxHealth / 2; // Número total de corazones
        int maxRows = Math.min(MAX_ROWS, (int) Math.ceil((double) totalHearts / HEARTS_PER_ROW)); // Limita el máximo a 10 filas

        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        animating = regeneration != null;


        long currentTime = System.currentTimeMillis();
        int animationSpeed = 50; // Velocidad de animación (más bajo = más rápido)

        int currentHeartIndex = (int) ((currentTime / animationSpeed) % totalHearts); // Índice del corazón que se está animando actualmente

        for (int i = 0; i < totalHearts; i++) {
            int row = (i / HEARTS_PER_ROW);
            if (row >= MAX_ROWS) break;

            int x = xStart + (i % HEARTS_PER_ROW) * 8;
            int y = yStart;

            // Animación si tiene regeneración
            int yOffset = (animating && i == currentHeartIndex) ? -1 : 0;
            y += yOffset;

            // Renderizar fondo vacío
            RenderSystem.setShaderTexture(0, backgroundTexture);
            event.getGuiGraphics().blit(backgroundTexture, x - 1, yStart - 1 , 0, 0, 9, 9, 9, 9);

            // Obtener y Renderizar el corazón correcto según su tier
            ResourceLocation heartTexture = getHeartTexture(i, currentHealth, maxHealth);
            RenderSystem.setShaderTexture(0, heartTexture);
            event.getGuiGraphics().blit(heartTexture, x, y, 0, 0, 7, 7, 7, 7);
        }


        RenderSystem.disableBlend();
    }

    private static ResourceLocation getHeartTexture(int heartIndex, int currentHealth, int maxHealth) {
        int totalTiers = MAX_ROWS; // Cantidad de tiers (niveles de corazones)
        int heartsPerTier = HEARTS_PER_ROW; // Corazones por fila

        int tierIndex = Math.min(heartIndex / heartsPerTier, totalTiers - 1);
        int healthInTier = currentHealth - (tierIndex * heartsPerTier * 2);

        if (healthInTier > (heartIndex % heartsPerTier) * 2) {
            return FULL_HEARTS_TEXTURES[tierIndex]; // Corazón lleno del tier actual
        } else if (healthInTier == (heartIndex % heartsPerTier) * 2 + 1) {
            return HALF_HEARTS_TEXTURES[tierIndex]; // Medio corazón
        } else if (tierIndex > 0) {
            return FULL_HEARTS_TEXTURES[tierIndex - 1]; // Mostrar el tier anterior si este está vacío
        } else {
            return backgroundTexture;
        }
    }



}


