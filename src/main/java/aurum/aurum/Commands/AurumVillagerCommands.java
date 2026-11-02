package aurum.aurum.Commands;

import aurum.aurum.Aurum;
import aurum.aurum.entity.AurumVillager.AurumVillageData;
import aurum.aurum.worldgen.Dimension.AurumNightCycle;
import aurum.aurum.worldgen.Dimension.AurumSkyData;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Subcomandos de prueba de los aldeanos y de la noche de dim1. Cuelgan del comando raíz /Aurum
 * (Brigadier fusiona los registros que usan el mismo literal raíz, como ya hacen AurumCommands y
 * GenerateBridgesCommand). Requieren permisos de operador (nivel 2).
 *
 *   /Aurum period status | calm | hostile | auto
 *       Fuerza el periodo de los aldeanos en cualquier dimensión (útil en un superflat).
 *
 *   /Aurum sky status
 *   /Aurum sky set <standard_night|dark_night|no_moon|purple_moon|aurum_rain>
 *   /Aurum sky next <estado>
 *       Consulta o cambia el estado de la noche de dim1.
 *
 *   /Aurum village status | forgive
 *       Pueblo más cercano a ti: estado de su golem, tus muertes en él y el recargo de sus alquileres.
 *       forgive borra tu reputación en ese pueblo (solo para pruebas: en el juego es permanente).
 */
@EventBusSubscriber(modid = Aurum.MODID)
public class AurumVillagerCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Cada subcomando lleva su propio requires: si otra clase registra antes el nodo raíz "Aurum"
        // sin permisos, el requires del raíz de esta clase se ignora al fusionarse.
        LiteralArgumentBuilder<CommandSourceStack> period = Commands.literal("period")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> periodStatus(ctx.getSource())))
                .then(Commands.literal("calm").executes(ctx -> forcePeriod(ctx.getSource(), Boolean.FALSE)))
                .then(Commands.literal("hostile").executes(ctx -> forcePeriod(ctx.getSource(), Boolean.TRUE)))
                .then(Commands.literal("auto").executes(ctx -> forcePeriod(ctx.getSource(), null)));

        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        LiteralArgumentBuilder<CommandSourceStack> next = Commands.literal("next");
        for (AurumWeatherState state : AurumWeatherState.values()) {
            set.then(Commands.literal(state.id()).executes(ctx -> setState(ctx.getSource(), state)));
            next.then(Commands.literal(state.id()).executes(ctx -> setNext(ctx.getSource(), state)));
        }
        LiteralArgumentBuilder<CommandSourceStack> sky = Commands.literal("sky")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> skyStatus(ctx.getSource())))
                .then(set)
                .then(next);

        event.getDispatcher().register(Commands.literal("Aurum")
                .requires(src -> src.hasPermission(2))
                .then(period)
                .then(sky)
                .then(Commands.literal("village")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("status").executes(ctx -> villageStatus(ctx.getSource())))
                        .then(Commands.literal("forgive").executes(ctx -> villageForgive(ctx.getSource())))));
    }

    // ---------- /Aurum period ----------
    private static int forcePeriod(CommandSourceStack source, @Nullable Boolean hostile) {
        AurumNightCycle.force(hostile);
        String text = hostile == null ? "periodo automático" : hostile ? "periodo hostil forzado" : "periodo tranquilo forzado";
        source.sendSuccess(() -> Component.literal("[Aurum] " + text), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int periodStatus(CommandSourceStack source) {
        Level level = source.getLevel();
        Boolean forced = AurumNightCycle.getForced();
        boolean hostile = AurumNightCycle.isHostilePeriod(level);
        String msg = "[Aurum] Periodo actual: " + (hostile ? "HOSTIL" : "tranquilo");
        if (forced != null) {
            msg += " (forzado; usa /Aurum period auto para volver al automático)";
        } else {
            msg += " (cambia en " + (AurumNightCycle.ticksUntilChange(level) / 20) + " s)";
        }
        if (AurumNightCycle.isProtectionSuspended(level)) {
            msg += " | la divine_armor no protege esta noche";
        }
        String finalMsg = msg;
        source.sendSuccess(() -> Component.literal(finalMsg), false);
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /Aurum sky ----------
    @Nullable
    private static ServerLevel skyLevel(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(AurumWeatherEvents.AURUM_DIMENSION_KEY);
        if (level == null) {
            String available = source.getServer().levelKeys().stream()
                    .map(key -> key.location().toString())
                    .collect(java.util.stream.Collectors.joining(", "));
            source.sendFailure(Component.literal("[Aurum] No se encuentra la dimensión "
                    + AurumWeatherEvents.AURUM_DIMENSION_KEY.location() + ". Dimensiones cargadas: " + available));
        }
        return level;
    }

    private static int skyStatus(CommandSourceStack source) {
        ServerLevel level = skyLevel(source);
        if (level == null) {
            return 0;
        }
        AurumSkyData data = AurumSkyData.get(level);
        String msg = "[Aurum] Noche actual: " + data.current().id()
                + " (quedan " + (data.ticksRemaining() / 20) + " s)"
                + " | siguiente: " + (data.next() == null ? "sin decidir" : data.next().id());
        source.sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setState(CommandSourceStack source, AurumWeatherState state) {
        ServerLevel level = skyLevel(source);
        if (level == null) {
            return 0;
        }
        AurumSkyData data = AurumSkyData.get(level);
        data.forceCurrent(state, level.getRandom());
        AurumWeatherEvents.announce(level, state);
        source.sendSuccess(() -> Component.literal("[Aurum] Noche cambiada a " + state.id()
                + " | siguiente: " + data.next().id()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setNext(CommandSourceStack source, AurumWeatherState state) {
        ServerLevel level = skyLevel(source);
        if (level == null) {
            return 0;
        }
        AurumSkyData.get(level).forceNext(state);
        source.sendSuccess(() -> Component.literal("[Aurum] Siguiente noche: " + state.id()), true);
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /Aurum village ----------
    private static int villageStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AurumVillageData data = AurumVillageData.get(player.serverLevel());
        AurumVillageData.Village village = data.findNearest(player.blockPosition());
        if (village == null) {
            source.sendFailure(Component.literal("[Aurum] No hay ningún pueblo registrado a menos de "
                    + (int) AurumVillageData.VILLAGE_RADIUS + " bloques"));
            return 0;
        }
        int kills = data.getKills(village.id, player.getUUID());
        String msg = "[Aurum] Pueblo en " + village.center.toShortString()
                + " | golem: " + (village.golemAlive ? "viva" : "perdida")
                + " | tus muertes aquí: " + kills
                + " | enemigo permanente: " + (kills > 0 ? "sí" : "no")
                + " | precio de alquiler: x" + String.format(java.util.Locale.ROOT, "%.2f",
                data.priceMultiplier(village.id, player.getUUID()));
        source.sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int villageForgive(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AurumVillageData data = AurumVillageData.get(player.serverLevel());
        AurumVillageData.Village village = data.findNearest(player.blockPosition());
        if (village == null) {
            source.sendFailure(Component.literal("[Aurum] No hay ningún pueblo cerca"));
            return 0;
        }
        data.forgive(village.id, player.getUUID());
        source.sendSuccess(() -> Component.literal("[Aurum] Reputación borrada en el pueblo de " + village.center.toShortString()), true);
        return Command.SINGLE_SUCCESS;
    }
}