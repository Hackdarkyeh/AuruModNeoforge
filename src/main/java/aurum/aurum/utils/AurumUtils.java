package aurum.aurum.utils;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.init.ModAttachments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AurumUtils {

    private AurumUtils() {
        // Clase de utilidades: no instanciable
    }

    /**
     * Calcula el máximo de Aurum en Sangre que puede tener un jugador,
     * en función de su alma activa (o de su valor actual si no tiene alma).
     */
    public static int calculateMaxAurum(Player player) {
        var soulContainer = player.getData(ModAttachments.ACTIVE_SOUL);
        ItemStack soulStack = soulContainer.soulStack();

        if (soulStack.isEmpty()) {
            return player.getData(ModAttachments.AURUM_BLOOD);
        }

        SoulData soulData = soulContainer.activeSoul();
        int maxHonor = soulData.maxHonor();
        int tier = soulData.idTier();

        int baseMaxHealth = (int) player.getAttributeBaseValue(Attributes.MAX_HEALTH);

        return (maxHonor + baseMaxHealth) * tier;
    }
}
