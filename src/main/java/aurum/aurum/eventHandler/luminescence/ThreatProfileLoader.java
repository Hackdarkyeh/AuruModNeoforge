package aurum.aurum.eventHandler.luminescence;

import aurum.aurum.Aurum;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Tabla de amenaza por entidad, editable con datapack: data/aurum/luminescence/*.json
 * (cada archivo es un objeto o una lista de objetos; ver el JSON de ejemplo).
 * Sirve para lo que no sale de los atributos: proyectiles, explosiones y cadencias.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ThreatProfileLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * damage: daño por proyectil/ataque a distancia, antes de dificultad y armadura (0 = sin ataque a distancia).
     * interval: ticks entre ataques a distancia; intervalHard: lo mismo en difícil (-1 = igual que interval).
     * aurum: si el daño es de Aurum (lo absorbe la barrera e ignora la armadura vanilla).
     * meleeInterval: ticks entre golpes cuerpo a cuerpo (20 por defecto, 0 = no ataca cuerpo a cuerpo).
     */
    public record Profile(float damage, int interval, int intervalHard, boolean aurum, int meleeInterval) {
        public boolean hasRanged() { return damage > 0 && interval > 0; }

        public double rangedSeconds(Difficulty difficulty) {
            int ticks = difficulty == Difficulty.HARD && intervalHard > 0 ? intervalHard : interval;
            return Math.max(1, ticks) / 20.0;
        }
    }

    private record Entry(String entity, float damage, int interval, int intervalHard, boolean aurum, int meleeInterval) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("entity").forGetter(Entry::entity),
                Codec.FLOAT.optionalFieldOf("damage", 0F).forGetter(Entry::damage),
                Codec.INT.optionalFieldOf("interval_ticks", 0).forGetter(Entry::interval),
                Codec.INT.optionalFieldOf("interval_ticks_hard", -1).forGetter(Entry::intervalHard),
                Codec.BOOL.optionalFieldOf("aurum", false).forGetter(Entry::aurum),
                Codec.INT.optionalFieldOf("melee_interval_ticks", 20).forGetter(Entry::meleeInterval)
        ).apply(i, Entry::new));
    }

    private static volatile Map<EntityType<?>, Profile> byType = Map.of();
    private static volatile List<Map.Entry<TagKey<EntityType<?>>, Profile>> byTag = List.of();

    @Nullable
    public static Profile get(Entity e) {
        Profile p = byType.get(e.getType());
        if (p != null) return p;
        for (Map.Entry<TagKey<EntityType<?>>, Profile> en : byTag) {
            if (e.getType().is(en.getKey())) return en.getValue();
        }
        return null;
    }

    @SubscribeEvent
    public static void onAddReload(AddReloadListenerEvent event) {
        event.addListener(new ThreatProfileLoader());
    }

    public ThreatProfileLoader() {
        super(new Gson(), "luminescence");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<EntityType<?>, Profile> types = new HashMap<>();
        List<Map.Entry<TagKey<EntityType<?>>, Profile>> tags = new ArrayList<>();

        files.forEach((fileId, json) -> {
            if (json.isJsonArray()) json.getAsJsonArray().forEach(el -> parse(fileId, el, types, tags));
            else parse(fileId, json, types, tags);
        });

        byType = types;
        byTag = tags;
        LOGGER.info("Luminiscencia: {} entidades y {} tags con perfil de amenaza", types.size(), tags.size());
    }

    private static void parse(ResourceLocation file, JsonElement json, Map<EntityType<?>, Profile> types,
                              List<Map.Entry<TagKey<EntityType<?>>, Profile>> tags) {
        Entry entry = Entry.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> LOGGER.error("Luminiscencia: error en {}: {}", file, err))
                .orElse(null);
        if (entry == null) return;

        Profile profile = new Profile(entry.damage(), entry.interval(), entry.intervalHard(), entry.aurum(), entry.meleeInterval());
        boolean isTag = entry.entity().startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.entity().substring(1) : entry.entity());
        if (id == null) {
            LOGGER.error("Luminiscencia: id inválido '{}' en {}", entry.entity(), file);
            return;
        }

        if (isTag) {
            tags.add(new AbstractMap.SimpleEntry<>(TagKey.create(Registries.ENTITY_TYPE, id), profile));
        } else {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isPresent()) types.put(type.get(), profile);
            else LOGGER.warn("Luminiscencia: la entidad '{}' de {} no existe (¿id mal escrito o mod ausente?)", id, file);
        }
    }
}