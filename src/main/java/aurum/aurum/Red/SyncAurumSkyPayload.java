package aurum.aurum.Red;

import aurum.aurum.Aurum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado de la noche de dim1 que el servidor manda a cada jugador (una vez por segundo).
 *
 * @param inAurum        true si el jugador está ahora en dim1 (si no, state/next/ticksRemaining no valen)
 * @param state          ordinal de AurumWeatherState actual
 * @param next           ordinal del siguiente estado, o -1 si aún no está decidido
 * @param ticksRemaining ticks que le quedan al estado actual
 * @param raining        true si en el nivel del jugador llueve la lluvia morada (estado AURUM_RAIN o forzada por comando)
 */
public record SyncAurumSkyPayload(boolean inAurum, int state, int next, int ticksRemaining, boolean raining)
        implements CustomPacketPayload {

    public static final Type<SyncAurumSkyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "sync_aurum_sky"));

    public static final StreamCodec<FriendlyByteBuf, SyncAurumSkyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncAurumSkyPayload::inAurum,
            ByteBufCodecs.VAR_INT, SyncAurumSkyPayload::state,
            ByteBufCodecs.VAR_INT, SyncAurumSkyPayload::next,
            ByteBufCodecs.VAR_INT, SyncAurumSkyPayload::ticksRemaining,
            ByteBufCodecs.BOOL, SyncAurumSkyPayload::raining,
            SyncAurumSkyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}