package aurum.aurum.item.projectile;

import aurum.aurum.entity.SkeletonAltered.AurumArrow;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

public class AurumArrowItem extends ArrowItem {

    public AurumArrowItem(Properties properties) {
        super(properties);
    }

    /** Arco de jugador, ballesta, etc. */
    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new AurumArrow(level, shooter, ammo, weapon);
    }

    /** Dispensador. */
    @Override
    public AbstractArrow asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        AurumArrow arrow = new AurumArrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}