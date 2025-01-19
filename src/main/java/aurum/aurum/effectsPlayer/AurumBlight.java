package aurum.aurum.effectsPlayer;

import aurum.aurum.init.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

import java.util.List;

import static aurum.aurum.init.ModEffects.AurumBlightEffect;

public class AurumBlight extends MobEffect {

    private static int ARMOR_DAMAGE = 10;
    private boolean inmuneMilk = false;

    public AurumBlight() {
        super(MobEffectCategory.HARMFUL, -26113);

    }

    @Override
    public String getDescriptionId() {
        return "effect.aurum.aurum_blight";
    }

    @Override
    public boolean applyEffectTick( LivingEntity entity, int amplifier) {
        inmuneMilk = amplifier != 0;
        // 1. Eliminar todos los efectos

        MobEffectInstance effectInstanceToKeep = null;
        if ( !entity.level().isClientSide()) {
                Holder<MobEffect> effectToKeep = AurumBlightEffect.getDelegate(); // El efecto que deseas mantener
                effectInstanceToKeep = entity.getEffect(effectToKeep); // Obtiene la instancia del efecto que deseas mantener


            // Elimina todos los efectos
            entity.removeAllEffects();

            // Si la entidad tenía el efecto que deseas mantener, reaplícalo
            if (effectInstanceToKeep != null) {
                int newDuration = effectInstanceToKeep.getDuration() -20;
                effectInstanceToKeep.update(ModEffects.createEffectInstance(AurumBlightEffect.getDelegate(), newDuration, 0));
                entity.addEffect(effectInstanceToKeep);
            }
            Level level = entity.level();
            DamageSource aurumBlightSource = AurumBlightDamageSource.createAurumBlightDamageSource(level.registryAccess());
            entity.hurt(aurumBlightSource, 2.0F * (amplifier + 1)); // Daño escala con el amplificador
            entity.hurt(entity.damageSources().wither(), 2);
            // 3. Dañar la armadura
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    ItemStack armorItem = entity.getItemBySlot(slot);
                    if (!armorItem.isEmpty()) {
                        // Aplica daño al ítem
                        armorItem.hurtAndBreak(ARMOR_DAMAGE * (amplifier + 1), entity, slot);

                        // Verifica si el ítem se rompió
                        if (armorItem.isEmpty()) {
                            // Remueve la armadura rota
                            entity.setItemSlot(slot, ItemStack.EMPTY);
                        }
                    }
                }
            }

            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(5));
            for (LivingEntity nearbyEntity : nearbyEntities) {
                if (nearbyEntity != entity) {
                    for (MobEffectInstance effect : nearbyEntity.getActiveEffects()) {
                        if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                            nearbyEntity.removeEffect(effect.getEffect());
                        }
                    }
                }
            }

        }

        return super.applyEffectTick(entity, amplifier);
    }


    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean isInstantenous() {
        // Este método puede ser útil para asegurarse de que el efecto no se trata como instantáneo
        return false;
    }





    @Override
    public boolean isBeneficial() {
        return inmuneMilk;
    }

    public static void getEffectTier1(Player player) {
        int duracionMaxima = 30 * 20 * 60; // 30 minutos en ticks
        if (player.hasEffect(AurumBlightEffect.getDelegate())) {
            MobEffectInstance efectoActual = player.getEffect(AurumBlightEffect.getDelegate());

            if (efectoActual != null) {
                if(efectoActual.getAmplifier() > 0){
                    efectoActual.getCures().clear();
                }
                // Verificar si la duración actual es menor que la máxima
                if (efectoActual.getDuration() < duracionMaxima && efectoActual.getAmplifier() == 0) {
                    // Modificar la duración del efecto existente
                    int nuevaDuracion = Math.min(efectoActual.getDuration() + 200, duracionMaxima);
                    if (nuevaDuracion == duracionMaxima) {
                        // Si la duración llega al máximo, aumentar el amplificador
                        efectoActual.update(ModEffects.createEffectInstance(AurumBlightEffect.getDelegate(), 2400, 1));
                        System.out.println("Amplificador actualizado");
                    } else {
                        efectoActual.update(ModEffects.createEffectInstance(AurumBlightEffect.getDelegate(), nuevaDuracion, 0));
                    }
                    //efectoActual.update(ModEffects.createEffectInstance(AurumBlightEffect.getDelegate(), nuevaDuracion, 0));
                    //System.out.println("Duración actualizada");
                }
            }
        } else {
            player.addEffect(ModEffects.createEffectInstance(AurumBlightEffect.getDelegate(), 200, 0));
        }
    }


}

