package aurum.aurum.eventHandler.ArmorEvents;

import aurum.aurum.Aurum;
import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.item.ArmorItem.AurumArmor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorEnergyEffectsHandler {
    private static final int INTERVAL = 10;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide() || p.tickCount % INTERVAL != 0) return;
        for (ItemStack s : p.getArmorSlots()) applyEffects(s, p);
    }

    /** Efectos gratuitos de la energía: velocidad con la oscura, curación con la limpia. Solo con energía. */
    public static void applyEffects(ItemStack s, Player p) {
        if (!AurumArmor.is(s) || !AurumArmor.hasProtectionPower(s)) return;
        switch (AurumArmor.getType(s)) {
            case DARK_ENERGY -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            case CLEAN_ENERGY -> {
                if (p.getHealth() < p.getMaxHealth())
                    p.heal(EnergyConfig.AureliteArmor.CLEAN_ENERGY_REGENERATION * INTERVAL);
            }
            default -> { }
        }
    }
}