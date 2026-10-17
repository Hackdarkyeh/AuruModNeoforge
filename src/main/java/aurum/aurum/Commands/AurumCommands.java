package aurum.aurum.Commands;

import aurum.aurum.Red.SyncAurumBloodPayload;
import aurum.aurum.Red.SyncSoulContainerPayload;
import aurum.aurum.eventHandler.AurumBlightRain;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.Soul.SoulData;
import aurum.aurum.utils.AurumUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class AurumCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Comando raíz /Aurum
        dispatcher.register(
                Commands.literal("Aurum")
                        // Nivel 2 de permiso = Administrador/OP
                        .requires(source -> source.hasPermission(2))

                        // --- COMANDOS EXISTENTES ---
                        .then(Commands.literal("startaurumrain")
                                .executes(context -> {
                                    AurumBlightRain.startAurumBlightRain();
                                    context.getSource().sendSuccess(() -> Component.literal("¡La lluvia de añublo de Aurum comenzó!"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("stopaurumrain")
                                .executes(context -> {
                                    AurumBlightRain.stopAurumBlightRain();
                                    context.getSource().sendSuccess(() -> Component.literal("¡La lluvia de añublo de Aurum finalizó!"), true);
                                    return 1;
                                })
                        )

                        // --- NUEVO COMANDO: /Aurum sethonor <valor> ---
                        .then(Commands.literal("sethonor")
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0)) // Mínimo 0 puntos
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            ServerPlayer player = source.getPlayerOrException();

                                            // Restricción: Solo si está en Creativo
                                            if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                                                source.sendFailure(Component.literal("¡Este comando solo se puede usar en modo Creativo!"));
                                                return 0;
                                            }

                                            int nuevoHonor = IntegerArgumentType.getInteger(context, "valor");

                                            // Modificar el honor dentro del alma activa del jugador
                                            var soulContainer = player.getData(ModAttachments.ACTIVE_SOUL);
                                            ItemStack soulStack = soulContainer.soulStack();

                                            if (soulStack.isEmpty()) {
                                                source.sendFailure(Component.literal("No tienes ninguna expansión de alma equipada para modificar su honor."));
                                                return 0;
                                            }

                                            SoulData oldData = soulContainer.activeSoul();
                                            // Creamos un nuevo SoulData manteniendo todo lo demás idéntico pero cambiando el honor
                                            SoulData newData = new SoulData(oldData.idTier(), nuevoHonor, oldData.maxHonor(), oldData.maxWeight(), oldData.abilities());

                                            // Guardamos los datos actualizados en el item y en el attachment
                                            soulStack.set(aurum.aurum.init.ModDataComponents.SOUL_COMPONENT.get(), newData);
                                            player.setData(ModAttachments.ACTIVE_SOUL, soulContainer);

                                            // Sincronizamos con el cliente para que el HUD se entere
                                            player.connection.send(new SyncSoulContainerPayload(soulContainer));

                                            source.sendSuccess(() -> Component.literal("Honor del alma establecido en: " + nuevoHonor), true);
                                            return 1;
                                        })
                                )
                        )

                        // --- NUEVO COMANDO: /Aurum setblood <valor> ---
                        .then(Commands.literal("setblood")
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            ServerPlayer player = source.getPlayerOrException();

                                            int valorSolicitado = IntegerArgumentType.getInteger(context, "valor");
                                            int maxAurum = AurumUtils.calculateMaxAurum(player);

                                            int nuevaSangre = Math.min(valorSolicitado, maxAurum);

                                            player.setData(ModAttachments.AURUM_BLOOD, nuevaSangre);
                                            player.connection.send(new SyncAurumBloodPayload(nuevaSangre));

                                            if (valorSolicitado > maxAurum) {
                                                source.sendSuccess(() -> Component.literal(
                                                        "El valor solicitado (" + valorSolicitado + ") supera el máximo permitido (" + maxAurum + "). " +
                                                                "Se ha ajustado la Sangre Aurum al máximo: " + nuevaSangre), true);
                                            } else {
                                                source.sendSuccess(() -> Component.literal("Sangre Aurum establecida en: " + nuevaSangre), true);
                                            }

                                            return 1;
                                        })
                                )
                        )
        );
    }
}