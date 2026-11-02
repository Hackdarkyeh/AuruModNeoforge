package aurum.aurum.item.ArmorItem;

import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor.EnergyType;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.armors.ModArmorMaterials;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** Lógica de las armaduras del mod sobre componentes: sirve para Aurelite y para cualquier armadura autorizada. */
public final class AurumArmor {
    private AurumArmor() {}

    /** Pieza "del mod": armadura autorizada (las Aurelite lo traen de fábrica). */
    public static boolean is(ItemStack s) {
        return s.getItem() instanceof ArmorItem && s.getOrDefault(ModDataComponents.ARMOR_AUTHORIZED.get(), false);
    }

    private static ArmorEnergyData data(ItemStack s) {
        return s.getOrDefault(ModDataComponents.ARMOR_ENERGY.get(), ArmorEnergyData.defaultData());
    }

    public static EnergyType getType(ItemStack s) { return data(s).energyType(); }
    public static int getEnergy(ItemStack s)      { return data(s).currentEnergy(); }
    public static int getMaxEnergy(ItemStack s)   { return data(s).maxCapacity(); }

    /** ¿Protege? Solo con energía cargada de un tipo concreto. */
    public static boolean hasProtectionPower(ItemStack s) {
        ArmorEnergyData d = data(s);
        return d.currentEnergy() > 0 && d.energyType() != EnergyType.NONE;
    }

    public static void setEnergy(ItemStack s, int amount) {
        ArmorEnergyData d = data(s);
        s.set(ModDataComponents.ARMOR_ENERGY.get(),
                new ArmorEnergyData(Math.min(Math.max(0, amount), d.maxCapacity()), d.maxCapacity(), d.energyType()));
    }

    public static void setType(ItemStack s, EnergyType type) {
        ArmorEnergyData d = data(s);
        s.set(ModDataComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(d.currentEnergy(), d.maxCapacity(), type));
    }

    /** Devuelve la energía realmente añadida. */
    public static int addEnergy(ItemStack s, int amount) {
        ArmorEnergyData d = data(s);
        int added = Math.max(0, Math.min(amount, d.maxCapacity() - d.currentEnergy()));
        setEnergy(s, d.currentEnergy() + added);
        return added;
    }

    /** Carga con un tipo de energía; solo si es compatible con la que ya tiene la pieza. */
    public static boolean addEnergyOfType(ItemStack s, EnergyType type, int amount) {
        EnergyType current = getType(s);
        if (current != EnergyType.NONE && current != type) return false;
        int added = addEnergy(s, amount);
        if (added > 0 && current == EnergyType.NONE) setType(s, type);
        return added > 0;
    }

    /** Gasto de energía. Al llegar a 0 vuelve a NONE y deja de proteger. */
    public static void consume(ItemStack s, int amount) {
        int remaining = getEnergy(s) - amount;
        if (remaining > 0) setEnergy(s, remaining);
        else { setEnergy(s, 0); setType(s, EnergyType.NONE); }
    }

    /** Calidad de la pieza: armadura + dureza. Es la que usa el Purifier para su coste. */
    public static float quality(ArmorItem a) {
        return a.getDefense() + a.getToughness();
    }

    /** Calidad respecto a la Aurelite básica del mismo tipo: básica = 1,0; dorada = 2,0; peto de hierro ≈ 0,55. */
    public static float materialQuality(ItemStack s) {
        if (!(s.getItem() instanceof ArmorItem a)) return 0F;
        ArmorMaterial base = ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC.value();
        float ref = base.defense().getOrDefault(a.getType(), 0) + base.toughness();
        return ref <= 0F ? 1F : quality(a) / ref;
    }

    /** La Aurelite básica sube de nivel más rápido; el resto, igual que antes. */
    public static double getExpGainMultiplier(ItemStack s) {
        return s.getItem() instanceof ArmorItem a && a.getMaterial() == ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC
                ? ModArmorItem.BASIC_EXP_GAIN_MULTIPLIER : 1.0;
    }
}