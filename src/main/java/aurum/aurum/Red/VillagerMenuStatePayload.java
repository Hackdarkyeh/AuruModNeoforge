package aurum.aurum.Red;

import aurum.aurum.Aurum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado del menú de interacción de un aldeano contratable, del servidor al cliente.
 * open = true abre la pantalla; open = false solo la refresca si ya está abierta.
 *
 * @param profession    ordinal de AurumVillagerEntity.Profession
 * @param status        STATUS_NONE / STATUS_MINE / STATUS_OTHER
 * @param ticksRemaining ticks que le quedan al contrato (0 si no hay)
 * @param price         yanyitos que cuesta un día para este jugador (ya con el recargo por reputación)
 * @param coins         yanyitos que lleva el jugador
 * @param canAddDay     false si añadir un día superaría el máximo
 * @param creative      en creativo no se paga
 */
public record VillagerMenuStatePayload(int entityId, boolean open, int profession, int status, long ticksRemaining,
                                       boolean follow, int price, int coins, boolean canAddDay, boolean creative)
        implements CustomPacketPayload {

    public static final int STATUS_NONE = 0;
    public static final int STATUS_MINE = 1;
    public static final int STATUS_OTHER = 2;

    public static final Type<VillagerMenuStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "villager_menu_state"));

    // [COMPROBAR] StreamCodec.of(encoder, decoder): el encoder recibe (buffer, valor)
    public static final StreamCodec<FriendlyByteBuf, VillagerMenuStatePayload> CODEC =
            StreamCodec.of(VillagerMenuStatePayload::write, VillagerMenuStatePayload::read);

    private static void write(FriendlyByteBuf buf, VillagerMenuStatePayload p) {
        buf.writeVarInt(p.entityId());
        buf.writeBoolean(p.open());
        buf.writeVarInt(p.profession());
        buf.writeVarInt(p.status());
        buf.writeVarLong(p.ticksRemaining());
        buf.writeBoolean(p.follow());
        buf.writeVarInt(p.price());
        buf.writeVarInt(p.coins());
        buf.writeBoolean(p.canAddDay());
        buf.writeBoolean(p.creative());
    }

    private static VillagerMenuStatePayload read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        boolean open = buf.readBoolean();
        int profession = buf.readVarInt();
        int status = buf.readVarInt();
        long ticks = buf.readVarLong();
        boolean follow = buf.readBoolean();
        int price = buf.readVarInt();
        int coins = buf.readVarInt();
        boolean canAddDay = buf.readBoolean();
        boolean creative = buf.readBoolean();
        return new VillagerMenuStatePayload(entityId, open, profession, status, ticks, follow, price, coins, canAddDay, creative);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
