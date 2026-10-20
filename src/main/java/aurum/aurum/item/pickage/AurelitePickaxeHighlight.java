// AurelitePickaxeHighlight.java - marca los bloques extra del 3x3 igual que Minecraft marca el bloque que apuntas
package aurum.aurum.item.pickage;

import aurum.aurum.item.pickage.AurelitePickaxe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * Solo cliente. Vanilla ya dibuja el contorno del bloque que apuntas; aquí dibujamos el de los
 * demás bloques del 3x3 con EXACTAMENTE el mismo estilo (líneas negras, alfa 0.4, forma real del bloque).
 */
public class AurelitePickaxeHighlight {

    @SubscribeEvent
    public static void onHighlightBlock(RenderHighlightEvent.Block event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isCrouching()) {
            return; // agachado pica 1 solo: no hay nada extra que marcar
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AurelitePickaxe) || !AurelitePickaxe.hasAreaPower(stack)) {
            return; // sin energía pica 1 solo: no hay nada extra que marcar
        }

        Level level = player.level();
        BlockHitResult hit = event.getTarget();
        BlockPos origin = hit.getBlockPos();

        // Mismo criterio que el servidor: si el bloque que apuntas no es de pico, no hay área
        if (!stack.isCorrectToolForDrops(level.getBlockState(origin))) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        Vec3 cam = event.getCamera().getPosition();

        for (BlockPos pos : AurelitePickaxe.getBreakArea(origin, hit.getDirection())) {
            if (pos.equals(origin) || !AurelitePickaxe.canBreakExtra(stack, level, pos)) {
                continue; // el principal ya lo dibuja vanilla
            }
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos, CollisionContext.of(player));
            renderOutline(poseStack, lines, shape,
                    pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        }
    }

    // Copia de LevelRenderer.renderShape (es privado) con el color del contorno vanilla: negro, alfa 0.4
    private static void renderOutline(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape,
                                      double x, double y, double z) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float dx = (float) (maxX - minX);
            float dy = (float) (maxY - minY);
            float dz = (float) (maxZ - minZ);
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            consumer.addVertex(pose, (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                    .setNormal(pose, dx, dy, dz);
            consumer.addVertex(pose, (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                    .setNormal(pose, dx, dy, dz);
        });
    }
}