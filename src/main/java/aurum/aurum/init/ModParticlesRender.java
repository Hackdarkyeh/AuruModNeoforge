package aurum.aurum.init;

import aurum.aurum.particle.AurumBlightRainParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticlesRender {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(), AurumBlightRainParticle.AurumBlightRainParticleProvider::new);
    }
}
