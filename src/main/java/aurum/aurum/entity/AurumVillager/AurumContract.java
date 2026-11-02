package aurum.aurum.entity.AurumVillager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

/**
 * Contrato de alquiler de un aldeano: quién lo contrató, cuántos ticks le quedan (0 = sin contrato)
 * y si debe seguir a su dueño.
 */
public record AurumContract(UUID owner, long ticksRemaining, boolean follow) {

    public static final AurumContract EMPTY = new AurumContract(new UUID(0L, 0L), 0L, true);

    public static final Codec<AurumContract> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(AurumContract::owner),
            Codec.LONG.fieldOf("ticks").forGetter(AurumContract::ticksRemaining),
            Codec.BOOL.optionalFieldOf("follow", true).forGetter(AurumContract::follow)
    ).apply(inst, AurumContract::new));

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public AurumContract withTicks(long ticks) {
        return new AurumContract(owner, ticks, follow);
    }

    public AurumContract withFollow(boolean newFollow) {
        return new AurumContract(owner, ticksRemaining, newFollow);
    }
}
