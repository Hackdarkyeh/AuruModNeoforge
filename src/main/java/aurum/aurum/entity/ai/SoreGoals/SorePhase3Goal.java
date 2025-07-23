package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SorePhase3Goal extends Goal {
    private final SoreBossEntity boss;
    private int itemUseCooldown = 0;
    private final Map<Item, Consumer<ItemStack>> itemBehaviors = new HashMap<>();

    public SorePhase3Goal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Configuración de items y sus comportamientos
        itemBehaviors.put(Items.GOLDEN_APPLE, stack -> {
            boss.heal(50.0f);
            boss.level().broadcastEntityEvent(boss, (byte) 10); // Efecto visual
        });

        itemBehaviors.put(Items.TOTEM_OF_UNDYING, stack -> {
            boss.setItemSlot(EquipmentSlot.OFFHAND, stack);
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
        });

        itemBehaviors.put(Items.ENDER_PEARL, stack -> {
            if (boss.getHealth() < boss.getMaxHealth() * 0.3) {
                Vec3 telePos = findSafeTeleportPosition();
                boss.randomTeleport(telePos.x, telePos.y, telePos.z, true);
            }
        });
    }

    @Override
    public boolean canUse() {
        return boss.getCurrentPhase() == 3;
    }

    @Override
    public void tick() {
        if (itemUseCooldown-- <= 0) {
            useStrategicItem();
            itemUseCooldown = 60 + boss.getRandom().nextInt(40);
        }
    }

    private void useStrategicItem() {
        // Prioridad de items según situación
        if (boss.getHealth() < boss.getMaxHealth() * 0.4) {
            tryUseItem(Items.GOLDEN_APPLE);
        } else if (boss.getTarget() != null && boss.distanceTo(boss.getTarget()) > 8) {
            tryUseItem(Items.ENDER_PEARL);
        } else if (boss.getHealth() < boss.getMaxHealth() * 0.6) {
            tryUseItem(Items.TOTEM_OF_UNDYING);
        }
    }

    private void tryUseItem(Item item) {
        ItemStack stack = new ItemStack(item);
        Consumer<ItemStack> behavior = itemBehaviors.get(item);
        if (behavior != null) {
            behavior.accept(stack);
            boss.swing(InteractionHand.MAIN_HAND);
        }
    }

    // En SorePhase3Goal.java
    private Vec3 findSafeTeleportPosition() {
        // Lógica mejorada para encontrar posición segura
        for (int i = 0; i < 10; i++) {
            double randX = boss.getX() + (boss.getRandom().nextDouble() * 16.0 - 8.0);
            double randY = boss.getY() + (boss.getRandom().nextDouble() * 8.0 - 4.0);
            double randZ = boss.getZ() + (boss.getRandom().nextDouble() * 16.0 - 8.0);
            Vec3 testPos = new Vec3(randX, randY, randZ);

            // Convierte Vec3 a BlockPos para obtener el estado del bloque
            BlockPos posDebajo = BlockPos.containing(testPos); // Bloque directamente debajo de donde estaría la entidad
            BlockPos posCuerpo = BlockPos.containing(testPos); // Bloque a la altura del cuerpo de la entidad
            BlockPos posCabeza = BlockPos.containing(testPos.x(), testPos.y() + 1.0, testPos.z()); // Bloque por encima de la cabeza

            // Simple check: Que el bloque debajo sea sólido y haya espacio para el cuerpo del jefe.
            // isCollisionShapeFullBlock(LevelReader, BlockPos) es la forma moderna de comprobar la "solidez" para la colisión.
            if (boss.level().getBlockState(posDebajo).isCollisionShapeFullBlock(boss.level(), posDebajo) && // Que tenga un bloque sólido debajo
                    !boss.level().getBlockState(posCuerpo).isCollisionShapeFullBlock(boss.level(), posCuerpo) && // Que no esté dentro de un bloque sólido a la altura de los pies
                    !boss.level().getBlockState(posCabeza).isCollisionShapeFullBlock(boss.level(), posCabeza)) { // Que haya espacio para el cuerpo (arriba de los pies)
                return testPos;
            }
        }
        return boss.position(); // Fallback si no encuentra una posición segura después de 10 intentos
    }
}