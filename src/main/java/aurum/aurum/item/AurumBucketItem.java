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

        // First, check if the block is an EnergyGeneratorBlockEntity.
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof EnergyGeneratorBlockEntity generator) {
            if (!world.isClientSide) {
                // Uncomment the code to add energy if you want to use this functionality.
                // generator.addEnergy(this.energyStored, false);

                // Check if the player is in creative mode before changing the item.
                // This prevents unexpected behavior.
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        // If it's not a generator, fall back to the default bucket behavior.
        return super.useOn(context);
    }

    public int getEnergyStored() {
        return energyStored;
    }
}

