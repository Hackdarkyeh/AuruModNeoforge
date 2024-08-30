package aurum.aurum.init;

import aurum.aurum.Aurum;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModEffects {
    private static final String AURUM_BLIGHT = "aurum_blight";
    public static final DeferredRegister<MobEffect> EFFECTS_REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, Aurum.MODID);

    public static final Holder<MobEffect> AurumBlightEffect = EFFECTS_REGISTRY.register(AURUM_BLIGHT,
            aurum.aurum.effectsPlayer.AurumBlight::new);


    public static MobEffectInstance createEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        // Access the actual MobEffect instance from the holder
        return new MobEffectInstance(effect, duration, amplifier);
    }

}
