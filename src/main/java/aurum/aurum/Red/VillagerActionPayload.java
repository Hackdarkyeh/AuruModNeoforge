package aurum.aurum.Red;

import aurum.aurum.Aurum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Acción que el jugador pide desde el menú de un aldeano (se valida siempre en el servidor). */
public record VillagerActionPayload(int entityId, int action) implements CustomPacketPayload {

    public static final int ACTION_HIRE = 0;     // pagar un día de contrato
    public static final int ACTION_FOLLOW = 1;   // alternar seguir / quedarse
    public static final int ACTION_DISMISS = 2;  // terminar el contrato

    public static final Type<VillagerActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "villager_action"));

    public static final StreamCodec<FriendlyByteBuf, VillagerActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerActionPayload::entityId,
            ByteBufCodecs.VAR_INT, VillagerActionPayload::action,
            VillagerActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
