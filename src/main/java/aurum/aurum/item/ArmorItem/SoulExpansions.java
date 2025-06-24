package aurum.aurum.item.ArmorItem;

import net.minecraft.world.item.Item;

public class SoulExpansions extends Item {
    private final int idTier;
    private final int honor;
    private final int maxWight;
    private ArmorExpansions[] abilities = new ArmorExpansions[8];


    public SoulExpansions(int idTier, int honor, int maxWight) {
        super(new Properties());
        this.idTier = idTier;
        this.honor = honor;
        this.maxWight = maxWight;
    }

    public int getPeso() { return honor; }
    public int getMaxWight() { return maxWight; }
    public int getId() { return idTier; }


    private int count = 0;

    private int getCurrentWeight() {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += abilities[i].getWeight(); // Asumimos que ArmorExpansions tiene getWeight()
        }
        return total;
    }

    public boolean addAbility(ArmorExpansions newAbility) {
        if (count >= 8) {
            System.out.println("Cannot add more abilities. Limit reached.");
            return false;
        }
        for (int i = 0; i < count; i++) {
            if (abilities[i].equals(newAbility)) {
                System.out.println("Ability already exists.");
                return false;
            }
        }
        int projectedWeight = getCurrentWeight() + newAbility.getWeight();
        if (projectedWeight > maxWight) {
            System.out.println("Cannot add ability. Weight limit exceeded.");
            return false;
        }

        abilities[count++] = newAbility;
        return true;
    }

    public boolean removeAbility(String name) {
        for (int i = 0; i < count; i++) {
            if (abilities[i].getName().equalsIgnoreCase(name)) {
                // Shift the rest to the left
                for (int j = i; j < count - 1; j++) {
                    abilities[j] = abilities[j + 1];
                }
                abilities[--count] = null;
                return true;
            }
        }
        System.out.println("Ability not found.");
        return false;
    }

    public void listAbilities() {
        System.out.println("Current abilities:");
        for (int i = 0; i < count; i++) {
            System.out.println("- " + abilities[i]);
        }
    }
}
