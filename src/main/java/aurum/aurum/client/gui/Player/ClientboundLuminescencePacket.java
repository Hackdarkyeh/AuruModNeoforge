package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.client.LuminescenceState;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor → jugador: ids de las entidades que deben brillar y su nivel de amenaza (0 verde, 1 amarillo, 2 rojo). */
public record ClientboundLuminescencePacket(int[] ids, byte[] levels) implements CustomPacketPayload {

    public static final Type<ClientboundLuminescencePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "luminescence_sync"));

    public static final StreamCodec<ByteBuf, ClientboundLuminescencePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                VarInt.write(buf, p.ids.length);
                for (int i = 0; i < p.ids.length; i++) {
                    VarInt.write(buf, p.ids[i]);
                    buf.writeByte(p.levels[i]);
                }
            },
            buf -> {
                int n = VarInt.read(buf);
                if (n < 0 || n > 4096) throw new DecoderException("Luminescence packet too big: " + n);
                int[] ids = new int[n];
                byte[] levels = new byte[n];
                for (int i = 0; i < n; i++) {
                    ids[i] = VarInt.read(buf);
                    levels[i] = buf.readByte();
                }
                return new ClientboundLuminescencePacket(ids, levels);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundLuminescencePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> LuminescenceState.set(payload.ids(), payload.levels()));
    }
}