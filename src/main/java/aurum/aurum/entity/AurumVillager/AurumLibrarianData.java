package aurum.aurum.entity.AurumVillager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Ofertas actuales de un bibliotecario y cuándo se reabasteció por última vez.
 * El nivel y la experiencia están en AurumVillagerLevel (común a todos los aldeanos).
 * Va como attachment del propio aldeano. Si offers está vacío, las ofertas se generan la primera vez que se comercia.
 */
public record AurumLibrarianData(MerchantOffers offers, long lastRestock) {

    /** Valor por defecto para un bibliotecario nuevo (cada llamada crea unas ofertas vacías propias). */
    public static AurumLibrarianData fresh() {
        return new AurumLibrarianData(new MerchantOffers(), 0L);
    }

    // [COMPROBAR] MerchantOffers.CODEC existe en 1.21.1 como Codec<MerchantOffers>
    public static final Codec<AurumLibrarianData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            MerchantOffers.CODEC.fieldOf("offers").forGetter(AurumLibrarianData::offers),
            Codec.LONG.fieldOf("last_restock").forGetter(AurumLibrarianData::lastRestock)
    ).apply(inst, AurumLibrarianData::new));
}
