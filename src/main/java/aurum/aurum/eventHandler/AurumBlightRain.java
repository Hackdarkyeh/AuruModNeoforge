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
    private static boolean shouldRain = false;
    private static int particleSpawnCooldown = 0;

    // Métodos existentes (se mantienen exactamente igual)
    public static void startAurumBlightRain() {
        shouldRain = true;
    }

    public static void stopAurumBlightRain() {
        shouldRain = false;
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Pre event) {
        if (!shouldRain || !(event.getLevel() instanceof ServerLevel world)) return;

        // Optimización: Solo procesar cada 3 ticks (reduce carga del servidor)
        if (++particleSpawnCooldown < 3) return;
        particleSpawnCooldown = 0;

        // Radio alrededor del jugador para spawnear partículas
        final int rainRadius = 50;
        // Altura mínima/máxima de las partículas
        final double minHeight = 10.0;
        final double maxHeight = 70.0;

        for (ServerPlayer player : world.players()) {
            BlockPos playerPos = player.blockPosition();

            // 1. Sistema mejorado de detección de exposición
            if (!isExposedToRain(world, playerPos)) continue;

            // 2. Aplicar efecto (manteniendo tu lógica original)
            AurumBlight.getEffectTier1(player);

            // 3. Nuevo sistema optimizado de partículas
            spawnAurumRainParticles(world, playerPos, rainRadius, minHeight, maxHeight);
        }
    }

    private static void spawnAurumRainParticles(ServerLevel world, BlockPos center, int radius, double minH, double maxH) {
        int particleCount = getParticleCountForRainIntensity();

        for (int i = 0; i < particleCount; i++) {
            // Distribución más uniforme alrededor del jugador
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = world.random.nextDouble() * radius;

            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            // Altura basada en el terreno
            int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
            double y = surfaceY + minH + world.random.nextDouble() * (maxH - minH);

            BlockPos particlePos = new BlockPos((int)x, (int)y, (int)z);

            // Solo si tiene línea de visión al cielo
            if (world.canSeeSky(particlePos)) {
                // Parámetros ajustados para mejor visualización
                world.sendParticles(
                        ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(),
                        x, y, z,         // Posición
                        0,                // Count (0 para partículas únicas)
                        -0.02, -0.5, 0.02, // Velocidad (movimiento aleatorio suave)
                        0.5               // Tamaño/scale
                );
            }
        }
    }

    // Método auxiliar para partículas en el suelo (splash)
    private static void spawnSplashParticles(ServerLevel world, BlockPos pos) {
        if (world.random.nextInt(3) == 0) { // 33% de chance de spawnear splash
            world.sendParticles(
                    ModParticles.AURUM_BLIGHT_PARTICLE_RAIN.get(), // Necesitarás registrar este tipo
                    pos.getX() + 0.5,
                    pos.getY() + 0.1,
                    pos.getZ() + 0.5,
                    2, // Cantidad
                    0.1, 0, 0.1, // Spread
                    0.2 // Velocidad
            );
        }
    }

    // Método auxiliar para calcular cantidad de partículas
    private static int getParticleCountForRainIntensity() {
        // Base + variación aleatoria
        return 8000 + (int)(Math.random() * 4000);
    }

    // Método mejorado para detectar exposición (se mantiene igual el header)
    private static boolean isExposedToRain(ServerLevel world, BlockPos pos) {
        // Verifica en un área 3x3 alrededor de la posición del jugador
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                if (world.canSeeSky(checkPos) && world.isEmptyBlock(checkPos.above())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Nuevo método opcional para verificar estado (sin modificar los existentes)
    public static boolean isAurumRainActive() {
        return shouldRain;
    }
}