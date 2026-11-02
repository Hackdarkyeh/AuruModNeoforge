package aurum.aurum.entity.AurumVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registro de pueblos de un nivel (se guarda en data/aurum_villages.dat de esa dimensión).
 *
 * Un pueblo nace cuando entra en el nivel su divine_armor (la que genera el town_center) y se identifica
 * por el UUID de esa divine_armor. El registro guarda dónde está el centro, si la divine_armor sigue viva y,
 * por cada jugador, cuántos aldeanos de ese pueblo ha matado (reputación).
 */
public class AurumVillageData extends SavedData {

    public static final String NAME = "aurum_villages";

    /** Distancia horizontal máxima entre un aldeano y el centro de su pueblo. */
    public static final double VILLAGE_RADIUS = 96.0;

    /** Recargo de los alquileres por cada aldeano del pueblo que el jugador ha matado (+25 % cada uno). */
    public static final double PRICE_PENALTY_PER_KILL = 0.25;
    /** Tope del multiplicador de precio (x3). */
    public static final double MAX_PRICE_MULTIPLIER = 3.0;

    public static final class Village {
        public final UUID id;
        public final BlockPos center;
        public boolean golemAlive = true;
        /** Aldeanos de este pueblo que ha matado cada jugador. */
        public final Map<UUID, Integer> kills = new HashMap<>();

        Village(UUID id, BlockPos center) {
            this.id = id;
            this.center = center;
        }
    }

    private final Map<UUID, Village> villages = new HashMap<>();

    // ---------- Acceso ----------
    public static AurumVillageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AurumVillageData::create, AurumVillageData::load), NAME);
    }

    public static AurumVillageData create() {
        return new AurumVillageData();
    }

    // ---------- Pueblos ----------
    /** Registra un pueblo nuevo si esa divine_armor aún no tiene el suyo. */
    public void registerGolemIfAbsent(UUID golemId, BlockPos center) {
        if (!villages.containsKey(golemId)) {
            villages.put(golemId, new Village(golemId, center.immutable()));
            this.setDirty();
        }
    }

    /** La divine_armor de ese pueblo ha muerto o se ha retirado. */
    public void markGolemGone(UUID golemId) {
        Village v = villages.get(golemId);
        if (v != null && v.golemAlive) {
            v.golemAlive = false;
            this.setDirty();
        }
    }

    @Nullable
    public Village get(UUID id) {
        return villages.get(id);
    }

    /** Pueblo más cercano (en horizontal) dentro de VILLAGE_RADIUS, o null. */
    @Nullable
    public Village findNearest(BlockPos pos) {
        Village best = null;
        double bestDist = VILLAGE_RADIUS * VILLAGE_RADIUS;
        for (Village v : villages.values()) {
            double dx = v.center.getX() - pos.getX();
            double dz = v.center.getZ() - pos.getZ();
            double d = dx * dx + dz * dz;
            if (d <= bestDist) {
                bestDist = d;
                best = v;
            }
        }
        return best;
    }

    // ---------- Reputación ----------
    /** Anota que el jugador ha matado a un aldeano del pueblo. Devuelve el total de muertes. */
    public int addKill(UUID villageId, UUID player) {
        Village v = villages.get(villageId);
        if (v == null) {
            return 0;
        }
        int total = v.kills.merge(player, 1, Integer::sum);
        this.setDirty();
        return total;
    }

    public int getKills(UUID villageId, UUID player) {
        Village v = villages.get(villageId);
        return v == null ? 0 : v.kills.getOrDefault(player, 0);
    }

    /** Enemigo permanente del pueblo: ha matado al menos a un aldeano suyo. No caduca. */
    public boolean isHostileTo(UUID villageId, UUID player) {
        return getKills(villageId, player) > 0;
    }

    /** Multiplicador que se aplicará al precio de los alquileres de este pueblo para ese jugador (Fase 5). */
    public double priceMultiplier(UUID villageId, UUID player) {
        return Math.min(MAX_PRICE_MULTIPLIER, 1.0 + PRICE_PENALTY_PER_KILL * getKills(villageId, player));
    }

    /** Solo para pruebas (/Aurum village forgive): borra la reputación del jugador en ese pueblo. */
    public void forgive(UUID villageId, UUID player) {
        Village v = villages.get(villageId);
        if (v != null && v.kills.remove(player) != null) {
            this.setDirty();
        }
    }

    // ---------- Guardado ----------
    public static AurumVillageData load(CompoundTag tag, HolderLookup.Provider registries) {
        AurumVillageData data = create();
        ListTag list = tag.getList("Villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                UUID id = UUID.fromString(t.getString("Id"));
                Village v = new Village(id, new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z")));
                v.golemAlive = t.getBoolean("GolemAlive");
                ListTag killsTag = t.getList("Kills", Tag.TAG_COMPOUND);
                for (int k = 0; k < killsTag.size(); k++) {
                    CompoundTag kt = killsTag.getCompound(k);
                    try {
                        v.kills.put(UUID.fromString(kt.getString("Player")), kt.getInt("Count"));
                    } catch (IllegalArgumentException ignored) {
                        // jugador corrupto: se descarta
                    }
                }
                data.villages.put(id, v);
            } catch (IllegalArgumentException ignored) {
                // entrada corrupta: se descarta
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        for (Village v : villages.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", v.id.toString());
            t.putInt("X", v.center.getX());
            t.putInt("Y", v.center.getY());
            t.putInt("Z", v.center.getZ());
            t.putBoolean("GolemAlive", v.golemAlive);
            ListTag killsTag = new ListTag();
            for (Map.Entry<UUID, Integer> e : v.kills.entrySet()) {
                CompoundTag kt = new CompoundTag();
                kt.putString("Player", e.getKey().toString());
                kt.putInt("Count", e.getValue());
                killsTag.add(kt);
            }
            t.put("Kills", killsTag);
            list.add(t);
        }
        tag.put("Villages", list);
        return tag;
    }
}