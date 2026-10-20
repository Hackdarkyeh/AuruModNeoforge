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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Dibuja el líquido dentro de la jaula. Cada bloque dibuja su caja de líquido; donde hay otro
 * EnergyStorageBlock al lado, la caja llega hasta el borde y solo se dibujan las partes de esa cara
 * que el vecino NO cubre. Así no quedan huecos ni escalones en formas irregulares.
 */
public class EnergyStorageBlockEntityRenderer implements BlockEntityRenderer<EnergyStorageBlockEntity> {

    private static final ResourceLocation CLEAN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "block/energy_fluid");
    private static final ResourceLocation DARK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "block/dark_energy_fluid");
    private static final int ALPHA = 215;
    private static final float EPS = 0.001f;

    /** Caja de líquido en unidades de 1/16 de bloque. */
    private record Box(float x0, float x1, float y0, float y1, float z0, float z1) {}

    public EnergyStorageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EnergyStorageBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

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

        BlockState state = be.getBlockState();
        Box b = boxOf(state, be.displayFill);
        if (b == null) return;

        BlockPos pos = be.getBlockPos();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(be.isDarkEnergy() ? DARK_TEXTURE : CLEAN_TEXTURE);
        VertexConsumer c = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f m = poseStack.last().pose();

        // ---- Cara superior ----
        if (state.getValue(EnergyStorageBlock.UP) && b.y1() >= 16f - EPS) {
            // Llega al borde: solo se dibuja lo que el bloque de arriba no tapa
            Box nb = neighborBox(level, pos, Direction.UP);
            float[] cut = (nb != null && nb.y0() <= EPS) ? new float[]{nb.x0(), nb.x1(), nb.z0(), nb.z1()} : null;
            face(c, m, sprite, Direction.UP, 16f, b.x0(), b.x1(), b.z0(), b.z1(), cut);
        } else {
            // Superficie libre del líquido
            face(c, m, sprite, Direction.UP, b.y1(), b.x0(), b.x1(), b.z0(), b.z1(), null);
        }

        // ---- Cara inferior ----
        if (state.getValue(EnergyStorageBlock.DOWN)) {
            Box nb = neighborBox(level, pos, Direction.DOWN);
            float[] cut = (nb != null && nb.y1() >= 16f - EPS) ? new float[]{nb.x0(), nb.x1(), nb.z0(), nb.z1()} : null;
            face(c, m, sprite, Direction.DOWN, 0f, b.x0(), b.x1(), b.z0(), b.z1(), cut);
        } else {
            face(c, m, sprite, Direction.DOWN, b.y0(), b.x0(), b.x1(), b.z0(), b.z1(), null);
        }

        // ---- Caras laterales ----
        side(c, m, sprite, level, pos, state, b, Direction.NORTH, EnergyStorageBlock.NORTH);
        side(c, m, sprite, level, pos, state, b, Direction.SOUTH, EnergyStorageBlock.SOUTH);
        side(c, m, sprite, level, pos, state, b, Direction.WEST, EnergyStorageBlock.WEST);
        side(c, m, sprite, level, pos, state, b, Direction.EAST, EnergyStorageBlock.EAST);
    }

    private static void side(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, Level level, BlockPos pos,
                             BlockState state, Box b, Direction d,
                             net.minecraft.world.level.block.state.properties.BooleanProperty connected) {
        boolean ns = d.getAxis() == Direction.Axis.Z; // norte/sur: el plano es z, el eje horizontal es x
        float plane = switch (d) {
            case NORTH -> b.z0();
            case SOUTH -> b.z1();
            case WEST -> b.x0();
            default -> b.x1();
        };
        float a0 = ns ? b.x0() : b.z0();
        float a1 = ns ? b.x1() : b.z1();

        float[] cut = null;
        if (state.getValue(connected)) {
            Box nb = neighborBox(level, pos, d);
            if (nb != null) {
                cut = ns ? new float[]{nb.x0(), nb.x1(), nb.y0(), nb.y1()}
                        : new float[]{nb.z0(), nb.z1(), nb.y0(), nb.y1()};
            }
        }
        face(c, m, s, d, plane, a0, a1, b.y0(), b.y1(), cut);
    }

    /** Caja de líquido de un bloque según sus conexiones y su nivel (0..1). null si está vacío. */
    private static Box boxOf(BlockState s, float p) {
        if (p <= EPS || !s.hasProperty(EnergyStorageBlock.NORTH)) return null;
        float x0 = s.getValue(EnergyStorageBlock.WEST) ? 0 : 1;
        float x1 = s.getValue(EnergyStorageBlock.EAST) ? 16 : 15;
        float z0 = s.getValue(EnergyStorageBlock.NORTH) ? 0 : 1;
        float z1 = s.getValue(EnergyStorageBlock.SOUTH) ? 16 : 15;
        float yb = s.getValue(EnergyStorageBlock.DOWN) ? 0 : 1;
        float yMax = s.getValue(EnergyStorageBlock.UP) ? 16 : 15;
        return new Box(x0, x1, yb, yb + p * (yMax - yb), z0, z1);
    }

    private static Box neighborBox(Level level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        BlockState ns = level.getBlockState(np);
        if (!ns.hasProperty(EnergyStorageBlock.NORTH)) return null;
        if (level.getBlockEntity(np) instanceof EnergyStorageBlockEntity nbe) {
            return boxOf(ns, nbe.displayFill);
        }
        return null;
    }

    /**
     * Dibuja el rectángulo [a0,a1]x[b0,b1] de una cara, quitando la parte cubierta por "cut"
     * (también {a0,a1,b0,b1}). Ejes: arriba/abajo → a=x, b=z; norte/sur → a=x, b=y; oeste/este → a=z, b=y.
     */
    private static void face(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, Direction d, float plane,
                             float a0, float a1, float b0, float b1, float[] cut) {
        if (a1 - a0 <= EPS || b1 - b0 <= EPS) return;
        if (cut == null) {
            quad(c, m, s, d, plane, a0, a1, b0, b1);
            return;
        }
        float ia0 = Math.max(a0, cut[0]), ia1 = Math.min(a1, cut[1]);
        float ib0 = Math.max(b0, cut[2]), ib1 = Math.min(b1, cut[3]);
        if (ia1 - ia0 <= EPS || ib1 - ib0 <= EPS) { // no se solapan: la cara entera queda a la vista
            quad(c, m, s, d, plane, a0, a1, b0, b1);
            return;
        }
        if (ia0 - a0 > EPS) quad(c, m, s, d, plane, a0, ia0, b0, b1);
        if (a1 - ia1 > EPS) quad(c, m, s, d, plane, ia1, a1, b0, b1);
        if (ib0 - b0 > EPS) quad(c, m, s, d, plane, ia0, ia1, b0, ib0);
        if (b1 - ib1 > EPS) quad(c, m, s, d, plane, ia0, ia1, ib1, b1);
    }

    /** Un cuadrilátero con los vértices en sentido antihorario visto desde fuera. */
    private static void quad(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, Direction d, float plane,
                             float a0, float a1, float b0, float b1) {
        switch (d) {
            case UP -> {
                v(c, m, s, a0, plane, b0, a0, b0, 0, 1, 0);
                v(c, m, s, a0, plane, b1, a0, b1, 0, 1, 0);
                v(c, m, s, a1, plane, b1, a1, b1, 0, 1, 0);
                v(c, m, s, a1, plane, b0, a1, b0, 0, 1, 0);
            }
            case DOWN -> {
                v(c, m, s, a0, plane, b0, a0, b0, 0, -1, 0);
                v(c, m, s, a1, plane, b0, a1, b0, 0, -1, 0);
                v(c, m, s, a1, plane, b1, a1, b1, 0, -1, 0);
                v(c, m, s, a0, plane, b1, a0, b1, 0, -1, 0);
            }
            case NORTH -> {
                v(c, m, s, a1, b0, plane, a1, 16 - b0, 0, 0, -1);
                v(c, m, s, a0, b0, plane, a0, 16 - b0, 0, 0, -1);
                v(c, m, s, a0, b1, plane, a0, 16 - b1, 0, 0, -1);
                v(c, m, s, a1, b1, plane, a1, 16 - b1, 0, 0, -1);
            }
            case SOUTH -> {
                v(c, m, s, a0, b0, plane, a0, 16 - b0, 0, 0, 1);
                v(c, m, s, a1, b0, plane, a1, 16 - b0, 0, 0, 1);
                v(c, m, s, a1, b1, plane, a1, 16 - b1, 0, 0, 1);
                v(c, m, s, a0, b1, plane, a0, 16 - b1, 0, 0, 1);
            }
            case WEST -> {
                v(c, m, s, plane, b0, a0, a0, 16 - b0, -1, 0, 0);
                v(c, m, s, plane, b0, a1, a1, 16 - b0, -1, 0, 0);
                v(c, m, s, plane, b1, a1, a1, 16 - b1, -1, 0, 0);
                v(c, m, s, plane, b1, a0, a0, 16 - b1, -1, 0, 0);
            }
            case EAST -> {
                v(c, m, s, plane, b0, a1, a1, 16 - b0, 1, 0, 0);
                v(c, m, s, plane, b0, a0, a0, 16 - b0, 1, 0, 0);
                v(c, m, s, plane, b1, a0, a0, 16 - b1, 1, 0, 0);
                v(c, m, s, plane, b1, a1, a1, 16 - b1, 1, 0, 0);
            }
        }
    }

    /** Un vértice. x,y,z y u,v en unidades de 1/16 de bloque. Brillo máximo: el líquido "emite" luz. */
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