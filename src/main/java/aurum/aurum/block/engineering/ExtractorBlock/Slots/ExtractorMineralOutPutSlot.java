package aurum.aurum.block.engineering.ExtractorBlock.Slots;

import aurum.aurum.init.ModBlocks;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

import static aurum.aurum.Aurum.MODID;

public class ExtractorMineralOutPutSlot extends Slot {
    private final Player player;
    private int removeCount;

    public ExtractorMineralOutPutSlot(Player pPlayer, Container pContainer, int pSlot, int pXPosition, int pYPosition) {
        super(pContainer, pSlot, pXPosition, pYPosition);
        this.player = pPlayer;
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        return false; // Impedir que el jugador coloque objetos en esta ranura
    }

    @Override
    public ItemStack remove(int pAmount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(pAmount, this.getItem().getCount());
        }
        return super.remove(pAmount);
    }

    @Override
    public void onTake(Player pPlayer, ItemStack pStack) {
        super.onTake(pPlayer, pStack);
        if (pStack.is(ModBlocks.AURELITE_ORE.asItem())) { // Verificar si el ítem extraído es Aurita
            this.grantAuritaAchievement(this.player);
        }
    }


    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {
        super.onQuickCraft(pStack, pAmount);
        this.removeCount += pAmount;
        if (pStack.is(ModBlocks.AURELITE_ORE.asItem())) { // Verificar si el ítem es Aurita
            this.grantAuritaAchievement(this.player);
        }
    }


    private void grantAuritaAchievement(Player pPlayer) {
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            Advancement advancement = Objects.requireNonNull(serverPlayer.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(MODID, "advancements/aurelite_ore_extractor"))).value();
            if (advancement != null) {
                serverPlayer.getAdvancements().award(Objects.requireNonNull(serverPlayer.server.getAdvancements().get(
                                ResourceLocation.fromNamespaceAndPath(MODID, "advancements/aurelite_ore_extractor"))),
                        "aurelite_ore_extractor");
            }
        }
    }

}


