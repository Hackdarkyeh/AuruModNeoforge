package aurum.aurum.client.gui.Player;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenSoulMenuPacket() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("aurum", "open_soul_menu");
    public static final Type<OpenSoulMenuPacket> TYPE = new Type<>(ID);

    // StreamCodec para 1.21.1
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, OpenSoulMenuPacket> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.unit(new OpenSoulMenuPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final OpenSoulMenuPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // El servidor recibe la petición y abre el menú
            context.player().openMenu(new SimpleMenuProvider(
                    (id, inv, player) -> new SoulInventoryPlayerMenu(id, inv),
                    Component.translatable("container.aurum.soul_inventory")
            ));
        });
    }
}
