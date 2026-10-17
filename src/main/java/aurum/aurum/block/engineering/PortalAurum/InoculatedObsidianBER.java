package aurum.aurum.block.engineering.PortalAurum;

import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntity;
import aurum.aurum.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
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

    public InoculatedObsidianBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AbstractInoculatedObsidianBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

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