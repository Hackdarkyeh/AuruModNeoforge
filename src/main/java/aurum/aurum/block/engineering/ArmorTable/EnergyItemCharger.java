package aurum.aurum.block.engineering.ArmorTable;

import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyWeapon;
import aurum.aurum.item.ArmorItem.AurumArmor;
import net.minecraft.world.item.ItemStack;

/**
 * Carga de ítems con energía para la ArmorTable. Funciona con cualquier ítem que sea una armadura del mod
 * (AurumArmor), implemente IEnergyArmor o implemente IEnergyWeapon.
 *
 * Siempre se intenta primero la energía oscura y después la limpia. Un ítem solo lleva un tipo de energía
 * a la vez: si ya tiene uno, el otro no entra.
 */
public final class EnergyItemCharger {
    private EnergyItemCharger() {}

    /** Energía que ha salido de cada tanque de la mesa en una carga. */
    public record Result(int dark, int clean) {
        public static final Result NONE = new Result(0, 0);

        public boolean any() {
            return dark > 0 || clean > 0;
        }
    }

    /** ¿Lleva energía y sabemos cargarlo? */
    public static boolean isEnergyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return AurumArmor.is(stack)
                || stack.getItem() instanceof IEnergyArmor
                || stack.getItem() instanceof IEnergyWeapon;
    }

    /**
     * Carga el ítem con la energía disponible, como máximo {@code rate} de cada tipo por llamada.
     *
     * @return la energía realmente consumida de cada tanque
     */
    public static Result charge(ItemStack stack, int availableDark, int availableClean, int rate) {
        if (stack.isEmpty() || rate <= 0) return Result.NONE;

        if (AurumArmor.is(stack)) {
            return chargeAurumArmor(stack, availableDark, availableClean, rate);
        }
        if (stack.getItem() instanceof IEnergyArmor armor) {
            int dark = addToArmor(stack, armor, IEnergyArmor.EnergyType.DARK_ENERGY, Math.min(rate, availableDark));
            int clean = addToArmor(stack, armor, IEnergyArmor.EnergyType.CLEAN_ENERGY, Math.min(rate, availableClean));
            return new Result(dark, clean);
        }
        if (stack.getItem() instanceof IEnergyWeapon weapon) {
            int dark = addToWeapon(stack, weapon, IEnergyWeapon.EnergyType.DARK_ENERGY, Math.min(rate, availableDark));
            int clean = addToWeapon(stack, weapon, IEnergyWeapon.EnergyType.CLEAN_ENERGY, Math.min(rate, availableClean));
            return new Result(dark, clean);
        }
        return Result.NONE;
    }

    // --- Armaduras del mod (componentes) ---

    private static Result chargeAurumArmor(ItemStack stack, int availableDark, int availableClean, int rate) {
        int usedDark = 0;
        int usedClean = 0;

        int darkAmount = Math.min(rate, availableDark);
        if (darkAmount > 0 && AurumArmor.getEnergy(stack) < AurumArmor.getMaxEnergy(stack)) {
            int before = AurumArmor.getEnergy(stack);
            if (AurumArmor.addEnergyOfType(stack, IEnergyArmor.EnergyType.DARK_ENERGY, darkAmount)) {
                usedDark = AurumArmor.getEnergy(stack) - before;
            }
        }

        int cleanAmount = Math.min(rate, availableClean);
        if (cleanAmount > 0 && AurumArmor.getEnergy(stack) < AurumArmor.getMaxEnergy(stack)) {
            int before = AurumArmor.getEnergy(stack);
            if (AurumArmor.addEnergyOfType(stack, IEnergyArmor.EnergyType.CLEAN_ENERGY, cleanAmount)) {
                usedClean = AurumArmor.getEnergy(stack) - before;
            }
        }
        return new Result(usedDark, usedClean);
    }

    // --- Ítems que implementan IEnergyArmor ---

    private static int addToArmor(ItemStack stack, IEnergyArmor item, IEnergyArmor.EnergyType type, int amount) {
        if (amount <= 0 || !item.canReceiveEnergy(stack)) return 0;
        if (item.getCurrentEnergy(stack) >= item.getMaxEnergy(stack)) return 0;

        IEnergyArmor.EnergyType current = item.getCurrentEnergyType(stack);
        if (current != IEnergyArmor.EnergyType.NONE && current != type) return 0;
        if (current == IEnergyArmor.EnergyType.NONE) item.setEnergyType(stack, type);

        int before = item.getCurrentEnergy(stack);
        item.addEnergy(stack, amount);
        int added = item.getCurrentEnergy(stack) - before;

        // Si no entró nada, dejamos el tipo como estaba
        if (added <= 0 && current == IEnergyArmor.EnergyType.NONE) {
            item.setEnergyType(stack, IEnergyArmor.EnergyType.NONE);
        }
        return Math.max(added, 0);
    }

    // --- Ítems que implementan IEnergyWeapon ---

    private static int addToWeapon(ItemStack stack, IEnergyWeapon item, IEnergyWeapon.EnergyType type, int amount) {
        if (amount <= 0 || !item.canReceiveEnergy(stack)) return 0;
        if (item.getCurrentEnergy(stack) >= item.getMaxEnergy(stack)) return 0;

        boolean allowed = type == IEnergyWeapon.EnergyType.DARK_ENERGY
                ? item.canUseDarkEnergy(stack)
                : item.canUseCleanEnergy(stack);
        if (!allowed) return 0;

        IEnergyWeapon.EnergyType current = item.getCurrentEnergyType(stack);
        if (current != IEnergyWeapon.EnergyType.NONE && current != type) return 0;
        if (current == IEnergyWeapon.EnergyType.NONE) item.setEnergyType(stack, type);

        int before = item.getCurrentEnergy(stack);
        item.addEnergy(stack, amount);
        int added = item.getCurrentEnergy(stack) - before;

        // Si no entró nada, dejamos el tipo como estaba
        if (added <= 0 && current == IEnergyWeapon.EnergyType.NONE) {
            item.setEnergyType(stack, IEnergyWeapon.EnergyType.NONE);
        }
        return Math.max(added, 0);
    }
}