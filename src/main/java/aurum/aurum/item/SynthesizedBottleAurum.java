package aurum.aurum.item;

import aurum.aurum.Red.SyncAurumBloodPayload;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.utils.AurumUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class SynthesizedBottleAurum extends Item {

    private static final ResourceLocation AURUM_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("aurum", "blood_max_health");
    private static final int AURUM_PER_ROW = 972;
    private static final double HEALTH_PER_ROW = 20.0; // 20.0 HP = 10 corazones (1 fila)

    public SynthesizedBottleAurum() {
        super(new Properties()
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        );
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        int aurumActual = player.getData(ModAttachments.AURUM_BLOOD);
        int maxAurum = AurumUtils.calculateMaxAurum(player);

        if (aurumActual >= maxAurum) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (entityLiving instanceof Player player) {
            int aurumActual = player.getData(ModAttachments.AURUM_BLOOD);
            int maxAurum = AurumUtils.calculateMaxAurum(player);

            if (aurumActual < maxAurum) {
                int nuevoAurum = aurumActual + 1;
                player.setData(ModAttachments.AURUM_BLOOD, nuevoAurum);

                updatePlayerMaxHealthModifiers(player, nuevoAurum);

                player.setHealth(1.0f);

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new SyncAurumBloodPayload(nuevoAurum));
                }
            }
        }

        if (entityLiving instanceof Player p && p.getAbilities().instabuild) {
            return stack;
        }

        return new ItemStack(Items.GLASS_BOTTLE);
    }

    public static void updatePlayerMaxHealthModifiers(Player player, int currentAurum) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        maxHealthAttr.removeModifier(AURUM_HEALTH_MODIFIER_ID);

        int filasExtra = currentAurum / AURUM_PER_ROW;

        if (filasExtra > 0) {
            double vidaExtraDeAtributo = filasExtra * HEALTH_PER_ROW;

            AttributeModifier aurumHealthModifier = new AttributeModifier(
                    AURUM_HEALTH_MODIFIER_ID,
                    vidaExtraDeAtributo,
                    AttributeModifier.Operation.ADD_VALUE
            );

            maxHealthAttr.addPermanentModifier(aurumHealthModifier);
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}