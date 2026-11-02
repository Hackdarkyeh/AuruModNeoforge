package aurum.aurum.energy.ArmorAndWeapons;

import aurum.aurum.item.Battery.BatteryItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EnergySiphon {
    private static final EquipmentSlot[] ARMOR =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void siphon(LivingEntity attacker, LivingEntity victim, ItemStack weapon, float damage, int level) {
        int budget = Math.min(EnergyConfig.Siphon.MAX_PER_HIT,
                Math.round(damage * EnergyConfig.Siphon.ENERGY_PER_DAMAGE_PER_LEVEL * level));

        for (EquipmentSlot slot : ARMOR) {
            if (budget <= 0) return;
            ItemStack piece = victim.getItemBySlot(slot);
            if (!(piece.getItem() instanceof IEnergyArmor src)) continue;

            IEnergyArmor.EnergyType kind = src.getCurrentEnergyType(piece);
            int available = src.getCurrentEnergy(piece);
            if (kind == IEnergyArmor.EnergyType.NONE || available <= 0) continue;

            int taken = Math.min(budget, available);
            src.setEnergy(piece, available - taken);   // la víctima siempre pierde lo robado
            budget -= taken;
            deliver(attacker, weapon, kind, taken);    // lo que no cabe en ningún sitio se pierde
        }
    }

    private static void deliver(LivingEntity attacker, ItemStack weapon, IEnergyArmor.EnergyType kind, int amount) {
        // 1) el propio arma
        if (weapon.getItem() instanceof IEnergyWeapon w && w.canReceiveEnergy(weapon)
                && w.getCurrentEnergyType(weapon).name().equals(kind.name())) {
            amount -= w.addEnergy(weapon, amount);
            if (amount <= 0) return;
        }
        // 2) armadura del atacante
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = attacker.getItemBySlot(slot);
            if (piece.getItem() instanceof IEnergyArmor a && a.canReceiveEnergy(piece)
                    && a.getCurrentEnergyType(piece) == kind) {
                amount -= a.addEnergy(piece, amount);
                if (amount <= 0) return;
            }
        }
        // 3) baterías del inventario (solo jugadores)
        if (attacker instanceof Player player) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!(s.getItem() instanceof BatteryItem)) continue;
                float accepted = new BatteryItem.Storage(s).addEnergy(kind, amount, false);
                amount -= Math.round(accepted);
            }
        }
    }
}