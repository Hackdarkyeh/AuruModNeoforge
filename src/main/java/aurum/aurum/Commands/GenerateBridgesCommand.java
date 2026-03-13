package aurum.aurum.Commands;

import aurum.aurum.structures.FortressBridgeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

/**
 * Comando para generar puentes de fortaleza manualmente
 * Uso: /generatebridges <x> <y> <z>
 */
public class GenerateBridgesCommand {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("Aurum").then(Commands.literal("generatebridges")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2)) // Requiere nivel 2 (admin)
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(context -> generateBridges(context, BlockPosArgument.getLoadedBlockPos(context, "pos"))))
                .executes(context -> generateBridgesAtPlayer(context))
                )
        );
    }

    private static int generateBridgesAtPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("Este comando solo funciona en el servidor"));
            return 0;
        }

        BlockPos playerPos = new BlockPos(
            (int) source.getPosition().x,
            (int) source.getPosition().y,
            (int) source.getPosition().z
        );

        return generateBridges(context, playerPos);
    }

    private static int generateBridges(CommandContext<CommandSourceStack> context, BlockPos pos) {
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("Este comando solo funciona en el servidor"));
            return 0;
        }

        try {
            source.sendSuccess(() -> Component.literal("Generando puentes en: " + pos), false);
            FortressBridgeGenerator.generateBridges(serverLevel, pos, pos.getY());
            source.sendSuccess(() -> Component.literal("✓ Puentes generados exitosamente"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error al generar puentes: " + e.getMessage()));
            return 0;
        }
    }
}

