package aurum.aurum.item.ArmorItem;

import net.minecraft.world.item.Item;

public class ArmorExpansions extends Item {
    private final String name;
    private final int weight;

    public ArmorExpansions(int peso, String name) {
        super(new Properties());
        this.weight = peso;
        this.name = name;
    }

    public String getName() { return name; }
    public int getWeight() { return weight; }
}
