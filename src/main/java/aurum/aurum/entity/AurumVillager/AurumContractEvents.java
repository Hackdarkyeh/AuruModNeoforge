package aurum.aurum.entity.AurumVillager;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Contratos de aldeanos: el clic derecho abre el menú de interacción; el tiempo baja cada segundo
 * y un aldeano contratado nunca ataca a su dueño.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumContractEvents {

    private AurumContractEvents() {}

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getTarget() instanceof AurumVillagerEntity villager)) {
            return;
        }
        AurumVillagerEntity.Profession profession = villager.getProfession();
        if (profession == AurumVillagerEntity.Profession.BANKER
                || profession == AurumVillagerEntity.Profession.LIBRARIAN
                || villager.isHostileForm()) {
            return; // el banquero y el bibliotecario comercian; un aldeano hostil no se atiende
        }

        Level level = event.getLevel();
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!AurumContractConfig.canBeHired(profession)) {
            player.displayClientMessage(Component.translatable("aurum.contract.not_hireable"), true);
            return;
        }
        AurumContractService.openMenu(villager, player);
    }

    /** El tiempo del contrato baja cada segundo; al llegar a 0 el contrato termina. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) { // [COMPROBAR] EntityTickEvent.Post en NeoForge 1.21.1
        if (!(event.getEntity() instanceof AurumVillagerEntity villager) || villager.level().isClientSide()) {
            return;
        }
        if (villager.tickCount % 20 != 0) {
            return;
        }
        AurumContract contract = villager.getData(ModAttachments.VILLAGER_CONTRACT);
        if (!contract.isActive()) {
            return;
        }
        long left = contract.ticksRemaining() - 20;
        if (left > 0) {
            villager.setData(ModAttachments.VILLAGER_CONTRACT, contract.withTicks(left));
            return;
        }

        villager.setData(ModAttachments.VILLAGER_CONTRACT, AurumContract.EMPTY);
        villager.getNavigation().stop();
        Player owner = villager.level().getPlayerByUUID(contract.owner());
        if (owner instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("aurum.contract.expired", villager.getDisplayName()), false);
        }
    }

    /** Un aldeano contratado nunca elige a su dueño como objetivo (ni aunque se vuelva hostil). */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof AurumVillagerEntity villager
                && event.getNewAboutToBeSetTarget() instanceof Player target) {
            AurumContract contract = villager.getData(ModAttachments.VILLAGER_CONTRACT);
            if (contract.isActive() && contract.owner().equals(target.getUUID())) {
                event.setNewAboutToBeSetTarget(null);
            }
        }
    }
}
