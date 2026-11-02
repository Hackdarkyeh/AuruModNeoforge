package aurum.aurum.client;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Lluvia morada dibujada en el CLIENTE. Sustituye a las partículas que antes mandaba el servidor
 * (8.000-12.000 paquetes cada 3 ticks por jugador). El servidor solo decide si llueve y aplica el efecto.
 *
 * Solo existe en el cliente (Dist.CLIENT).
 */
@EventBusSubscriber(modid = Aurum.MODID, value = Dist.CLIENT)
public class AurumRainClient {

    /** Partículas nuevas por tick. Punto de partida: sube o baja según la densidad que quieras ver. */
    public static final int PARTICLES_PER_TICK = 80;
    /** Radio alrededor del jugador en el que aparecen. */
    private static final int RADIUS = 40;
    /** Altura de aparición respecto al jugador. */
    private static final double MIN_ABOVE = 2.0;
    private static final double MAX_ABOVE = 26.0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!AurumSkyClient.isRaining()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            return;
        }

        RandomSource random = level.random;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * RADIUS; // reparto uniforme en el disco
            double x = px + Math.cos(angle) * distance;
            double z = pz + Math.sin(angle) * distance;
            double y = py + MIN_ABOVE + random.nextDouble() * (MAX_ABOVE - MIN_ABOVE);

            // Solo donde se ve el cielo: bajo techo no llueve.
            if (!level.canSeeSky(BlockPos.containing(x, y, z))) {
                continue;
            }
            // Misma velocidad que antes: (-0.02, -0.5, 0.02) * 0.5
            level.addParticle(ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(), x, y, z, -0.01, -0.25, 0.01);
        }
    }
}