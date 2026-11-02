package aurum.aurum.client;

import aurum.aurum.Red.SyncAurumSkyPayload;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import org.jetbrains.annotations.Nullable;

/**
 * Lo que el cliente sabe del estado de la noche de dim1 (se rellena con SyncAurumSkyPayload).
 * No importa nada exclusivo del cliente, así que es seguro referenciarla desde ModNetworking.
 * Aquí se apoyarán el cielo, los presagios, la lluvia en el cliente y el Geiger.
 */
public final class AurumSkyClient {

    private static boolean inAurum = false;
    private static int state = 0;
    private static int next = -1;
    private static int ticksRemaining = 0;
    private static boolean raining = false;
    private static long receivedAtGameTime = 0L;

    private AurumSkyClient() {}

    public static void update(SyncAurumSkyPayload payload, long clientGameTime) {
        inAurum = payload.inAurum();
        state = payload.state();
        next = payload.next();
        ticksRemaining = payload.ticksRemaining();
        raining = payload.raining();
        receivedAtGameTime = clientGameTime;
    }

    public static boolean isInAurum() {
        return inAurum;
    }

    public static boolean isRaining() {
        return raining;
    }

    /** Estado actual de la noche, o null si el jugador no está en dim1. */
    @Nullable
    public static AurumWeatherState getState() {
        return inAurum ? byOrdinal(state) : null;
    }

    /** Siguiente estado, o null si no está en dim1 o aún no se ha decidido. */
    @Nullable
    public static AurumWeatherState getNext() {
        return inAurum ? byOrdinal(next) : null;
    }

    /** Ticks estimados que le quedan al estado actual (descuenta lo transcurrido desde el último paquete). */
    public static int getTicksRemaining(long nowGameTime) {
        long elapsed = Math.max(0L, nowGameTime - receivedAtGameTime);
        return (int) Math.max(0L, ticksRemaining - elapsed);
    }

    @Nullable
    private static AurumWeatherState byOrdinal(int ordinal) {
        AurumWeatherState[] values = AurumWeatherState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}