package aurum.aurum.item.Battery;

import aurum.aurum.energy.ArmorAndWeapons.IEnergyArmor.EnergyType;
import aurum.aurum.energy.engineering.IEnergyStorage;
import aurum.aurum.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BatteryItem extends Item {

    public BatteryItem() {
        super(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.BATTERY_DATA.get(), BatteryData.defaultData()));
    }

    public static BatteryData data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BATTERY_DATA.get(), BatteryData.defaultData());
    }

    /** Estado visual para el modelo: 0 vacía | 1-4 oscura | 5-8 limpia. */
    public static float getVisualState(ItemStack stack) {
        BatteryData d = data(stack);
        float cap = d.tierData().capacity;
        if (d.energy() <= 0) return 0;
        int stage = d.energy() >= cap ? 4 : d.energy() / cap >= 2f / 3f ? 3 : d.energy() / cap >= 1f / 3f ? 2 : 1;
        return d.energyType() == EnergyType.CLEAN_ENERGY ? 4 + stage : stage; // NONE con carga => se ve oscura
    }

    // ---- barra y tooltip ----
    @Override public boolean isBarVisible(ItemStack stack) { return data(stack).energy() > 0; }

    @Override public int getBarWidth(ItemStack stack) {
        BatteryData d = data(stack);
        return Math.round(13.0F * d.energy() / d.tierData().capacity);
    }

    @Override public int getBarColor(ItemStack stack) {
        return switch (data(stack).energyType()) {
            case CLEAN_ENERGY -> 0x33FF99;
            case DARK_ENERGY -> 0x6600CC;
            default -> 0x9933FF;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        BatteryData d = data(stack);
        String type = switch (d.energyType()) { case DARK_ENERGY -> "Oscura"; case CLEAN_ENERGY -> "Limpia"; default -> "Sin energía"; };
        tip.add(Component.literal("Energía " + type + ": " + (int) d.energy() + " / " + d.tierData().capacity)
                .withStyle(d.energyType() == EnergyType.CLEAN_ENERGY ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE));
        tip.add(Component.literal("Tier " + d.tier()).withStyle(ChatFormatting.GRAY));
    }

    // ---- almacenamiento ligado al ItemStack (lo usarán las máquinas) ----
    public static class Storage implements IEnergyStorage {
        private final ItemStack stack;
        public Storage(ItemStack stack) { this.stack = stack; }

        private BatteryData d() { return data(stack); }
        private void set(float e, EnergyType t) {
            float clamped = Mth.clamp(e, 0, d().tierData().capacity);
            stack.set(ModDataComponents.BATTERY_DATA.get(), d().withEnergy(clamped, clamped <= 0 ? EnergyType.NONE : t));
        }

        /** Carga con tipo: una batería no mezcla energía limpia y oscura. */
        public float addEnergy(EnergyType type, float amount, boolean simulate) {
            BatteryData d = d();
            if (d.energy() > 0 && d.energyType() != EnergyType.NONE && d.energyType() != type) return 0;
            float accepted = Math.min(Math.min(amount, d.tierData().maxReceive), getRemainingCapacity());
            if (!simulate && accepted > 0) set(d.energy() + accepted, type);
            return accepted;
        }

        @Override public float getEnergyStored() { return d().energy(); }
        @Override public float getCapacity() { return d().tierData().capacity; }
        @Override public float getRemainingCapacity() { return getCapacity() - getEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }

        @Override public float receiveEnergy(float maxReceive, boolean simulate) {
            float accepted = Math.min(Math.min(maxReceive, d().tierData().maxReceive), getRemainingCapacity());
            if (!simulate && accepted > 0) set(getEnergyStored() + accepted, d().energyType());
            return accepted;
        }
        @Override public float addEnergy(float energy, boolean simulate) { // sin límite de tasa
            float accepted = Math.min(energy, getRemainingCapacity());
            if (!simulate && accepted > 0) set(getEnergyStored() + accepted, d().energyType());
            return accepted;
        }
        @Override public float consumeEnergy(float energy, boolean simulate) {
            float out = Math.min(Math.min(energy, d().tierData().maxExtract), getEnergyStored());
            if (!simulate && out > 0) set(getEnergyStored() - out, d().energyType());
            return out;
        }
        @Override public void setStoredEnergy(float e) { set(e, d().energyType()); }

        @Override public CompoundTag saveEnergyToTag(CompoundTag tag) { tag.putFloat("energy", getEnergyStored()); return tag; }
        @Override public void loadEnergyFromTag(CompoundTag tag) { setStoredEnergy(tag.getFloat("energy")); }
    }
}