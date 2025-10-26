package aurum.aurum.block.AntiElytraBlock;

import aurum.aurum.init.ModBlockEntities;
import aurum.aurum.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class AntiElytraBlockEntity extends BlockEntity {

    private int attackCooldown = 0;
    private int currentPower = 1;
    private static final int MAX_POWER = 10;
    private static final int BASE_DAMAGE = 50;
    private UUID lastTargetId;
    private static final int ATTACK_INTERVAL = 40; // 2 segundos (40 ticks)
    private static final int SEARCH_RADIUS = 40;

    public AntiElytraBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ANTI_ELYTRA_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, AntiElytraBlockEntity pBlockEntity) {
        if (pLevel.isClientSide) {
            return;
        }

        pBlockEntity.attackCooldown--;

        if (pBlockEntity.attackCooldown <= 0) {
            ServerLevel serverLevel = (ServerLevel) pLevel;
            AABB searchArea = new AABB(pPos).inflate(SEARCH_RADIUS);
            ServerPlayer targetPlayer = null;

            // Buscar jugadores en el área
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, searchArea)) {
                // Verificar si tiene Elytra puesta y no está dañada
                ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
                if (elytra.getItem() == Items.ELYTRA) {
                    targetPlayer = player;
                    break;
                }
            }

            // Si hay un objetivo, aplicar daño y efectos
            if (targetPlayer != null) {
                // Lógica de "torre infernal"
                if (pBlockEntity.lastTargetId == null || !pBlockEntity.lastTargetId.equals(targetPlayer.getUUID())) {
                    pBlockEntity.currentPower = 1;
                    pBlockEntity.lastTargetId = targetPlayer.getUUID();
                } else {
                    if (pBlockEntity.currentPower < MAX_POWER) {
                        pBlockEntity.currentPower++;
                    }
                }

                // Aplicar daño a la Elytra
                ItemStack elytra = targetPlayer.getItemBySlot(EquipmentSlot.CHEST);
                int damageToApply = BASE_DAMAGE * pBlockEntity.currentPower;
                elytra.hurtAndBreak(damageToApply, targetPlayer, EquipmentSlot.CHEST);

                // Generar partículas de rayo
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        pPos.getX() + 0.5,
                        pPos.getY() + 1.0,
                        pPos.getZ() + 0.5,
                        50,
                        (targetPlayer.getX() - pPos.getX()) / 2.0,
                        (targetPlayer.getY() - pPos.getY()) / 2.0,
                        (targetPlayer.getZ() - pPos.getZ()) / 2.0,
                        0.1
                );

                pBlockEntity.attackCooldown = ATTACK_INTERVAL;
            } else {
                // Si no hay objetivo, reiniciar la "torre infernal"
                if (pBlockEntity.lastTargetId != null) {
                    pBlockEntity.currentPower = 1;
                    pBlockEntity.lastTargetId = null;
                }
            }
        }
    }
}