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

    @Override
    public boolean canUse() {
        if (cooldown-- > 0) return false;

        LivingEntity currentTarget = mob.getTarget();
        LivingEntity attacker = mob.getLastHurtByMob();

        if (attacker == null) return false;

        // Caso 1: No hay objetivo actual
        if (currentTarget == null) {
            return true;
        }

        // Caso 2: Comparar fuerza de los objetivos
        float currentTargetPower = calculateEntityPower(currentTarget);
        float attackerPower = calculateEntityPower(attacker);

        if (attackerPower < currentTargetPower) {
            spawnMinions(attacker, 1); // Invoca aliados para el atacante débil
            return false;
        } else if (attackerPower > currentTargetPower) {
            spawnMinions(mob, 2); // Invoca refuerzos para Sore
            return true;
        }

        return false;
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
