package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// Ejemplo de goal para una fase específica
public class SorePhase4Goal extends Goal {
    private final SoreBossEntity boss;
    private int cooldown;

    public SorePhase4Goal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return boss.getCurrentPhase() == 4 && cooldown-- <= 0;
    }

    @Override
    public void start() {
        // Comportamiento de fase inicial (ej: invocar aliados)
        //boss.level().addFreshEntity(new SoreMinion(boss));
        //cooldown = 200; // 10 segundos
    }
}
