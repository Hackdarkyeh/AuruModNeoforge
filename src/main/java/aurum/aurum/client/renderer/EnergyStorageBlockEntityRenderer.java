package aurum.aurum.client.renderer;

import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Dibuja el líquido dentro de la jaula. Cada bloque dibuja su porción; donde hay otro
 * EnergyStorageBlock al lado el líquido se extiende hasta el borde y no dibuja esa cara,
 * así que todo el conjunto se ve como una sola masa continua.
 */
public class EnergyStorageBlockEntityRenderer implements BlockEntityRenderer<EnergyStorageBlockEntity> {

    private static final ResourceLocation FLUID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "block/energy_fluid");
    private static final int ALPHA = 215;

    public EnergyStorageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EnergyStorageBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // --- Suavizado del nivel (el servidor solo manda saltos de 1/64) ---
        float target = be.getFillFraction();
        long now = System.currentTimeMillis();
        if (be.lastRenderMillis == 0L || now - be.lastRenderMillis > 500L) {
            be.displayFill = target;
        } else {
            float dt = Math.min((now - be.lastRenderMillis) / 1000f, 0.1f);
            be.displayFill += (target - be.displayFill) * (1f - (float) Math.exp(-dt * 8f));
            if (Math.abs(target - be.displayFill) < 0.002f) be.displayFill = target;
        }
        be.lastRenderMillis = now;

        float p = be.displayFill;
        if (p <= 0.001f) return;

        BlockState state = be.getBlockState();
        if (!state.hasProperty(EnergyStorageBlock.NORTH)) return;
        boolean north = state.getValue(EnergyStorageBlock.NORTH);
        boolean south = state.getValue(EnergyStorageBlock.SOUTH);
        boolean east = state.getValue(EnergyStorageBlock.EAST);
        boolean west = state.getValue(EnergyStorageBlock.WEST);
        boolean up = state.getValue(EnergyStorageBlock.UP);
        boolean down = state.getValue(EnergyStorageBlock.DOWN);

        // Cuerpo del líquido en unidades de 1/16 de bloque (dentro del marco de 1 px)
        float x0 = west ? 0 : 1, x1 = east ? 16 : 15;
        float z0 = north ? 0 : 1, z1 = south ? 16 : 15;
        float yb = down ? 0 : 1;
        float yMax = up ? 16 : 15;
        float yt = yb + p * (yMax - yb);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FLUID_TEXTURE);
        VertexConsumer c = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f m = poseStack.last().pose();

        // Superficie (no se dibuja si la capa está llena y hay otro bloque encima)
        if (!(up && p >= 0.999f)) {
            v(c, m, sprite, x0, yt, z0, x0, z0, 0, 1, 0);
            v(c, m, sprite, x0, yt, z1, x0, z1, 0, 1, 0);
            v(c, m, sprite, x1, yt, z1, x1, z1, 0, 1, 0);
            v(c, m, sprite, x1, yt, z0, x1, z0, 0, 1, 0);
        }
        if (!down) {
            v(c, m, sprite, x0, yb, z0, x0, z0, 0, -1, 0);
            v(c, m, sprite, x1, yb, z0, x1, z0, 0, -1, 0);
            v(c, m, sprite, x1, yb, z1, x1, z1, 0, -1, 0);
            v(c, m, sprite, x0, yb, z1, x0, z1, 0, -1, 0);
        }
        if (!north) {
            v(c, m, sprite, x1, yb, z0, x1, 16 - yb, 0, 0, -1);
            v(c, m, sprite, x0, yb, z0, x0, 16 - yb, 0, 0, -1);
            v(c, m, sprite, x0, yt, z0, x0, 16 - yt, 0, 0, -1);
            v(c, m, sprite, x1, yt, z0, x1, 16 - yt, 0, 0, -1);
        }
        if (!south) {
            v(c, m, sprite, x0, yb, z1, x0, 16 - yb, 0, 0, 1);
            v(c, m, sprite, x1, yb, z1, x1, 16 - yb, 0, 0, 1);
            v(c, m, sprite, x1, yt, z1, x1, 16 - yt, 0, 0, 1);
            v(c, m, sprite, x0, yt, z1, x0, 16 - yt, 0, 0, 1);
        }
        if (!west) {
            v(c, m, sprite, x0, yb, z0, z0, 16 - yb, -1, 0, 0);
            v(c, m, sprite, x0, yb, z1, z1, 16 - yb, -1, 0, 0);
            v(c, m, sprite, x0, yt, z1, z1, 16 - yt, -1, 0, 0);
            v(c, m, sprite, x0, yt, z0, z0, 16 - yt, -1, 0, 0);
        }
        if (!east) {
            v(c, m, sprite, x1, yb, z1, z1, 16 - yb, 1, 0, 0);
            v(c, m, sprite, x1, yb, z0, z0, 16 - yb, 1, 0, 0);
            v(c, m, sprite, x1, yt, z0, z0, 16 - yt, 1, 0, 0);
            v(c, m, sprite, x1, yt, z1, z1, 16 - yt, 1, 0, 0);
        }
    }

    /** Un vértice. x,y,z y u,v están en unidades de 1/16 de bloque. Brillo máximo: el líquido "emite" luz. */
    private static void v(VertexConsumer c, Matrix4f m, TextureAtlasSprite s,
                          float x, float y, float z, float u, float vv,
                          float nx, float ny, float nz) {
        c.addVertex(m, x / 16f, y / 16f, z / 16f)
                .setColor(255, 255, 255, ALPHA)
                .setUv(s.getU0() + (s.getU1() - s.getU0()) * (u / 16f),
                       s.getV0() + (s.getV1() - s.getV0()) * (vv / 16f))
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(nx, ny, nz);
    }
}