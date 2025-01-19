package aurum.aurum.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class ExtinguishFireGoal extends Goal {
    private final Mob golem;
    private BlockPos targetFirePos;

    public ExtinguishFireGoal(Mob golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Buscar un bloque de fuego cercano
        BlockPos golemPos = golem.blockPosition();
        for (int x = -5; x <= 5; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = golemPos.offset(x, y, z);
                    if (golem.level().getBlockState(pos).is(Blocks.FIRE)) {
                        targetFirePos = pos;
                        return true; // Si encuentra fuego, activa el goal
                    }
                }
            }
        }
        return false; // No hay fuego cerca
    }

    @Override
    public void start() {
        if (targetFirePos != null) {
            // Moverse hacia el fuego
            golem.getNavigation().moveTo(targetFirePos.getX(), targetFirePos.getY(), targetFirePos.getZ(), 1.0);
        }
    }

    @Override
    public void tick() {
        // Verifica si ha llegado al fuego
        if (targetFirePos != null && golem.blockPosition().closerThan(targetFirePos, 2.0)) {
            // Apagar el fuego
            golem.level().setBlock(targetFirePos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // Continúa usando el goal si todavía hay fuego en la posición objetivo
        return targetFirePos != null && golem.level().getBlockState(targetFirePos).is(Blocks.FIRE);
    }

    @Override
    public void stop() {
        targetFirePos = null; // Reiniciar la posición objetivo
    }
}

