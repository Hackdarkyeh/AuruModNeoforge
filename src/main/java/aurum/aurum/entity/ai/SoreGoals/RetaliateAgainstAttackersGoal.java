package aurum.aurum.entity.ai.SoreGoals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class RetaliateAgainstAttackersGoal extends TargetGoal {
    private final TargetingConditions retaliateConditions = TargetingConditions.forCombat().ignoreLineOfSight();
    private final Map<String, Integer> minionSpawns = new HashMap<>(); // Mob ID -> Cantidad
    private int cooldown = 0;

    public RetaliateAgainstAttackersGoal(Mob mob) {
        super(mob, false);
        this.setFlags(EnumSet.of(Flag.TARGET));
        setupDefaultMinions(); // Configuración inicial de invocaciones
    }

    // Método para configurar fácilmente las invocaciones
    public RetaliateAgainstAttackersGoal addMinion(String mobId, int quantity) {
        minionSpawns.put(mobId, quantity);
        return this; // Permite encadenar llamadas
    }

    private void setupDefaultMinions() {
        // Valores por defecto (modificables desde fuera)
        minionSpawns.put("minecraft:zombie", 2);
        minionSpawns.put("minecraft:skeleton", 1);
    }

    // En RetaliateAgainstAttackersGoal.java
    @Override
    public boolean canUse() {

        LivingEntity currentTarget = mob.getTarget();
        LivingEntity attacker = mob.getLastHurtByMob();

        // Obtener una lista de atacantes recientes (requiere una nueva propiedad en SoreBossEntity)
        // Por ejemplo, List<LivingEntity> recentAttackers = ((SoreBossEntity)mob).getRecentAttackers();
        // int numberOfAttackers = recentAttackers.size();

        if (attacker == null) {
            // Si no hay un último atacante, verificar si hay un objetivo actual válido
            if (currentTarget != null && currentTarget.isAlive() && currentTarget.isAttackable()) {
                return false; // Continúa con el objetivo actual si es válido
            }
            return false; // No hay atacante y no hay objetivo válido
        }

        // Si el atacante es el objetivo actual y es válido, no hay necesidad de cambiar
        if (currentTarget == attacker && attacker.isAlive() && attacker.isAttackable()) {
            return false;
        }

        // Si el atacante no es válido, intentar encontrar otro objetivo o salir
        if (!attacker.isAlive() || !attacker.isAttackable()) {
            mob.setLastHurtByMob(null); // Limpiar atacante inválido
            return false;
        }

        // Caso 1: No hay objetivo actual o el objetivo actual es inválido/débil
        if (cooldown-- > 0 ) {
            if (currentTarget == null || !currentTarget.isAlive() || !currentTarget.isAttackable() ||
                    calculateEntityPower(attacker) > calculateEntityPower(currentTarget) * 1.2) { // Atacante significativamente más fuerte
                this.mob.setTarget(attacker);
                spawnMinions(mob, 1); // Invoca refuerzos para Sore
                cooldown = 150; // 7.5 segundos de cooldown 
                return true;
            }

            // Caso 2: El atacante es "fuerte" pero no lo suficiente para cambiar el objetivo
            if (calculateEntityPower(attacker) > calculateEntityPower(currentTarget) * 0.8) { // Atacante comparable
                spawnMinions(mob, 1); // Invoca un aliado para Sore
                cooldown = 150;
                return false; // No cambia de objetivo principal, pero invoca
            }

        }
        return false; // No cambia de objetivo
    }

    private float calculateEntityPower(LivingEntity entity) {
        // Fórmula personalizable: salud + daño de ataque
        return (float) (entity.getHealth() + (entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3));
    }

    private void spawnMinions(LivingEntity forWho, int multiplier) {
        minionSpawns.forEach((mobId, quantity) -> {
            for (int i = 0; i < quantity * multiplier; i++) {
                summonMinion(mobId, forWho);
            }
        });
        cooldown = 200; // 10 segundos de cooldown
    }

    private void summonMinion(String mobId, LivingEntity forWho) {
        // Implementación de invocación (ejemplo simplificado)
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(mobId));
        if (type != null && type.create(mob.level()) instanceof Mob minion) {
            minion.moveTo(forWho.position());
            minion.setTarget(forWho.getLastHurtByMob());
            mob.level().addFreshEntity(minion);
        }
    }

    @Override
    public void start() {
        LivingEntity attacker = mob.getLastHurtByMob();
        mob.setTarget(attacker);
        mob.setLastHurtByMob(null);
        super.start();
    }
}
