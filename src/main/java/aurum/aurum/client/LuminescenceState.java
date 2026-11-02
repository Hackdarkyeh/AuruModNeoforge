package aurum.aurum.client;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Entidades que el jugador local debe ver brillando y su nivel de amenaza (0 verde, 1 amarillo, 2 rojo).
 * Sin imports de cliente a propósito: el paquete la toca desde código común. Los mixins la consultan por frame.
 */
public final class LuminescenceState {
    private LuminescenceState() {}

    public static final int[] COLORS = {0x55FF55, 0xFFFF55, 0xFF5555};

    private static volatile Int2IntOpenHashMap map = new Int2IntOpenHashMap();
    public static void set(int[] ids, byte[] levels) {
        Int2IntOpenHashMap m = new Int2IntOpenHashMap(ids.length);
        for (int i = 0; i < ids.length; i++) m.put(ids[i], levels[i]);
        map = m; // se sustituye entera: el render nunca ve un mapa a medias
    }

    public static void clear() {
        map = new Int2IntOpenHashMap();
    }

    public static boolean contains(int entityId) {
        Int2IntOpenHashMap m = map;
        return !m.isEmpty() && m.containsKey(entityId);
    }

    /** Color RGB del contorno (sin alfa). */
    public static int color(int entityId) {
        int level = map.getOrDefault(entityId, 0);
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, level))];
    }
}