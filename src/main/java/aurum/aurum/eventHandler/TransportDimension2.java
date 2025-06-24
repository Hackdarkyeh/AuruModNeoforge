package aurum.aurum.eventHandler;

import aurum.aurum.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Objects;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
public class TransportDimension2 {


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {

            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(MODID, "dimension1"));
            if (player.level().dimension().equals(dimension) && player.getY() < 0 && hasItemInHand(player, ModItems.VEILPIERCER.get())) {
                MinecraftServer minecraftLevel = serverPlayer.getServer();
                assert minecraftLevel != null;
                ResourceKey<Level> dimension2 = ResourceKey.create(Registries.DIMENSION,
                        ResourceLocation.fromNamespaceAndPath(MODID, "dimension2"));
                serverPlayer.teleportTo(Objects.requireNonNull(minecraftLevel.getLevel(dimension2)),
                        serverPlayer.getX(), 100, serverPlayer.getZ(), 10, 10);

            }
        }
    }
    public static boolean hasItemInHand(Player player, Item item) {
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

}
