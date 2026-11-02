package aurum.aurum.Commands;

import aurum.aurum.Aurum;
import aurum.aurum.eventHandler.AurumExposureHandler;
import aurum.aurum.eventHandler.AurumExposureTier;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /Aurum exposure status | set <valor> | volcano. Se fusiona con el nodo raíz /Aurum que ya existe. */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumExposureCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("Aurum")
                        .then(Commands.literal("exposure")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("status")
                                        .executes(AurumExposureCommands::status))
                                .then(Commands.literal("volcano")
                                        .executes(AurumExposureCommands::volcano))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value",
                                                        FloatArgumentType.floatArg(0.0f, AurumExposureHandler.MAX_EXPOSURE))
                                                .executes(AurumExposureCommands::set)))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        float exposure = AurumExposureHandler.get(p);
        float rate = AurumExposureHandler.currentRatePerSecond(p);
        AurumExposureTier tier = AurumExposureTier.fromExposure(exposure);
        String change = rate > 0.0f
                ? String.format("sube %.2f por segundo", rate)
                : (exposure > 0.0f ? String.format("baja %.2f por segundo", AurumExposureHandler.DECAY_PER_SECOND) : "estable");
        String msg = String.format("Exposición: %.1f / %.0f (%.0f %%) | tramo: %s | ahora %s",
                exposure, AurumExposureHandler.MAX_EXPOSURE,
                100.0f * exposure / AurumExposureHandler.MAX_EXPOSURE, tier.label, change);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Para calibrar el volcán: bioma, ruido en tu posición y radiación local. */
    private static int volcano(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String biomeId = p.level().getBiome(p.blockPosition()).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        String msg = String.format("Bioma: %s | en bioma volcán: %s | ruido: %.3f | radiación volcán: %.2f por segundo (sin barrera)",
                biomeId, AurumExposureHandler.inVolcanoBiome(p), AurumExposureHandler.volcanoNoise(p),
                AurumExposureHandler.volcanoRate(p));
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        float value = FloatArgumentType.getFloat(ctx, "value");
        AurumExposureHandler.set(p, value);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("Exposición fijada a %.1f", value)), true);
        return 1;
    }
}