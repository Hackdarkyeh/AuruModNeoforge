package aurum.aurum.entity.AurumVillager;

import aurum.aurum.Red.VillagerActionPayload;
import aurum.aurum.Red.VillagerMenuStatePayload;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Lógica de servidor del menú de contratos: construye el estado y aplica (y valida) cada acción. */
public final class AurumContractService {

    /** Distancia máxima (al cuadrado) entre jugador y aldeano para aplicar una acción: 8 bloques. */
    private static final double MAX_DISTANCE_SQR = 64.0;

    private AurumContractService() {}

    public static void openMenu(AurumVillagerEntity villager, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, stateFor(villager, player, true)); // [COMPROBAR] firma en 1.21.1
    }

    public static VillagerMenuStatePayload stateFor(AurumVillagerEntity villager, ServerPlayer player, boolean open) {
        AurumContract contract = villager.getData(ModAttachments.VILLAGER_CONTRACT);
        int status = !contract.isActive() ? VillagerMenuStatePayload.STATUS_NONE
                : (contract.owner().equals(player.getUUID()) ? VillagerMenuStatePayload.STATUS_MINE
                : VillagerMenuStatePayload.STATUS_OTHER);

        int price = AurumContractConfig.dayPrice(AurumBankerTrading.priceMultiplier(villager, player));
        long current = status == VillagerMenuStatePayload.STATUS_MINE ? contract.ticksRemaining() : 0L;
        long max = (long) AurumContractConfig.MAX_CONTRACT_DAYS * AurumContractConfig.TICKS_PER_DAY;
        boolean canAddDay = current + AurumContractConfig.TICKS_PER_DAY <= max;

        return new VillagerMenuStatePayload(villager.getId(), open, villager.getProfession().ordinal(), status,
                contract.isActive() ? contract.ticksRemaining() : 0L, contract.follow(), price,
                countYanyitos(player), canAddDay, player.isCreative());
    }

    public static void handleAction(ServerPlayer player, VillagerActionPayload action) {
        Entity entity = player.level().getEntity(action.entityId());
        if (!(entity instanceof AurumVillagerEntity villager) || !villager.isAlive()
                || player.distanceToSqr(villager) > MAX_DISTANCE_SQR) {
            return;
        }
        if (villager.isHostileForm() || !AurumContractConfig.canBeHired(villager.getProfession())) {
            return;
        }

        AurumContract contract = villager.getData(ModAttachments.VILLAGER_CONTRACT);
        boolean mine = contract.isActive() && contract.owner().equals(player.getUUID());
        boolean other = contract.isActive() && !mine;

        switch (action.action()) {
            case VillagerActionPayload.ACTION_HIRE -> hire(villager, player, contract, mine, other);
            case VillagerActionPayload.ACTION_FOLLOW -> {
                if (mine) {
                    villager.setData(ModAttachments.VILLAGER_CONTRACT, contract.withFollow(!contract.follow()));
                    villager.getNavigation().stop();
                }
            }
            case VillagerActionPayload.ACTION_DISMISS -> {
                if (mine) {
                    villager.setData(ModAttachments.VILLAGER_CONTRACT, AurumContract.EMPTY);
                    villager.getNavigation().stop();
                }
            }
            default -> { }
        }
        PacketDistributor.sendToPlayer(player, stateFor(villager, player, false));
    }

    private static void hire(AurumVillagerEntity villager, ServerPlayer player, AurumContract contract,
                             boolean mine, boolean other) {
        if (other) {
            return;
        }
        int price = AurumContractConfig.dayPrice(AurumBankerTrading.priceMultiplier(villager, player));
        long current = mine ? contract.ticksRemaining() : 0L;
        long max = (long) AurumContractConfig.MAX_CONTRACT_DAYS * AurumContractConfig.TICKS_PER_DAY;
        if (current + AurumContractConfig.TICKS_PER_DAY > max) {
            return;
        }
        if (!player.isCreative() && !removeYanyitos(player, price)) { // en creativo es gratis, para probar
            return;
        }
        long total = current + AurumContractConfig.TICKS_PER_DAY;
        villager.setData(ModAttachments.VILLAGER_CONTRACT,
                mine ? contract.withTicks(total) : new AurumContract(player.getUUID(), total, true));
    }

    public static int countYanyitos(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.YANYITO.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Quita N yanyitos del inventario; false (sin quitar nada) si no tiene suficientes. */
    private static boolean removeYanyitos(ServerPlayer player, int amount) {
        if (countYanyitos(player) < amount) {
            return false;
        }
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (stack.is(ModItems.YANYITO.get())) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
        return true;
    }
}
