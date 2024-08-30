package aurum.aurum.init;



import aurum.aurum.Aurum;
import aurum.aurum.particle.AurumBlightRainParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.function.Supplier;


public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Aurum.MODID);

    public static final Supplier<SimpleParticleType> AURUM_BLIGHT_PARTICLE_RAIN = REGISTRY.register("aurum_blight_rain_particle", () -> new SimpleParticleType(false));


    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(), AurumBlightRainParticle::provider);
    }
}
