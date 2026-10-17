package aurum.aurum.client.gui.Player;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundUpgradeSkillPacket(int slotIndex, int skillType) implements CustomPacketPayload {

    public static final Type<ServerboundUpgradeSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "upgrade_skill"));

    public static final StreamCodec<ByteBuf, ServerboundUpgradeSkillPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundUpgradeSkillPacket::slotIndex,
            ByteBufCodecs.VAR_INT, ServerboundUpgradeSkillPacket::skillType,
            ServerboundUpgradeSkillPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundUpgradeSkillPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                int slotIndex = payload.slotIndex(); // Acceso al slot
                int skillType = payload.skillType(); // Acceso al tipo de pilar (1 a 4)

                // En el inventario de armadura de Minecraft: 3 - slotIndex
                ItemStack armorStack = player.getInventory().getArmor(3 - slotIndex);

                if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                    ArmorSkillsData oldSkills = armorStack.getOrDefault(
                            ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData());

                    ArmorSkillsData newSkills = switch (skillType) {
                        case 1 -> oldSkills.withAbsorption(oldSkills.absorption() + 1);
                        case 2 -> oldSkills.withRecovery(oldSkills.recovery() + 1);
                        case 3 -> oldSkills.withMagicAmp(oldSkills.magicAmp() + 1);
                        case 4 -> oldSkills.withResonance(oldSkills.resonance() + 1);
                        default -> oldSkills;
                    };

                    armorStack.set(ModDataComponents.ARMOR_SKILLS.get(), newSkills);
                }
            }
        });
    }
}