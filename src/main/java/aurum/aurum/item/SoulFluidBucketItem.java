package aurum.aurum.item;

import aurum.aurum.init.ModFluids;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;

public class SoulFluidBucketItem extends BucketItem {
    public SoulFluidBucketItem() {
        super(ModFluids.SOUL_FLUID.get(), new Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Fall back to the default bucket behavior, which handles placing the fluid.
        return super.useOn(context);
    }
}