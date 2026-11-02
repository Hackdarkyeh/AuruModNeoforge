package aurum.aurum.entity.AurumVillager;

import aurum.aurum.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * El aldeano con contrato sigue a su dueño (en la misma dimensión), si el contrato tiene "seguir" activado,
 * y si se queda muy atrás se teletransporta a su lado.
 */
public class FollowContractOwnerGoal extends Goal {

    /** Si el dueño se aleja más de esto, el aldeano se teletransporta a su lado. */
    private static final double TELEPORT_DISTANCE = 24.0;

    private final AurumVillagerEntity villager;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    @Nullable
    private Player owner;
    private int recalcDelay;

    public FollowContractOwnerGoal(AurumVillagerEntity villager, double speed, float startDistance, float stopDistance) {
        this.villager = villager;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    private Player findOwner() {
        AurumContract contract = villager.getData(ModAttachments.VILLAGER_CONTRACT);
        if (!contract.isActive() || !contract.follow()) {
            return null;
        }
        Player player = villager.level().getPlayerByUUID(contract.owner());
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }

    @Override
    public boolean canUse() {
        owner = findOwner();
        return owner != null && villager.distanceToSqr(owner) > (double) (startDistance * startDistance);
    }

    @Override
    public boolean canContinueToUse() {
        owner = findOwner();
        return owner != null && villager.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        recalcDelay = 0;
    }

    @Override
    public void stop() {
        owner = null;
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        villager.getLookControl().setLookAt(owner, 10.0F, (float) villager.getMaxHeadXRot());
        if (--recalcDelay > 0) {
            return;
        }
        recalcDelay = adjustedTickDelay(10);

        if (villager.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            tryTeleportNearOwner();
        } else {
            villager.getNavigation().moveTo(owner, speed);
        }
    }

    private void tryTeleportNearOwner() {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = villager.getRandom().nextIntBetweenInclusive(-3, 3);
            int dz = villager.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue; // no encima del dueño
            }
            BlockPos target = base.offset(dx, 0, dz);
            boolean groundBelow = !villager.level().getBlockState(target.below()).isAir();
            boolean freeSpace = villager.level().noCollision(villager,
                    villager.getBoundingBox().move(target.getX() + 0.5 - villager.getX(),
                            target.getY() - villager.getY(), target.getZ() + 0.5 - villager.getZ()));
            if (groundBelow && freeSpace) {
                villager.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, villager.getYRot(), villager.getXRot());
                villager.getNavigation().stop();
                return;
            }
        }
    }
}
