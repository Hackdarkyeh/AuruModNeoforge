package aurum.aurum.entity.EnderDragonAltered;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface DragonAlteredPhaseInstance {
    boolean isSitting();

    void doClientTick();

    void doServerTick();

    void onCrystalDestroyed(EndCrystal crystal, BlockPos pos, DamageSource damageSource, @Nullable Player player);

    void begin();

    void end();

    float getFlySpeed();

    float getTurnSpeed();

    EnderDragonAlteredPhase<? extends DragonAlteredPhaseInstance> getPhase();

    @Nullable
    Vec3 getFlyTargetLocation();

    float onHurt(DamageSource damageSource, float amount);
}
