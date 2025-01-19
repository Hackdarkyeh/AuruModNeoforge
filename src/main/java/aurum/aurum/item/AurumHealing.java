package aurum.aurum.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import static aurum.aurum.init.ModEffects.AurumBlightEffect;

public class AurumHealing extends Item {
    public AurumHealing() {
        super(new Item.Properties().stacksTo(1));  // El ítem solo puede apilarse hasta 1 unidad
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;  // Animación de bebida
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);  // Inicia la animación de uso
        return InteractionResultHolder.consume(stack);  // Consume el ítem cuando se usa
    }

    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
        return 32;  // Duración de uso (tiempo que tarda en beberse)
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            if (entity instanceof ServerPlayer serverPlayer) {
                // Elimina el efecto "Aurum" solo cuando el ítem es completamente consumido
                if (serverPlayer.hasEffect(AurumBlightEffect.getDelegate())) {
                    serverPlayer.removeEffect(AurumBlightEffect.getDelegate());
                }

                // Si el jugador está en modo creativo, no se consume el ítem
                if (serverPlayer.isCreative()) {
                    return stack;
                }
            }
            stack.shrink(1);  // Reduce el tamaño del stack por 1 después de usarlo
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack;  // Devuelve el stack restante o vacío si se ha consumido todo
    }
}
