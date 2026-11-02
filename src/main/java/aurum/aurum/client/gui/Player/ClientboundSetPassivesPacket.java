package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundSetPassivesPacket(SetPassivesData data) implements CustomPacketPayload {

    public static final Type<ClientboundSetPassivesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "set_passives_sync"));

    public static final StreamCodec<ByteBuf, ClientboundSetPassivesPacket> STREAM_CODEC =
            SetPassivesData.STREAM_CODEC.map(ClientboundSetPassivesPacket::new, ClientboundSetPassivesPacket::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundSetPassivesPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().setData(ModDataComponents.SET_PASSIVES, payload.data()));
    }
}