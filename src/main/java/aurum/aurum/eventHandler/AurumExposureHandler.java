package aurum.aurum.eventHandler;

import aurum.aurum.Aurum;
import aurum.aurum.Red.SyncAurumExposurePayload;
import aurum.aurum.eventHandler.ArmorEvents.ArmorBlightShield;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModFluids;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents;
import aurum.aurum.worldgen.Dimension.AurumWeatherEvents.AurumWeatherState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Exposición a Aurum: se acumula por la radiación ambiental de dim1 y por fuentes locales (volcán, fluido),
 * la frena la Barrera de Corrosión y baja sola, despacio, cuando no hay radiación.
 * El valor vive en ModAttachments.AURUM_EXPOSURE y se sincroniza al cliente.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumExposureHandler {

    // ── Valores ajustables: radiación ambiental ──────────────────────────────
    /** Exposición que se gana por segundo en cada estado (los demás estados no emiten). */
    public static final float PURPLE_MOON_RATE = 0.5f;
    public static final float AURUM_RAIN_RATE = 0.25f;

    // ── Valores ajustables: volcán ───────────────────────────────────────────
    /** Radiación por segundo en el cráter (máximo). */
    public static final float VOLCANO_MAX_RATE = 3.0f;
    /** Valor del ruido del volcán donde empieza a notarse la radiación (0 radiación aquí). Calíbralo con /Aurum exposure volcano. */
    public static final double VOLCANO_START_N = 0.50;
    /** Valor del ruido a partir del cual la radiación es máxima (0.89 = borde del cráter, ver VolcanoCraterFeature). */
    public static final double VOLCANO_FULL_N = 0.89;
    /** 1 = sube de forma lineal; mayor que 1 = sube más deprisa cuanto más cerca del cráter. */
    public static final float VOLCANO_CURVE = 1.5f;

    // ── Valores ajustables: fluido de Aurum ──────────────────────────────────
    /** Radiación por segundo con los pies dentro del fluido. */
    public static final float FLUID_FEET_RATE = 1.5f;
    /** Radiación por segundo con la cabeza sumergida en el fluido. */
    public static final float FLUID_SUBMERGED_RATE = 3.0f;
    /** false = el fluido irradia en cualquier dimensión; true = solo en dim1. */
    public static final boolean FLUID_ONLY_IN_DIM1 = false;

    // ── Valores ajustables: barrera y bajada ─────────────────────────────────
    /** Con la Barrera de Corrosión activa se gana esta fracción. */
    public static final float BARRIER_FACTOR = 0.1f;
    /** Exposición que baja por segundo cuando no se gana nada (mucho más lenta que la subida). */
    public static final float DECAY_PER_SECOND = 0.2f;
    public static final float MAX_EXPOSURE = 1000.0f;
    /** Cada cuántos ticks se acumula (20 = 1 s; las tasas de arriba son por segundo). */
    private static final int INTERVAL_TICKS = 20;

    private static final ResourceKey<Level> DIM1 =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dimension1"));
    // [COMPROBAR] ids de los biomas: se deducen de los nombres de archivo dim1_biome1_volcano*.json
    private static final ResourceKey<Biome> VOLCANO =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dim1_biome1_volcano"));
    private static final ResourceKey<Biome> VOLCANO_AROUND =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dim1_biome1_volcano_around"));

    private AurumExposureHandler() {}

    public static float get(ServerPlayer p) {
        return p.getData(ModAttachments.AURUM_EXPOSURE);
    }

    /** Tramo actual de exposición del jugador. */
    public static AurumExposureTier tier(ServerPlayer p) {
        return AurumExposureTier.fromExposure(get(p));
    }

    /** Fija la exposición (con tope), la guarda y la manda al cliente. */
    public static void set(ServerPlayer p, float value) {
        float v = Math.max(0.0f, Math.min(MAX_EXPOSURE, value));
        p.setData(ModAttachments.AURUM_EXPOSURE, v);
        PacketDistributor.sendToPlayer(p, new SyncAurumExposurePayload(v)); // [COMPROBAR] firma en 1.21.1
    }

    /** Radiación ambiental de la noche actual (sin barrera). */
    private static float ambientRate(ServerPlayer p) {
        AurumWeatherState state = AurumWeatherEvents.getCurrentWeatherState(); // [COMPROBAR] firma y que sirva en el servidor
        if (state == null) {
            return 0.0f;
        }
        switch (state) {
            case PURPLE_MOON:
                return PURPLE_MOON_RATE; // radia aunque estés a cubierto
            case AURUM_RAIN:
                return p.level().canSeeSky(p.blockPosition()) ? AURUM_RAIN_RATE : 0.0f;
            default:
                return 0.0f; // STANDARD_NIGHT, DARK_NIGHT y NO_MOON no emiten
        }
    }

    /** Valor del ruido del volcán en la posición del jugador (el mismo que usa VolcanoCraterFeature). NaN si no aplica. */
    public static double volcanoNoise(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) {
            return Double.NaN;
        }
        DensityFunction noise = level.getChunkSource().randomState().router().lavaNoise();
        return noise.compute(new DensityFunction.SinglePointContext(Mth.floor(p.getX()), 0, Mth.floor(p.getZ())));
    }

    /** True si el jugador está en el bioma del volcán o en el de alrededor. */
    public static boolean inVolcanoBiome(ServerPlayer p) {
        Holder<Biome> biome = p.level().getBiome(p.blockPosition());
        return biome.is(VOLCANO) || biome.is(VOLCANO_AROUND);
    }

    /** Radiación local del volcán (sin barrera): 0 en el borde exterior, máxima en el cráter. */
    public static float volcanoRate(ServerPlayer p) {
        if (!inVolcanoBiome(p)) {
            return 0.0f;
        }
        double n = volcanoNoise(p);
        if (Double.isNaN(n)) {
            return 0.0f;
        }
        double t = Mth.clamp((n - VOLCANO_START_N) / (VOLCANO_FULL_N - VOLCANO_START_N), 0.0, 1.0);
        return (float) (VOLCANO_MAX_RATE * Math.pow(t, VOLCANO_CURVE));
    }

    /** True si en esa posición hay fluido de Aurum (fuente o fluyendo). */
    private static boolean isAurumFluidAt(Level level, BlockPos pos) {
        Fluid aurum = ModFluids.AURUM_ROSA.get();
        // [COMPROBAR] FlowingFluid.isSame devuelve true para la fuente y para el fluido fluyendo
        return aurum.isSame(level.getFluidState(pos).getType());
    }

    /** Radiación del fluido de Aurum (sin barrera): más si tienes la cabeza sumergida. */
    public static float fluidRate(ServerPlayer p) {
        Level level = p.level();
        if (isAurumFluidAt(level, BlockPos.containing(p.getEyePosition()))) {
            return FLUID_SUBMERGED_RATE;
        }
        if (isAurumFluidAt(level, p.blockPosition())) {
            return FLUID_FEET_RATE;
        }
        return 0.0f;
    }

    /** Exposición que ganaría ahora mismo este jugador por segundo (ya con la barrera aplicada). */
    public static float currentRatePerSecond(ServerPlayer p) {
        boolean inDim1 = p.level().dimension().equals(DIM1);

        float rate = 0.0f;
        if (inDim1) {
            rate += ambientRate(p) + volcanoRate(p);
        }
        if (inDim1 || !FLUID_ONLY_IN_DIM1) {
            rate += fluidRate(p);
        }

        if (rate > 0.0f && ArmorBlightShield.hasBarrier(p)) {
            rate *= BARRIER_FACTOR;
        }
        return rate;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (p.tickCount % INTERVAL_TICKS != 0 || p.isCreative() || p.isSpectator()) {
            return;
        }
        float rate = currentRatePerSecond(p);
        if (rate > 0.0f) {
            set(p, get(p) + rate);
        } else {
            // Sin radiación: la exposición baja sola, despacio
            float current = get(p);
            if (current > 0.0f) {
                set(p, current - DECAY_PER_SECOND);
            }
        }
    }

    /** Al entrar al mundo el cliente recibe su exposición guardada. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new SyncAurumExposurePayload(get(p)));
        }
    }
}