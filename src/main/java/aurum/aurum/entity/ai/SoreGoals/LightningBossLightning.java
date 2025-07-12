package aurum.aurum.entity.ai.SoreGoals;

import aurum.aurum.entity.SoreBoss.SoreBossEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LightningBossLightning extends LightningBolt {
    private static final int DURATION = 30; // Duración en ticks
    private final SoreBossEntity owner;
    private int life;
    private boolean visualOnly;

    public LightningBossLightning(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
        this.owner = null;
        this.life = 2;
        this.visualOnly = false;
    }

    public LightningBossLightning(SoreBossEntity owner, Level level, Vec3 pos, boolean visualOnly) {
        super(EntityType.LIGHTNING_BOLT, level);
        this.owner = owner;
        this.setPos(pos);
        this.life = visualOnly ? 1 : DURATION;
        this.visualOnly = visualOnly;

        // Configuración especial para el boss
        if (owner != null && owner.getLastAttacker() instanceof ServerPlayer player) {
            this.setCause(player);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.life-- <= 0) {
            this.discard();
            return;
        }

        // Efectos visuales personalizados
        if (this.level().isClientSide) {
            spawnClientParticles();
        } else if (!visualOnly) {
            affectEntities();
        }
    }

    private void spawnClientParticles() {
        // Partículas moradas en lugar de las normales
        for (int i = 0; i < 10; ++i) {
            double x = this.getX() + this.random.nextGaussian() * 2.0;
            double z = this.getZ() + this.random.nextGaussian() * 2.0;
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    x, this.getY(), z,
                    0.0, 0.0, 0.0);
        }
    }

    private void affectEntities() {
        // Daño personalizado y efectos
        AABB area = new AABB(
                this.getX() - 3.0, this.getY() - 3.0, this.getZ() - 3.0,
                this.getX() + 3.0, this.getY() + 6.0, this.getZ() + 3.0
        );

        List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != owner && e.isAlive() && (owner == null || !owner.isAlliedTo(e))
        );

        for (LivingEntity entity : entities) {
            if (owner != null) {
                entity.hurt(owner.damageSources().lightningBolt(), owner.getPhaseDamageMultiplier());
            } else {
                entity.hurt(entity.damageSources().lightningBolt(), 10.0F);
            }

            // Efecto especial en jugadores
            if (entity instanceof Player player) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.CONFUSION,
                        200,
                        0,
                        false,
                        true
                ));
            }
        }
    }

    // Método para spawnear desde el boss
    public static void spawn(SoreBossEntity owner, Vec3 pos) {
        LightningBossLightning lightning = new LightningBossLightning(
                owner,
                owner.level(),
                pos,
                false
        );
        owner.level().addFreshEntity(lightning);
    }

    // Versión visual sin daño
    public static void spawnVisual(Level level, Vec3 pos) {
        LightningBossLightning lightning = new LightningBossLightning(
                null,
                level,
                pos,
                true
        );
        level.addFreshEntity(lightning);
    }
}
