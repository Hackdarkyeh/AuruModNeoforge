package aurum.aurum.block.engineering.PortalAurum;

import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntity;
import aurum.aurum.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public class InoculatedObsidianBER implements BlockEntityRenderer<AbstractInoculatedObsidianBlockEntity> {

    private static final ResourceLocation BARRIER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("aurum", "textures/misc/aurum_barrier.png");
    /** Lados del cilindro (cuantos más, más redondo; con radio ~100 son ~4 bloques por lado). */
    private static final int BARRIER_SEGMENTS = 160;
    /** Bloques que ocupa una repetición de la textura de la barrera. */
    private static final float BARRIER_TILE = 8.0F;
    /** A partir de esta distancia horizontal la barrera se ve casi transparente. */
    private static final float BARRIER_FADE_DISTANCE = 64.0F;

    public InoculatedObsidianBER(BlockEntityRendererProvider.Context context) {
    }

    // La barrera está a ~100 bloques del núcleo: hay que seguir dibujando aunque el bloque
    // quede lejos o fuera de la pantalla.
    @Override
    public boolean shouldRenderOffScreen(AbstractInoculatedObsidianBlockEntity blockEntity) {
        // Debe ser SIEMPRE true: el juego decide si un bloque con renderer es "global" cuando compila
        // su chunk, no cada frame. Si dependiera de barrierActive, la barrera solo se dibujaría
        // mientras ese trozo de mapa estuviese en pantalla (parpadeo al girar la cámara).
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    /**
     * Por defecto la caja de renderizado es solo el bloque (1x1x1) y el juego descarta el bloque
     * cuando no sale en pantalla: la barrera desaparecía al mirar al cielo. Mientras esté activa,
     * la caja abarca todo el cilindro, así que se dibuja siempre que estés dentro o cerca.
     */
    @Override
    public AABB getRenderBoundingBox(AbstractInoculatedObsidianBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos p = blockEntity.getBlockPos();
        if (!blockEntity.isBarrierActive() || level == null) {
            return new AABB(p);
        }
        double r = AbstractInoculatedObsidianBlockEntity.getBarrierRadius() + 2.0D;
        return new AABB(p.getX() + 0.5D - r, level.getMinBuildHeight(), p.getZ() + 0.5D - r,
                p.getX() + 0.5D + r, level.getMaxBuildHeight(), p.getZ() + 0.5D + r);
    }

    /**
     * Barrera tipo world border de Minecraft, pero morada y translúcida: un cilindro vertical
     * alrededor del núcleo. Se vuelve más opaca cuanto más cerca está el jugador.
     */
    private void renderBarrier(AbstractInoculatedObsidianBlockEntity be, float partialTick,
                               PoseStack poseStack, MultiBufferSource bufferSource) {
        Level level = be.getLevel();
        if (level == null) return;

        BlockPos pos = be.getBlockPos();
        double radius = AbstractInoculatedObsidianBlockEntity.getBarrierRadius();
        float yBottom = level.getMinBuildHeight() - pos.getY();
        float yTop = level.getMaxBuildHeight() - pos.getY();

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double worldCx = pos.getX() + 0.5D;
        double worldCz = pos.getZ() + 0.5D;

        float time = level.getGameTime() + partialTick;
        float scrollU = time * 0.0020F;
        float scrollV = time * 0.0040F;
        int tilesAround = Math.max(1, Math.round((float) (2.0D * Math.PI * radius) / BARRIER_TILE));
        float vTop = (yTop - yBottom) / BARRIER_TILE;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(BARRIER_TEXTURE));
        Matrix4f mat = poseStack.last().pose();

        for (int i = 0; i < BARRIER_SEGMENTS; i++) {
            double a0 = 2.0D * Math.PI * i / BARRIER_SEGMENTS;
            double a1 = 2.0D * Math.PI * (i + 1) / BARRIER_SEGMENTS;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            float x0 = (float) (0.5D + c0 * radius), z0 = (float) (0.5D + s0 * radius);
            float x1 = (float) (0.5D + c1 * radius), z1 = (float) (0.5D + s1 * radius);

            float u0 = tilesAround * (i / (float) BARRIER_SEGMENTS) + scrollU;
            float u1 = tilesAround * ((i + 1) / (float) BARRIER_SEGMENTS) + scrollU;

            int alpha0 = barrierAlpha(worldCx + c0 * radius, worldCz + s0 * radius, cam);
            int alpha1 = barrierAlpha(worldCx + c1 * radius, worldCz + s1 * radius, cam);

            barrierVertex(vc, mat, x0, yBottom, z0, u0, scrollV, alpha0);
            barrierVertex(vc, mat, x1, yBottom, z1, u1, scrollV, alpha1);
            barrierVertex(vc, mat, x1, yTop, z1, u1, vTop + scrollV, alpha1);
            barrierVertex(vc, mat, x0, yTop, z0, u0, vTop + scrollV, alpha0);
        }
    }

    private static int barrierAlpha(double wx, double wz, Vec3 cam) {
        double dx = wx - cam.x;
        double dz = wz - cam.z;
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        float a = Mth.clamp(1.0F - d / BARRIER_FADE_DISTANCE, 0.12F, 0.90F);
        return (int) (a * 255.0F);
    }

    private static void barrierVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                      float u, float v, int alpha) {
        vc.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public void render(AbstractInoculatedObsidianBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (blockEntity.isBarrierActive()) {
            renderBarrier(blockEntity, partialTick, poseStack, bufferSource);
        }

        if (blockEntity.getCurrentState() == AbstractInoculatedObsidianBlockEntity.PortalState.MISSING_PEDESTALS) {
            Level level = blockEntity.getLevel();
            if (level == null) return;

            BlockState ghostPedestalState = ModBlocks.PEDESTAL.get().defaultBlockState();

            // 1. Recopilamos qué ítems YA están colocados en los pedestales del mundo
            List<Item> placedItems = new ArrayList<>();
            BlockPos centerPos = blockEntity.getBlockPos();

            for (BlockPos offset : AbstractInoculatedObsidianBlockEntity.getPedestalOffsets()) {
                BlockPos targetPos = centerPos.offset(offset);
                BlockEntity be = level.getBlockEntity(targetPos);
                if (be instanceof PedestalBlockEntity pedestalBe) {
                    ItemStack stack = pedestalBe.inventory.getStackInSlot(0);
                    if (!stack.isEmpty()) {
                        placedItems.add(stack.getItem());
                    }
                }
            }

            // 2. Calculamos cuáles faltan de la lista total de requeridos
            // (Imaginemos que REQUIRED_ITEMS es accesible o la replicamos/compartimos)
            // Nota: Asegúrate de tener una forma de acceder a la lista de ítems requeridos desde aquí.
            // Por simplicidad, puedes hacer un método getter estático en el BlockEntity que devuelva REQUIRED_ITEMS.
            List<Item> missingItems = new ArrayList<>(AbstractInoculatedObsidianBlockEntity.getRequiredItems());
            missingItems.removeAll(placedItems); // Quitamos los que ya se pusieron

            int missingItemIndex = 0;

            // 3. Recorremos los offsets para dibujar el fantasma y el ítem flotante solo en los vacíos
            for (BlockPos offset : AbstractInoculatedObsidianBlockEntity.getPedestalOffsets()) {
                BlockPos targetPos = centerPos.offset(offset);
                BlockEntity be = level.getBlockEntity(targetPos);

                boolean isEmpty = true;
                if (be instanceof PedestalBlockEntity pedestalBe) {
                    isEmpty = pedestalBe.inventory.getStackInSlot(0).isEmpty();
                }

                // Si el pedestal está vacío, proyectamos el holograma del pedestal y del ítem que falta
                if (isEmpty) {
                    poseStack.pushPose();

                    // Posicionarnos en el pedestal
                    poseStack.translate(offset.getX(), offset.getY(), offset.getZ());

                    // --- A) RENDERIZAR EL PEDESTAL FANTASMA ---
                    poseStack.pushPose();
                    poseStack.scale(0.95F, 0.95F, 0.95F);
                    poseStack.translate(0.025, 0.025, 0.025);

                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                            ghostPedestalState,
                            poseStack,
                            bufferSource,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                            RenderType.translucent()
                    );
                    poseStack.popPose();

                    // --- B) RENDERIZAR EL ÍTEM FLOTANTE QUE FALTA ---
                    if (missingItemIndex < missingItems.size()) {
                        Item nextMissingItem = missingItems.get(missingItemIndex);
                        missingItemIndex++;

                        poseStack.pushPose();
                        // Subimos el ítem un poco por encima del pedestal (ej: 1.1 bloques hacia arriba)
                        poseStack.translate(0.5, 1.2, 0.5);

                        // Efecto de rotación suave basado en el tiempo del mundo
                        long time = level.getGameTime();
                        float angle = (time + partialTick) * 2.0F; // Velocidad de giro
                        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

                        // Escalar el ítem para que no sea gigantesco
                        poseStack.scale(0.5F, 0.5F, 0.5F);

                        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                        itemRenderer.renderStatic(
                                new ItemStack(nextMissingItem),
                                net.minecraft.world.item.ItemDisplayContext.GROUND,
                                LightTexture.FULL_BRIGHT,
                                OverlayTexture.NO_OVERLAY,
                                poseStack,
                                bufferSource,
                                level,
                                0
                        );

                        poseStack.popPose();
                    }

                    poseStack.popPose();
                }
            }
        }
    }
}