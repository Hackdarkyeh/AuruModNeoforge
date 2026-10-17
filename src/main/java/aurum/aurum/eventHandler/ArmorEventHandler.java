package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorEventHandler {

    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "set_passive_health");

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            if (event.getSlot().isArmor()) {
                int equippedAurumCount = 0;

                for (ItemStack stack : player.getArmorSlots()) {
                    if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                        equippedAurumCount++;
                    }
                }

                AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);

                if (equippedAurumCount == 4) {
                    // Si se equipa las 4 piezas y no tiene el atributo, le asigna Nivel I base (+4.0 HP = +2 corazones)
                    if (healthAttribute != null && !healthAttribute.hasModifier(HEALTH_MODIFIER_ID)) {
                        healthAttribute.addTransientModifier(new AttributeModifier(
                                HEALTH_MODIFIER_ID,
                                20.0D,
                                AttributeModifier.Operation.ADD_VALUE
                        ));
                    }
                } else {
                    // Si pierde el set 4/4, removemos el atributo de vida y las demás pasivas
                    if (healthAttribute != null) {
                        healthAttribute.removeModifier(HEALTH_MODIFIER_ID);
                    }
                    player.removeEffect(MobEffects.JUMP);
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    player.removeEffect(MobEffects.MOVEMENT_SPEED);
                }
            }
        }
    }
}