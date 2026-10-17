package aurum.aurum.Red; // <-- CAMBIADO: Fuera de la carpeta 'client'

import aurum.aurum.Aurum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncAurumBloodPayload(int value) implements CustomPacketPayload {
    public static final Type<SyncAurumBloodPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "sync_aurum_blood"));

    public static final StreamCodec<FriendlyByteBuf, SyncAurumBloodPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncAurumBloodPayload::value,
            SyncAurumBloodPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}