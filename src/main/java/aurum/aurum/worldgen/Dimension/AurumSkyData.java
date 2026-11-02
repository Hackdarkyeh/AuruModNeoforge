package aurum.aurum.worldgen.Dimension;

import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Estado de la "noche" de dim1, guardado en el mundo (data/aurum_sky.dat de esa dimensión).
 *
 * Antes el estado vivía en variables estáticas y se perdía al cerrar el mundo. Ahora se guarda el estado
 * actual, los ticks que le quedan y el SIGUIENTE estado, que se decide al empezar el actual (así se puede
 * anticipar: presagios, contador de radiación, etc.).
 *
 * Las reglas de transición son las de tu AurumWeatherEvents original.
 */
public class AurumSkyData extends SavedData {

    public static final String NAME = "aurum_sky";
    /** Duración de cada estado (la misma que tenía el contador original). */
    public static final int STATE_TICKS = 24000;

    private AurumWeatherState current = AurumWeatherState.STANDARD_NIGHT;
    private AurumWeatherState next = null;
    private int ticksRemaining = STATE_TICKS;

    // ---------- Acceso ----------
    public static AurumSkyData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AurumSkyData::new, AurumSkyData::load), NAME);
    }

    public AurumWeatherState current() {
        return current;
    }

    /** Siguiente estado ya decidido (null solo antes del primer tick). */
    public AurumWeatherState next() {
        return next;
    }

    public int ticksRemaining() {
        return ticksRemaining;
    }

    // ---------- Reglas de transición (las del AurumWeatherEvents original) ----------
    public static AurumWeatherState chooseNext(AurumWeatherState from, RandomSource random) {
        return switch (from) {
            case STANDARD_NIGHT -> AurumWeatherState.DARK_NIGHT;
            case DARK_NIGHT -> AurumWeatherState.NO_MOON;
            case NO_MOON -> random.nextFloat() < 0.2F ? AurumWeatherState.PURPLE_MOON : AurumWeatherState.STANDARD_NIGHT;
            case PURPLE_MOON -> random.nextFloat() < 0.1F ? AurumWeatherState.AURUM_RAIN : AurumWeatherState.STANDARD_NIGHT;
            case AURUM_RAIN -> AurumWeatherState.STANDARD_NIGHT;
        };
    }

    // ---------- Avance ----------
    /** Avanza un tick. Devuelve true si acaba de cambiar el estado actual. */
    public boolean tick(RandomSource random) {
        if (next == null) {
            next = chooseNext(current, random);
        }
        ticksRemaining--;
        boolean changed = false;
        if (ticksRemaining <= 0) {
            current = next;
            next = chooseNext(current, random);
            ticksRemaining = STATE_TICKS;
            changed = true;
        }
        this.setDirty();
        return changed;
    }

    /** Para los comandos de prueba: cambia ya el estado actual y reinicia su duración. */
    public void forceCurrent(AurumWeatherState state, RandomSource random) {
        current = state;
        next = chooseNext(state, random);
        ticksRemaining = STATE_TICKS;
        this.setDirty();
    }

    /** Para los comandos de prueba: fija el siguiente estado. */
    public void forceNext(AurumWeatherState state) {
        next = state;
        this.setDirty();
    }

    // ---------- Guardado ----------
    public static AurumSkyData load(CompoundTag tag, HolderLookup.Provider registries) {
        AurumSkyData data = new AurumSkyData();
        data.current = parse(tag.getString("Current"), AurumWeatherState.STANDARD_NIGHT);
        data.next = tag.contains("Next") ? parse(tag.getString("Next"), null) : null;
        data.ticksRemaining = tag.contains("TicksRemaining") ? tag.getInt("TicksRemaining") : STATE_TICKS;
        return data;
    }

    private static AurumWeatherState parse(String name, AurumWeatherState fallback) {
        try {
            return AurumWeatherState.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putString("Current", current.name());
        if (next != null) {
            tag.putString("Next", next.name());
        }
        tag.putInt("TicksRemaining", ticksRemaining);
        return tag;
    }
}