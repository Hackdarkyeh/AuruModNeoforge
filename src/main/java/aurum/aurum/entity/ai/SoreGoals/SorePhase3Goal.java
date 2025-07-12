package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
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

    private Vec3 findSafeTeleportPosition() {
        // Lógica para encontrar posición segura
        return boss.position().add(
                boss.getRandom().nextGaussian() * 5,
                0,
                boss.getRandom().nextGaussian() * 5
        );
    }
}