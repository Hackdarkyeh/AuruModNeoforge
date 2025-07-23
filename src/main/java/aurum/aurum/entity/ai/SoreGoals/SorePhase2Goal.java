package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

public class SorePhase2Goal extends Goal {
    private final SoreBossEntity boss;
    private int switchCooldown = 0;

    public SorePhase2Goal(SoreBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return boss.getCurrentPhase() == 2;
    }

    @Override
    public void tick() {
        // Cambio dinámico de armas
        if (switchCooldown-- <= 0 && boss.getTarget() != null) {
            LivingEntity target = boss.getTarget();
            if (target != null) {
                double distance = boss.distanceTo(target);

                if (distance < 3) {
                    boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                } else if (distance > 3) {
                    boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                }
                switchCooldown = 40;
            }
        }

        // Ataque con rayos a distancia
        if (boss.getRandom().nextFloat() < 0.05f && boss.getMainHandItem().getItem() == Items.TRIDENT) {
            shootLightning();
        }
    }

    // En SorePhase2Goal.java - en shootLightning()
    private void shootLightning() {
        LivingEntity target = boss.getTarget();
        if (target != null) {
            // Usa el método estático de LightningBossLightning para asegurar que el owner se pase correctamente
            LightningBossLightning.spawn(boss, target.position()); //
        }
    }
}