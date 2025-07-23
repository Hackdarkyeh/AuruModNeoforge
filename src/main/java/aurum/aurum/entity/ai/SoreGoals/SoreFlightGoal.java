package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SoreFlightGoal extends Goal {
    private final SoreBossEntity boss;
    private boolean isFlying = false;
    private int flightDuration = 0;
    private int lightningAttackCooldown = 0; // Nuevo: Cooldown para ataques de rayo

    public SoreFlightGoal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); // Añadido Flag.LOOK para que mire al objetivo
    }

    @Override
    public boolean canUse() {
        // Condiciones para empezar a volar:
        // 1. Salud por debajo del 50% (con 10% de probabilidad) para escape/reposicionamiento.
        // 2. O si el objetivo está lejos (más de 12 bloques) para ataques a distancia.
        return (boss.getHealth() < boss.getMaxHealth() * 0.5 && boss.getRandom().nextFloat() < 0.1f) ||
                (boss.getTarget() != null && boss.distanceTo(boss.getTarget()) > 12);
    }

    @Override
    public void start() {
        isFlying = true;
        flightDuration = 100 + boss.getRandom().nextInt(100); // Duración aleatoria de vuelo
        boss.setNoGravity(true); // Desactiva la gravedad para volar
        lightningAttackCooldown = 20; // Cooldown inicial para el primer ataque de rayo
        boss.invokeStart.start(boss.tickCount); // Opcional: Inicia animación de vuelo/invocación
    }

    @Override
    public void tick() {
        if (isFlying) {
            LivingEntity target = boss.getTarget();

            // Movimiento en vuelo: Se mueve hacia el objetivo, pero ligeramente por encima
            if (target != null) {
                Vec3 targetPos = target.position().add(0, 3, 0); // Volar 3 bloques sobre el objetivo
                Vec3 moveDir = targetPos.subtract(boss.position()).normalize();
                boss.setDeltaMovement(moveDir.scale(0.3)); // Velocidad de movimiento en vuelo

                // Hace que el jefe mire al objetivo mientras vuela
                boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

                // Ataques aéreos: Lanza un rayo periódicamente
                if (lightningAttackCooldown-- <= 0) {
                    LightningBossLightning.spawn(boss, target.position()); // Lanza un rayo al objetivo
                    lightningAttackCooldown = 60 + boss.getRandom().nextInt(40); // Cooldown para el siguiente rayo (ej: 3-5 segundos)
                }
            } else {
                // Si no hay objetivo, simplemente flota o desciende gradualmente
                boss.setDeltaMovement(boss.getDeltaMovement().add(0, -0.05, 0)); // Descenso lento
            }

            // Condiciones para dejar de volar:
            // 1. La duración del vuelo ha terminado.
            // 2. O la salud del jefe se ha recuperado significativamente (si voló para escapar por baja salud).
            // 3. O el objetivo está lo suficientemente cerca para un combate en tierra.
            if (flightDuration-- <= 0 ||
                    (boss.getHealth() > boss.getMaxHealth() * 0.8 && boss.getRandom().nextFloat() < 0.2f) || // Si recuperó mucha salud
                    (target != null && boss.distanceTo(target) < 6)) { // Si el objetivo está cerca para ataque cuerpo a cuerpo
                stopFlying();
            }
        }
    }

    private void stopFlying() {
        isFlying = false;
        boss.setNoGravity(false); // Reactiva la gravedad
        // Reduce la velocidad horizontal para un aterrizaje más suave
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(0.5, 0, 0.5));
        boss.invokeEnd.start(boss.tickCount); // Opcional: Inicia animación de aterrizaje/fin de invocación
    }

    @Override
    public boolean canContinueToUse() {
        return isFlying; // Continúa usando el Goal mientras está volando
    }

    @Override
    public void stop() {
        // Asegurarse de que el jefe no tenga gravedad si el goal se detiene por otras razones
        if (isFlying) {
            stopFlying();
        }
        super.stop();
    }
}