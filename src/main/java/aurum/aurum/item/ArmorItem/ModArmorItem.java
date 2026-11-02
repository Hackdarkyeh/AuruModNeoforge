package aurum.aurum.item.ArmorItem;

import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor;
import aurum.aurum.eventHandler.ArmorEvents.ArmorEnergyEffectsHandler;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.armors.ModArmorMaterials;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import static aurum.aurum.init.ModDataComponents.ARMOR_AUTHORIZED;

public class ModArmorItem extends ArmorItem implements IEnergyArmor {

    /** Multiplicador de ganancia de EXP de la armadura básica (la dorada usa 1.0). */
    public static final double BASIC_EXP_GAIN_MULTIPLIER = 1.5;

    /** Energía que pierde la armadura por cada punto de daño que le hace una entidad. */
    public static final float ENERGY_LOSS_PER_DAMAGE = 1.0F;

    public ModArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1)
                .component(ModDataComponents.ARMOR_TIER.get(), new ArmorTierData(1, true))
                .component(ModDataComponents.ARMOR_ENERGY.get(), new ArmorEnergyData(0,
                        EnergyConfig.AureliteArmor.MAX_ENERGY_CAPACITY, EnergyType.NONE))
                .component(ARMOR_AUTHORIZED, true));
    }

    public static boolean hasProtectionPower(ItemStack stack) { return AurumArmor.hasProtectionPower(stack); }

    public double getExpGainMultiplier() {
        return getMaterial() == ModArmorMaterials.AURELITE_ARMOR_MATERIAL_BASIC ? BASIC_EXP_GAIN_MULTIPLIER : 1.0;
    }

    // ── IEnergyArmor: todo delega en AurumArmor ──────────────────────────────
    @Override public EnergyType getCurrentEnergyType(ItemStack s) { return AurumArmor.getType(s); }
    @Override public void setEnergyType(ItemStack s, EnergyType t) { AurumArmor.setType(s, t); }
    @Override public int getCurrentEnergy(ItemStack s) { return AurumArmor.getEnergy(s); }
    @Override public int getMaxEnergy(ItemStack s) { return AurumArmor.getMaxEnergy(s); }
    @Override public void setEnergy(ItemStack s, int amount) { AurumArmor.setEnergy(s, amount); }
    @Override public int addEnergy(ItemStack s, int amount) { return AurumArmor.addEnergy(s, amount); }
    @Override public boolean canReceiveEnergy(ItemStack s) { return getCurrentEnergy(s) < getMaxEnergy(s); }

    @Override
    public int getEnergyConsumptionPerTick(ItemStack stack) {
        return switch (getCurrentEnergyType(stack)) {
            case DARK_ENERGY -> (int) (EnergyConfig.AureliteArmor.DARK_ENERGY_CONSUMPTION_PER_TICK
                    * EnergyConfig.AureliteArmor.DARK_ENERGY_DRAIN_MULTIPLIER);
            case CLEAN_ENERGY -> (int) (EnergyConfig.AureliteArmor.CLEAN_ENERGY_CONSUMPTION_PER_TICK
                    * EnergyConfig.AureliteArmor.CLEAN_ENERGY_DRAIN_MULTIPLIER);
            default -> 0;
        };
    }

    public boolean addDarkEnergy(ItemStack stack, int amount) { return AurumArmor.addEnergyOfType(stack, EnergyType.DARK_ENERGY, amount); }
    public boolean addCleanEnergy(ItemStack stack, int amount) { return AurumArmor.addEnergyOfType(stack, EnergyType.CLEAN_ENERGY, amount); }

    public boolean repairWithCleanEnergy(ItemStack stack, int cleanEnergyAmount) {
        if (getCurrentEnergyType(stack) != EnergyType.CLEAN_ENERGY) return false;
        int repairCost = cleanEnergyAmount * EnergyConfig.AureliteArmor.ENERGY_REPAIR_AMOUNT;
        if (getCurrentEnergy(stack) >= repairCost) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            setEnergy(stack, getCurrentEnergy(stack) - repairCost);
            return true;
        }
        return false;
    }

    public void applyEnergyEffectsPerTick(ItemStack stack, Player player) { ArmorEnergyEffectsHandler.applyEffects(stack, player); }

    public void consumeEnergyForHit(ItemStack stack, int amount) { AurumArmor.consume(stack, amount); }
}