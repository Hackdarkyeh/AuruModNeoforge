package aurum.aurum.entity.AurumVillager;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModAttachments;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /Aurum contract set <aldeano> <ticks>: contrato de prueba a nombre de quien ejecuta el comando (0 lo termina). */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumContractCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("Aurum")
                        .then(Commands.literal("contract")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("ticks", LongArgumentType.longArg(0))
                                                        .executes(AurumContractCommands::set))))));
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity target = EntityArgument.getEntity(ctx, "target");
        long ticks = LongArgumentType.getLong(ctx, "ticks");
        if (!(target instanceof AurumVillagerEntity villager)) {
            ctx.getSource().sendFailure(Component.literal("Ese objetivo no es un aldeano de Aurum"));
            return 0;
        }
        villager.setData(ModAttachments.VILLAGER_CONTRACT,
                ticks > 0 ? new AurumContract(player.getUUID(), ticks, true) : AurumContract.EMPTY);
        ctx.getSource().sendSuccess(() -> Component.literal("Contrato fijado a " + ticks + " ticks"), true);
        return 1;
    }
}
