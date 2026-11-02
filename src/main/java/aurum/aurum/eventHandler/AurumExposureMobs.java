package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Los mobs corruptos ignoran a los jugadores muy expuestos a Aurum (como si ya fueran uno de ellos).
 * La lista de mobs es el tag de entidades aurum:ignores_exposed_players (data/aurum/tags/entity_type/).
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumExposureMobs {

    public static final TagKey<EntityType<?>> IGNORES_EXPOSED_PLAYERS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "ignores_exposed_players"));

    /** Tramo de exposición a partir del cual te ignoran (HIGH = 75 %). */
    private static final AurumExposureTier IGNORE_FROM_TIER = AurumExposureTier.HIGH;
    /** Radio en el que se sueltan los objetivos ya fijados. */
    private static final double RELEASE_RADIUS = 32.0;

    private AurumExposureMobs() {}

    /** True si este mob debe ignorar a este jugador ahora mismo. */
    public static boolean ignores(Mob mob, Player player) {
        if (!mob.getType().is(IGNORES_EXPOSED_PLAYERS)) {
            return false;
        }
        if (mob.getLastHurtByMob() == player) {
            return false; // si lo hieres, se defiende
        }
        float exposure = player.getData(ModAttachments.AURUM_EXPOSURE);
        return AurumExposureTier.fromExposure(exposure).ordinal() >= IGNORE_FROM_TIER.ordinal();
    }

    /** Impide que fijen como objetivo a un jugador expuesto. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        // [COMPROBAR] nombres del evento en NeoForge 1.21.1 (getNewAboutToBeSetTarget / setNewAboutToBeSetTarget)
        if (event.getNewAboutToBeSetTarget() instanceof Player player && ignores(mob, player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Los que ya te perseguían te sueltan: una vez por segundo. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (AurumExposureHandler.tier(player).ordinal() < IGNORE_FROM_TIER.ordinal()) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(RELEASE_RADIUS),
                m -> m.getTarget() == player && ignores(m, player))) {
            mob.setTarget(null);
        }
    }
}