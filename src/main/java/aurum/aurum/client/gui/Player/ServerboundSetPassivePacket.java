package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
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
            if (context.player() instanceof ServerPlayer player) {
                int index = payload.passiveIndex();
                int level = payload.newLevel();

                switch (index) {
                    case 0 -> { // Modificador directo al ATRIBUTO MAX_HEALTH
                        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
                        if (healthAttribute != null) {
                            // Limpiamos el modificador anterior para refrescarlo
                            healthAttribute.removeModifier(HEALTH_MODIFIER_ID);

                            if (level > 0) {
                                // Nivel I = +4.0 (2 corazones)
                                // Nivel II = +8.0 (4 corazones)
                                // Nivel III = +12.0 (6 corazones)
                                double amount = level * 20.0D;

                                AttributeModifier modifier = new AttributeModifier(
                                        HEALTH_MODIFIER_ID,
                                        amount,
                                        AttributeModifier.Operation.ADD_VALUE
                                );

                                healthAttribute.addTransientModifier(modifier);
                            }
                        }
                    }
                    case 1 -> { // Salto
                        player.removeEffect(MobEffects.JUMP);
                        if (level > 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, level - 1, false, false, true));
                        }
                    }
                    case 2 -> { // Visión Nocturna
                        player.removeEffect(MobEffects.NIGHT_VISION);
                        if (level > 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, true));
                        }
                    }
                    case 3 -> { // Velocidad
                        player.removeEffect(MobEffects.MOVEMENT_SPEED);
                        if (level > 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, level - 1, false, false, true));
                        }
                    }
                }
            }
        });
    }
}