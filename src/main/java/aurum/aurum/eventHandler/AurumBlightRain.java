package aurum.aurum.eventHandler;

import aurum.aurum.effectsPlayer.AurumBlight;
import aurum.aurum.init.ModParticles;
import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;


@EventBusSubscriber(Dist.DEDICATED_SERVER)
public class AurumBlightRain {

    private static boolean shouldRain = false; // Bandera para activar o desactivar la lluvia


    // Evento que se ejecuta en cada tick del mundo
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Pre event) {
        // Verificar que estamos en el lado del servidor y en la fase final del tick
        ServerLevel world = null;
        if (!event.getLevel().isClientSide()) {
            world = event.getLevel().getServer().getLevel(event.getLevel().dimension());

            if (!shouldRain) return; // Si no se debe activar la lluvia, salir

            // Generar partículas en posiciones aleatorias dentro del mundo
            for (int i = 0; i < world.players().size(); i++) {
                // Obtenemos al jugador y su posición actual
                ServerPlayer player = world.players().get(i);
                BlockPos playerPos = player.blockPosition();

                // Número de partículas a generar por tick en un área alrededor del jugador
                int particleCount = 10000;  // Ajusta este número para la densidad de la lluvia

                // Generar partículas alrededor del jugador
                for (int j = 0; j < particleCount; j++) {
                    // Calcula una posición aleatoria alrededor del jugador
                    double offsetX = (world.random.nextDouble() - 0.5) * 100; // Desplazamiento en X
                    double offsetZ = (world.random.nextDouble() - 0.5) * 100; // Desplazamiento en Z
                    double x = playerPos.getX() + offsetX;
                    double z = playerPos.getZ() + offsetZ;

                    // Obtener la altura del bloque más alto en la posición (x, z)
                    double terrainHeight = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int) x, 0, (int) z)).getY();

                    // Generar las partículas a una altura mayor que el terreno
                    double y = terrainHeight + 10 + (world.random.nextDouble() * 60);  // Altura variable por encima del terreno

                    // Verifica que no haya un techo sólido (un bloque opaco) en la trayectoria de la partícula
                    BlockPos particlePos = new BlockPos((int) x, (int) y, (int) z);
                    boolean hasSkyAccess = world.canSeeSky(particlePos);

                    // Si la partícula tiene acceso al cielo, generar la partícula
                    if (hasSkyAccess) {
                        world.sendParticles(ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(),
                                x, y, z,  // Posición
                                1,  // Número de partículas
                                0.0, -0.2, 0.0,  // Velocidad en X, Y (negativa para caer), Z
                                0.1  // Velocidad de la partícula
                        );
                    }
                }
            }
            // Aplicar el efecto de regeneración a los jugadores que estén expuestos a la lluvia
            for (ServerPlayer player : world.players()) {
                if (player.onGround() && isExposedToRain(world, player.blockPosition())) {
                    AurumBlight.getEffectTier1(player);
                }
            }
        }


    }

    private static boolean isExposedToRain(ServerLevel world, BlockPos pos) {
        // Verifica si se puede ver el cielo desde ahí.
        return world.canSeeSky(pos);
    }


    // Método para activar la lluvia personalizada
    public static void startAurumBlightRain() {
        shouldRain = true;
    }

    // Método para detener la lluvia personalizada
    public static void stopAurumBlightRain() {
        shouldRain = false;
    }
}

