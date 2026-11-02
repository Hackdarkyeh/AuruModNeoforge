package aurum.aurum.worldgen.Dimension;

import aurum.aurum.worldgen.Dimension.AurumSkyData;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Periodos tranquilo y hostil de los aldeanos.
 *
 * En dim1 salen del estado de la noche (AurumSkyData): tranquilo en STANDARD_NIGHT y hostil en el resto.
 * En cualquier otra dimensión (por ejemplo un superflat para probar pueblos) se usa un ciclo simple por
 * tiempo de juego: CALM_TICKS tranquilos y HOSTILE_TICKS hostiles.
 *
 * /aurumperiod calm|hostile|auto fuerza el periodo en todas las dimensiones (solo para probar).
 */
public final class AurumNightCycle {

    public static final long CALM_TICKS = 12000L;
    public static final long HOSTILE_TICKS = 12000L;
    private static final long CYCLE_TICKS = CALM_TICKS + HOSTILE_TICKS;

    /** null = automático. TRUE/FALSE = forzado con /aurumperiod. */
    @Nullable
    private static volatile Boolean forced = null;

    private AurumNightCycle() {}

    private static boolean isAurumDimension(Level level) {
        return level instanceof ServerLevel && level.dimension() == AurumWeatherEvents.AURUM_DIMENSION_KEY;
    }

    public static boolean isHostilePeriod(Level level) {
        Boolean f = forced;
        if (f != null) {
            return f;
        }
        if (isAurumDimension(level)) {
            AurumWeatherState state = AurumSkyData.get((ServerLevel) level).current();
            return state.isHostile();
        }
        return Math.floorMod(level.getGameTime(), CYCLE_TICKS) >= CALM_TICKS;
    }

    /**
     * En PURPLE_MOON la divine_armor se retira y deja de proteger el pueblo, aunque siga viva.
     * (Con el periodo forzado por comando no se aplica, para poder probar la protección.)
     */
    public static boolean isProtectionSuspended(Level level) {
        if (forced != null || !isAurumDimension(level)) {
            return false;
        }
        return AurumSkyData.get((ServerLevel) level).current() == AurumWeatherState.PURPLE_MOON;
    }

    /** Ticks que faltan para el siguiente cambio de periodo en modo automático. */
    public static long ticksUntilChange(Level level) {
        if (isAurumDimension(level)) {
            return AurumSkyData.get((ServerLevel) level).ticksRemaining();
        }
        long t = Math.floorMod(level.getGameTime(), CYCLE_TICKS);
        return t >= CALM_TICKS ? CYCLE_TICKS - t : CALM_TICKS - t;
    }

    public static void force(@Nullable Boolean hostile) {
        forced = hostile;
    }

    @Nullable
    public static Boolean getForced() {
        return forced;
    }
}