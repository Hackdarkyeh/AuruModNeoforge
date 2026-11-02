package aurum.aurum.item;

import aurum.aurum.eventHandler.AurumExposureHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Contador Geiger. Funciona desde cualquier hueco del inventario (como el reloj):
 * - la aguja (textura) la mueve el cliente con la exposición acumulada (ver ModItemProperties);
 * - los clics los hace el servidor según la radiación que recibe el jugador ahora mismo.
 */
public class GeigerCounterItem extends Item {

    /** Probabilidad de clic por tick = ganancia por segundo * este factor. 0.6 -> luna morada sin barrera: ~6 clics/s. */
    private static final float CLICK_FACTOR = 0.6f;

    public GeigerCounterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        float rate = AurumExposureHandler.currentRatePerSecond(player);
        if (rate <= 0.0f) {
            return; // en silencio cuando no hay radiación
        }
        if (level.random.nextFloat() < rate * CLICK_FACTOR) {
            // Solo lo oye el dueño. [COMPROBAR] ServerPlayer.playNotifySound(SoundEvent, SoundSource, float, float)
            player.playNotifySound(SoundEvents.COMPARATOR_CLICK, SoundSource.PLAYERS, 0.4f,
                    0.9f + level.random.nextFloat() * 0.3f);
        }
    }
}