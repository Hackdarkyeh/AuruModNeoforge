package aurum.aurum.worldgen.Dimension;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;


public class AurumWeatherEvents {

    // Custom weather states
    public enum AurumWeatherState {
        STANDARD_NIGHT, DARK_NIGHT, NO_MOON, PURPLE_MOON, AURUM_RAIN
    }

    private static AurumWeatherState currentWeatherState = AurumWeatherState.STANDARD_NIGHT;
    private static long ticksInDimension = 0;

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) { // Corrected: Use LevelTickEvent.Post for phase
        if (event.getLevel() instanceof ServerLevel serverLevel) { // Corrected: Use getLevel() and check instanceof
            if (serverLevel.dimension() == AurumDimension.AURUM_DIMENSION_KEY) { // Corrected: AURUM_DIMENSION_KEY should be accessible
                ticksInDimension++;

                // Always set time to night
                serverLevel.setDayTime(18000); // Sets time to midnight

                // Control light levels for constant hostile mob spawning
                // This might require a custom ChunkGenerator or more direct packet manipulation
                // For now, relying on the dimension type's ambient_light and monster_spawn_light_level

                // Implement weather events based on ticks or random chance
                if (ticksInDimension % 24000 == 0) { // Every Minecraft day (20 minutes)
                    // Cycle through weather states or apply random chance
                    switch (currentWeatherState) {
                        case STANDARD_NIGHT:
                            currentWeatherState = AurumWeatherState.DARK_NIGHT;
                            serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("The Aurum Dimension grows darker..."), false);
                            break;
                        case DARK_NIGHT:
                            currentWeatherState = AurumWeatherState.NO_MOON;
                            serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("The moon vanishes from the Aurum sky."), false);
                            break;
                        case NO_MOON:
                            if (serverLevel.getRandom().nextFloat() < 0.2F) { // 20% chance for Purple Moon
                                currentWeatherState = AurumWeatherState.PURPLE_MOON;
                                serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("A mystical purple moon rises in the Aurum Dimension!"), false);
                            } else {
                                currentWeatherState = AurumWeatherState.STANDARD_NIGHT;
                                serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("The Aurum Dimension returns to its standard night."), false);
                            }
                            break;
                        case PURPLE_MOON:
                            if (serverLevel.getRandom().nextFloat() < 0.1F) { // 10% chance for Aurum Rain after Purple Moon
                                currentWeatherState = AurumWeatherState.AURUM_RAIN;
                                serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("Golden rain begins to fall in the Aurum Dimension!"), false);
                            } else {
                                currentWeatherState = AurumWeatherState.STANDARD_NIGHT;
                                serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("The purple moon fades, and the Aurum Dimension returns to its standard night."), false);
                            }
                            break;
                        case AURUM_RAIN:
                            currentWeatherState = AurumWeatherState.STANDARD_NIGHT;
                            serverLevel.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("The golden rain subsides in the Aurum Dimension."), false);
                            break;
                    }
                }

                // Apply weather effects (e.g., visual changes, sound changes)
                // This would involve sending custom packets to clients or modifying client-side rendering.
                // For now, only messages are broadcasted.
            }
        }
    }

    public static AurumWeatherState getCurrentWeatherState() {
        return currentWeatherState;
    }
}
