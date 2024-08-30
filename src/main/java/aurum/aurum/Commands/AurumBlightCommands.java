package aurum.aurum.Commands;

import aurum.aurum.eventHandler.AurumBlightRain;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;


@EventBusSubscriber
public class AurumBlightCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Comando raíz /Aurum
        dispatcher.register(
                Commands.literal("Aurum")
                        .then(Commands.literal("startaurumrain")
                                .executes(context -> {
                                    AurumBlightRain.startAurumBlightRain();
                                    context.getSource().sendSuccess(() -> Component.nullToEmpty("La lluvia de añublo de Aurum comenzó!"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("stopaurumrain")
                                .executes(context -> {
                                    AurumBlightRain.stopAurumBlightRain();
                                    context.getSource().sendSuccess(() -> Component.nullToEmpty("La lluvia de añublo de Aurum finalizó!"), true);
                                    return 1;
                                })
                        )
        );
    }
}
