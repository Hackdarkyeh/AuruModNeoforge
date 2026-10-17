package aurum.aurum.client.gui.Player;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// IMPORTANTE: Asegúrate de importar la clase de tu menú de armaduras
// import aurum.aurum.client.gui.ArmorControlMenu; 

public record OpenArmorMenuPacket() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("aurum", "open_armor_menu");
    public static final Type<OpenArmorMenuPacket> TYPE = new Type<>(ID);

    // StreamCodec para 1.21.1
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, OpenArmorMenuPacket> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.unit(new OpenArmorMenuPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final OpenArmorMenuPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // El servidor recibe la petición y abre el menú de la armadura
            context.player().openMenu(new SimpleMenuProvider(
                    // NOTA: Cambia "ArmorControlMenu" por el nombre exacto de tu clase Menu de armaduras
                    (id, inv, player) -> new ArmorControlMenu(id, inv),
                    Component.translatable("container.aurum.armor_inventory")
            ));
        });
    }
}