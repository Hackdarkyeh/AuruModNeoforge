package aurum.aurum.Red;

import aurum.aurum.Aurum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Exposición a Aurum del jugador, del servidor al cliente. */
public record SyncAurumExposurePayload(float value) implements CustomPacketPayload {

    public static final Type<SyncAurumExposurePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "sync_aurum_exposure"));

    public static final StreamCodec<FriendlyByteBuf, SyncAurumExposurePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncAurumExposurePayload::value,
            SyncAurumExposurePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}