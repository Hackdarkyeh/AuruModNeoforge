package aurum.aurum.Red;


import aurum.aurum.Aurum;
import aurum.aurum.Soul.SoulData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncSoulContainerPayload(SoulData.PlayerSoulContainer container) implements CustomPacketPayload {
    public static final Type<SyncSoulContainerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "sync_soul_container"));

    // Convertimos el Codec normal a StreamCodec usando ByteBufCodecs.fromCodec
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoulContainerPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(SoulData.PlayerSoulContainer.CODEC), SyncSoulContainerPayload::container,
            SyncSoulContainerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}