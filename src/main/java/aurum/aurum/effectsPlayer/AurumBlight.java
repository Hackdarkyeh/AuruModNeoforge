package aurum.aurum.effectsPlayer;

import aurum.aurum.eventHandler.ArmorEvents.ArmorBlightShield;
import aurum.aurum.init.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static aurum.aurum.init.ModEffects.AurumBlightEffect;

public class AurumBlight extends MobEffect {

    public AurumBlight() {
        super(MobEffectCategory.HARMFUL, -26113);
    }

    @Override
    public String getDescriptionId() {
        return "effect.aurum.aurum_blight";
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;

        // 1. Aurum se come los efectos del portador (menos él mismo), salvo que la barrera aguante
        devourEffects(entity);

        // 2. Un solo golpe; ArmorBlightShield lo intercepta (barrera -> sobrante al jugador)
        DamageSource src = AurumBlightDamageSource.createAurumBlightDamageSource(entity.level().registryAccess());
        entity.hurt(src, 2.0F * (amplifier + 1));

        // 3. Quita los efectos beneficiosos a las entidades cercanas
        stripNearbyBeneficialEffects(entity);
        return true;
    }

    /**
     * Aurum se come los efectos de la entidad (todos menos el propio Aurum), salvo que la barrera aguante.
     * Sacado de applyEffectTick para que lo reutilicen los golpes de la exposición a Aurum.
     */
    public static void devourEffects(LivingEntity entity) {
        if (ArmorBlightShield.hasBarrier(entity)) return;
        List<Holder<MobEffect>> eaten = new ArrayList<>();
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            if (!(inst.getEffect().value() instanceof AurumBlight)) eaten.add(inst.getEffect());
        }
        eaten.forEach(entity::removeEffect);
    }

    /** Quita los efectos beneficiosos a las entidades cercanas (5 bloques). También lo reutiliza la exposición. */
    public static void stripNearbyBeneficialEffects(LivingEntity entity) {
        for (LivingEntity near : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(5))) {
            if (near == entity) continue;
            for (MobEffectInstance e : List.copyOf(near.getActiveEffects())) {
                if (e.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                    near.removeEffect(e.getEffect());
                }
            }
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }

    public static void getEffectTier1(Player player) {
        final int max = 30 * 20 * 60;
        Holder<MobEffect> eff = AurumBlightEffect.getDelegate();

        MobEffectInstance cur = player.getEffect(eff);
        if (cur == null) {
            player.addEffect(ModEffects.createEffectInstance(eff, 200, 0));
            return;
        }
        if (cur.getAmplifier() > 0) {
            cur.getCures().clear();
            return;
        }
        int next = cur.getDuration() + 200;
        if (next >= max) {
            player.addEffect(ModEffects.createEffectInstance(eff, 2400, 1));
            MobEffectInstance applied = player.getEffect(eff);
            if (applied != null) applied.getCures().clear(); // Tier 1: la leche ya no lo cura
        } else {
            player.addEffect(ModEffects.createEffectInstance(eff, next, 0));
        }
    }

    public static void applyCorruptedBlight(LivingEntity target, double aurumValue) {
        final int max = 30 * 20 * 60;
        int added = 100 + (int) (aurumValue * 40);
        Holder<MobEffect> eff = AurumBlightEffect.getDelegate();

        MobEffectInstance cur = target.getEffect(eff);
        if (cur == null) {
            target.addEffect(ModEffects.createEffectInstance(eff, added, 0));
            return;
        }

        if (cur.getAmplifier() > 0) {
            cur.getCures().clear();
            return;
        }

        if (aurumValue >= 10.0D || (long) cur.getDuration() + added >= max) {
            target.addEffect(ModEffects.createEffectInstance(eff, 2400, 1));
            MobEffectInstance applied = target.getEffect(eff);
            if (applied != null) applied.getCures().clear();
        } else {
            target.addEffect(ModEffects.createEffectInstance(eff, cur.getDuration() + added, 0));
        }
    }

    // En AurumBlight
    public static void applyFromEnchant(LivingEntity target, int enchantLevel) {
        int amp = enchantLevel >= 4 ? enchantLevel - 1 : 0;  // IV → Aurum IV, V → Aurum V
        int add = enchantLevel * 120;                         // 6 s por nivel → V = 30 s por golpe

        MobEffectInstance cur = target.getEffect(AurumBlightEffect.getDelegate());
        long duration = add;
        if (cur != null) {
            if (cur.getAmplifier() > amp) return;             // no degradar un Aurum más fuerte
            if (cur.getAmplifier() == amp) duration = (long) cur.getDuration() + add;
        }
        duration = Math.min(duration, 1_000_000_000L);

        target.addEffect(ModEffects.createEffectInstance(
                AurumBlightEffect.getDelegate(), (int) duration, amp));

        if (amp > 0) {                                        // IV y V no se curan con leche
            MobEffectInstance applied = target.getEffect(AurumBlightEffect.getDelegate());
            if (applied != null) applied.getCures().clear();
        }
    }
}