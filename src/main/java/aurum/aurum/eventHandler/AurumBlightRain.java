package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.effectsPlayer.AurumBlight;
import aurum.aurum.worldgen.Dimension.AurumSkyData;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

// CAMBIADO: antes era @EventBusSubscriber(Dist.DEDICATED_SERVER), que solo registra la clase en un servidor
// dedicado: en un mundo de un jugador (servidor integrado) la lluvia no hacía nada. El handler ya comprueba
// que el nivel es un ServerLevel, así que no hace falta limitar por Dist.
@EventBusSubscriber(modid = Aurum.MODID)
public class AurumBlightRain {
    // Lluvia FORZADA a mano con /Aurum startaurumrain y /Aurum stopaurumrain (afecta a todas las dimensiones).
    private static boolean shouldRain = false;
    private static int effectCooldown = 0;

    // Métodos existentes (se mantienen exactamente igual)
    public static void startAurumBlightRain() {
        shouldRain = true;
    }

    public static void stopAurumBlightRain() {
        shouldRain = false;
    }

    /**
     * ¿Llueve en este nivel? Sí si la lluvia está forzada por comando, o si es dim1 y el estado de la
     * noche es AURUM_RAIN. Así la lluvia del estado solo ocurre en su dimensión y se apaga sola al terminar.
     */
    public static boolean isRainingIn(ServerLevel level) {
        if (shouldRain) {
            return true;
        }
        return level.dimension() == AurumWeatherEvents.AURUM_DIMENSION_KEY
                && AurumSkyData.get(level).current() == AurumWeatherState.AURUM_RAIN;
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel world) || !isRainingIn(world)) return;

        // Optimización: Solo procesar cada 3 ticks (reduce carga del servidor)
        if (++effectCooldown < 3) return;
        effectCooldown = 0;

        for (ServerPlayer player : world.players()) {
            // 1. Detección de exposición: a cubierto no hay efecto
            if (!isExposedToRain(world, player.blockPosition())) continue;

            // 2. Aplicar efecto (tu lógica original)
            AurumBlight.getEffectTier1(player);
        }
        // Las partículas de la lluvia ya no se mandan desde aquí: las dibuja el cliente (AurumRainClient),
        // a partir del estado que le manda AurumSkySync.
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