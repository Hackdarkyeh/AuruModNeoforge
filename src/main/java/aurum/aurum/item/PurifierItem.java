package aurum.aurum.item;

import aurum.aurum.eventHandler.AurumExposureHandler;
import aurum.aurum.init.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PurifierItem extends Item {

    // ── Valores ajustables ───────────────────────────────────────────────────
    /** Exposición que baja cada uso. */
    public static final float EXPOSURE_REDUCTION = 100.0f;
    /** Durabilidad que gasta cada uso (de 500: 20 usos). */
    public static final int DURABILITY_COST = 25;
    /** Espera entre usos (60 ticks = 3 s). */
    public static final int COOLDOWN_TICKS = 60;

    public PurifierItem(Properties pProperties) {
        // Definimos durabilidad máxima, por ejemplo 500
        super(pProperties.durability(500));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // El attachment está sincronizado, así que lo podemos leer igual en cliente y servidor
        float exposure = player.getData(ModAttachments.AURUM_EXPOSURE);
        if (exposure <= 0.0f) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("item.aurum.purifier.clean"), true);
            }
            return InteractionResultHolder.fail(stack); // sin exposición no se gasta nada
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            float after = Math.max(0.0f, exposure - EXPOSURE_REDUCTION);
            AurumExposureHandler.set(serverPlayer, after);

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            stack.hurtAndBreak(DURABILITY_COST, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

            // [COMPROBAR] nombres de sonido y partícula (provisionales)
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                        12, 0.4, 0.6, 0.4, 0.03);
            }

            player.displayClientMessage(
                    Component.translatable("item.aurum.purifier.used", Math.round(exposure), Math.round(after)), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        // Solo brilla si aún tiene "energía" (no está completamente roto)
        // Cuanto menor sea el daño, más "energía" tiene
        return pStack.getDamageValue() < pStack.getMaxDamage();
    }
}