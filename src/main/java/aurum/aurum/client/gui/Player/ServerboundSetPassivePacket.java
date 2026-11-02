package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.eventHandler.SetPassivesHandler;
import aurum.aurum.init.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSetPassivePacket(int passiveIndex, int newLevel) implements CustomPacketPayload {

    public static final Type<ServerboundSetPassivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "set_passive_update"));

    public static final StreamCodec<ByteBuf, ServerboundSetPassivePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundSetPassivePacket::passiveIndex,
            ByteBufCodecs.VAR_INT, ServerboundSetPassivePacket::newLevel,
            ServerboundSetPassivePacket::new
    );

    // Identificador único para el modificador de vida de Aurum
    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "set_passive_health");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetPassivePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            SetPassivesData updated = player.getData(ModDataComponents.SET_PASSIVES)
                    .with(payload.passiveIndex(), payload.newLevel());

            // El servidor no se fía del cliente: no se pueden gastar más puntos de los que hay
            if (updated.spentPoints() <= SetPassivesData.EXTRA_POINTS) {
                player.setData(ModDataComponents.SET_PASSIVES, updated);
                SetPassivesHandler.apply(player); // aplica o quita los efectos según la barrera
            }
            // Siempre se reenvía el valor real: si se rechazó el cambio, el cliente vuelve a él
            SetPassivesHandler.sync(player);
        });
    }
}