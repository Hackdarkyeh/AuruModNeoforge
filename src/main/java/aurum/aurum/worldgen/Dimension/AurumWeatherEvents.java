package aurum.aurum.worldgen.Dimension;

import aurum.aurum.Aurum;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Locale;

/**
 * La noche fija se define con "fixed_time": 18000 en el dimension_type JSON,
 * así que no hace falta llamar a setDayTime() cada tick.
 *
 * El estado de la noche (STANDARD_NIGHT, DARK_NIGHT, ...) lo guarda AurumSkyData en el mundo; esta clase solo
 * lo hace avanzar cada tick y avisa a los jugadores cuando cambia.
 */
@EventBusSubscriber(modid = Aurum.MODID)
public class AurumWeatherEvents {

    public static final ResourceKey<Level> AURUM_DIMENSION_KEY = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dimension1"));

    public enum AurumWeatherState {
        STANDARD_NIGHT, DARK_NIGHT, NO_MOON, PURPLE_MOON, AURUM_RAIN;

        /** Los aldeanos se vuelven hostiles en todos los estados menos en la noche habitual. */
        public boolean isHostile() {
            return this != STANDARD_NIGHT;
        }

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        /** Clave del mensaje de aviso al empezar este estado (a rellenar en el archivo de idioma). */
        public String changeMessageKey() {
            return "aurum.sky.change." + id();
        }
    }

    /**
     * Copia en memoria del estado actual, solo en el servidor (para quien ya lo usaba).
     * En el cliente siempre vale STANDARD_NIGHT: el aviso a los clientes llega en un paso posterior.
     */
    private static volatile AurumWeatherState currentWeatherState = AurumWeatherState.STANDARD_NIGHT;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != AURUM_DIMENSION_KEY) return;

        AurumSkyData data = AurumSkyData.get(level);
        boolean changed = data.tick(level.getRandom());
        currentWeatherState = data.current();
        if (changed) {
            announce(level, data.current());
        }
    }

    /** Avisa a los jugadores que están en dim1. */
    public static void announce(ServerLevel level, AurumWeatherState state) {
        Component text = Component.translatable(state.changeMessageKey());
        level.players().forEach(player -> player.sendSystemMessage(text));
    }

    /** Se mantiene por compatibilidad. Mejor usar AurumSkyData.get(level).current(). */
    public static AurumWeatherState getCurrentWeatherState() {
        return currentWeatherState;
    }
}