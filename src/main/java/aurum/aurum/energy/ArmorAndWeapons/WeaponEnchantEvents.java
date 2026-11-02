package aurum.aurum.energy.ArmorAndWeapons;

import aurum.aurum.effectsPlayer.AurumBlight;
import aurum.aurum.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "aurum")
public class WeaponEnchantEvents {

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ItemStack weapon = e.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;

        var enchants = victim.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        int dew = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(ModEnchantments.AURUM_DEW), weapon);
        if (dew > 0) {
            AurumBlight.applyFromEnchant(victim, dew);
        }

        int siphon = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(ModEnchantments.ENERGY_SIPHON), weapon);
        if (siphon > 0 && e.getNewDamage() > 0) {
            EnergySiphon.siphon(attacker, victim, weapon, e.getNewDamage(), siphon);
        }
    }
}