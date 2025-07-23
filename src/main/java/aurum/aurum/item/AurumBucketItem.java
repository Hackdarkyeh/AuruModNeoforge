package aurum.aurum.item;

import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlockEntity;
import aurum.aurum.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AurumBucketItem  extends BucketItem {
    private final int energyStored; // Energía que almacena este cubo
    public AurumBucketItem() {
        super(ModFluids.AURUM_ROSA.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.RARE));
        this.energyStored = 10; // Asigna la energía que contiene el cubo
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnergyGeneratorBlockEntity generator) {
                // Si es un generador de energía, almacenamos la energía en él
                //generator.addEnergy(this.energyStored, false); // Añadimos la energía del cubo al generador

                // Cambiamos el item al cubo vacío
                player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    public int getEnergyStored() {
        return energyStored;
    }
}

