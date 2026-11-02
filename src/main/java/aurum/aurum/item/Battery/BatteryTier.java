package aurum.aurum.item.Battery;

public enum BatteryTier {
    //          tier, capacidad, recibe/tick, entrega/tick
    BASIC(1,    1_000,    10,    10),
    ADVANCED(2, 20_000,  100,   100),
    ELITE(3,   200_000, 1_000, 1_000);

    public final int tier, capacity, maxReceive, maxExtract;

    BatteryTier(int tier, int capacity, int maxReceive, int maxExtract) {
        this.tier = tier; this.capacity = capacity;
        this.maxReceive = maxReceive; this.maxExtract = maxExtract;
    }

    public static BatteryTier of(int tier) {
        for (BatteryTier t : values()) if (t.tier == tier) return t;
        return BASIC;
    }

    public static final int MAX_TIER = 3;
}