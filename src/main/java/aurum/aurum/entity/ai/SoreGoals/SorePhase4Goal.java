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

    // En SorePhase4Goal.java
    @Override
    public void start() {
        // Invocar aliados más fuertes o en mayor cantidad
        // Puedes definir diferentes tipos de minions o cantidades según la salud del boss.
        for (int i = 0; i < 3; i++) { // Por ejemplo, 3 minions al inicio de la fase
            // Asegúrate de que 'SoreMinion' exista o usa un mob de Minecraft existente.
            // EntityType<Zombie> zombieType = EntityType.ZOMBIE;
            // Zombie minion = zombieType.create(boss.level());
            // if (minion != null) {
            //     minion.moveTo(boss.position().add(boss.getRandom().nextGaussian() * 2, 0, boss.getRandom().nextGaussian() * 2));
            //     minion.setTarget(boss.getTarget());
            //     boss.level().addFreshEntity(minion);
            // }
        }
        cooldown = 200; // Cooldown para invocar de nuevo
    }

    @Override
    public void tick() {
        // Podrías añadir ataques específicos de la fase 4 aquí, como un ataque de área
        if (cooldown-- <= 0) {
            // Ejemplo: un ataque de área cada cierto tiempo en fase 4
            if (boss.getTarget() != null && boss.distanceTo(boss.getTarget()) < 8) {
                // Simular una explosión o un ataque de área
                // boss.level().explode(boss, boss.getX(), boss.getY(), boss.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
            }
            cooldown = 100; // Menor cooldown para ataques más frecuentes
        }
    }
}
