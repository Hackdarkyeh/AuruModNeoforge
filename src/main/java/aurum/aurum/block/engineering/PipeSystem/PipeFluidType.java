package aurum.aurum.block.engineering.PipeSystem;

import net.minecraft.util.StringRepresentable;

public enum PipeFluidType implements StringRepresentable {
    EMPTY(0),          // Sin energía
    ENERGY(10),        // Energía normal
    DARK_ENERGY(20),   // Energía oscura
    AURELITE(15);       // Esencia mágica (ejemplo)

    private final int weight;

    PipeFluidType(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return this.weight;
    }

    // Retorna el tipo con mayor peso
    public static PipeFluidType getHeavier(PipeFluidType a, PipeFluidType b) {
        return a.weight >= b.weight ? a : b;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}

