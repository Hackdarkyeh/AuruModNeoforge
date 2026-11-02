package aurum.aurum.eventHandler.luminescence;

import aurum.aurum.client.gui.Player.ArmorSkillsData;
import aurum.aurum.client.gui.Player.ClientboundLuminescencePacket;
import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.eventHandler.ArmorEvents.ArmorBlightShield;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.AurumArmor;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Pasiva Luminiscencia (lado servidor). Cada SCAN_INTERVAL ticks, para el jugador con la pasiva activa:
 * busca entidades hostiles en el radio, calcula su amenaza con el equipo REAL del jugador y envía al cliente
 * solo la lista id → nivel cuando cambia. El render lo hacen los mixins del cliente.
 *
 * Amenaza = max( ratio, energía ):
 *   ratio   = (tiempo que tardas en matarla) / (tiempo que tarda en matarte); <0.5 verde, <1 amarillo, >=1 rojo
 *   energía = golpes que aguanta tu pieza con menos energía; <=3 rojo, <=6 amarillo como mínimo
 */
public final class LuminescenceScanner {
    private LuminescenceScanner() {}

    public static final int SCAN_INTERVAL = 10;
    public static final int GREEN = 0, YELLOW = 1, RED = 2;

    /** Radio por nivel de la pasiva (índice = nivel). */
    private static final double[] RADIUS = {0.0, 20.0, 30.0};

    private static final double RED_RATIO = 1.0;
    private static final double YELLOW_RATIO = 0.5;
    private static final double RED_ENERGY_HITS = 3.0;
    private static final double YELLOW_ENERGY_HITS = 6.0;

    /** Último mapa enviado a cada jugador (solo hilo del servidor). Ausente = el cliente tiene el mapa vacío. */
    private static final Map<UUID, Int2IntOpenHashMap> LAST = new HashMap<>();

    // ── API ──────────────────────────────────────────────────────────────────

    public static void scan(ServerPlayer p) {
        int lvl = Mth.clamp(p.getData(ModDataComponents.SET_PASSIVES).luminescence(), 0, RADIUS.length - 1);
        Int2IntOpenHashMap result = new Int2IntOpenHashMap();

        if (lvl > 0 && ArmorBlightShield.hasBarrier(p)) {
            double r = RADIUS[lvl];
            Me me = Me.of(p);
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                    x -> x != p && x.isAlive())) {
                if (p.distanceToSqr(e) > r * r) continue;   // distancia 3D (esfera)
                if (!isHostile(p, e)) continue;
                int level = evaluate(me, p, e);
                if (level >= 0) result.put(e.getId(), level);
            }
        }
        UUID id = p.getUUID();
        Int2IntOpenHashMap prev = LAST.get(id);
        if (prev == null) {
            if (result.isEmpty()) return;
        } else if (prev.equals(result)) {
            return;
        }
        if (result.isEmpty()) LAST.remove(id); else LAST.put(id, result);
        send(p, result);
    }

    /** Respawn o cambio de dimensión: las ids de entidad ya no valen, se vacía el mapa del cliente. */
    public static void reset(ServerPlayer p) {
        LAST.remove(p.getUUID());
        send(p, new Int2IntOpenHashMap());
    }

    public static void forget(ServerPlayer p) {
        LAST.remove(p.getUUID());
    }

    private static void send(ServerPlayer p, Int2IntOpenHashMap map) {
        int[] ids = new int[map.size()];
        byte[] levels = new byte[map.size()];
        int i = 0;
        for (Int2IntMap.Entry en : map.int2IntEntrySet()) {
            ids[i] = en.getIntKey();
            levels[i++] = (byte) en.getIntValue();
        }
        PacketDistributor.sendToPlayer(p, new ClientboundLuminescencePacket(ids, levels));
    }

    // ── ¿Cuenta como peligro? ────────────────────────────────────────────────

    private static boolean isHostile(ServerPlayer p, LivingEntity e) {
        if (e instanceof Player other) return !other.isSpectator();
        if (!(e instanceof Mob m)) return false;

        // Corruptos y jefes: siempre que estén en el radio (aviso temprano)
        if (m instanceof ICorruptedMob || m.getType().is(Tags.EntityTypes.BOSSES)) return true;

        boolean targetsMe = targetOf(m) == p;
        // Neutrales (también los "Enemy" neutrales como endermen o piglins zombificados): solo si te tienen de objetivo
        if (m instanceof NeutralMob || m instanceof AbstractPiglin) return targetsMe;
        if (m instanceof Enemy) return true;
        return targetsMe; // cualquier otro mob que vaya a por ti
    }

    /** Objetivo del mob: el campo clásico o, si usa Brain (piglins, hoglins, warden…), la memoria de ataque. */
    @Nullable
    private static LivingEntity targetOf(Mob m) {
        LivingEntity t = m.getTarget();
        if (t != null) return t;
        Brain<?> brain = m.getBrain();
        return brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                ? brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)
                : null;
    }

    // ── Amenaza ──────────────────────────────────────────────────────────────

    /** Tu estado, calculado una vez por escaneo. */
    private record Me(double hp, double soak, float armor, float toughness, double hit, double speed,
                      float aurumMult, List<ItemStack> pieces) {
        static Me of(ServerPlayer p) {
            List<ItemStack> pieces = new ArrayList<>(4);
            for (ItemStack s : p.getArmorSlots()) pieces.add(s);
            return new Me(
                    p.getHealth() + p.getAbsorptionAmount(),
                    ArmorBlightShield.aurumSoak(p),
                    (float) p.getAttributeValue(Attributes.ARMOR),
                    (float) p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
                    p.getAttributeValue(Attributes.ATTACK_DAMAGE),   // arma en mano y Fuerza incluidos
                    p.getAttributeValue(Attributes.ATTACK_SPEED),
                    ArmorBlightShield.aurumDamageMultiplier(p),
                    pieces);
        }
    }

    /** @return GREEN/YELLOW/RED, o -1 si la entidad no puede hacerte daño. */
    private static int evaluate(Me me, ServerPlayer p, LivingEntity e) {
        Difficulty diff = p.level().getDifficulty();
        ThreatProfileLoader.Profile prof = ThreatProfileLoader.get(e);

        double physDps = 0, aurumDps = 0;   // fisico: ya con tu armadura. aurum: ya con la exposición
        double physHit = 0, aurumHit = 0;   // golpe individual más grande de cada tipo (para la energía)

        // 1. Cuerpo a cuerpo (atributo de la entidad; en jugadores incluye el arma que lleven)
        AttributeInstance atk = e.getAttribute(Attributes.ATTACK_DAMAGE);
        double meleeSecs = meleeSeconds(e, prof);
        if (atk != null && meleeSecs > 0) {
            double hit = e instanceof Player ? atk.getValue() : scaleDifficulty(atk.getValue(), diff);
            physHit = Math.max(physHit, hit);
            physDps += armorReduced(hit, me.armor(), me.toughness()) / meleeSecs;
        }

        // 2. A distancia / explosiones (tabla del datapack)
        if (prof != null && prof.hasRanged()) {
            double secs = prof.rangedSeconds(diff);
            if (prof.aurum()) {
                double hit = prof.damage() * me.aurumMult();
                aurumHit = Math.max(aurumHit, hit);
                aurumDps += hit / secs;
            } else {
                double hit = scaleDifficulty(prof.damage(), diff);
                physHit = Math.max(physHit, hit);
                physDps += armorReduced(hit, me.armor(), me.toughness()) / secs;
            }
        }

        // 3. Plaga de Aurum de los corruptos (AurumBlight: 2 * (amp + 1) cada 2 s; amp 1 si su valor >= 10)
        if (e instanceof ICorruptedMob c && c.getAurumValue() > 0) {
            aurumDps += (c.getAurumValue() >= 10.0 ? 2.0 : 1.0) * me.aurumMult();
        }

        if (physDps <= 0 && aurumDps <= 0) return -1;

        // Tiempo que tardas en matarla / que tarda en matarte
        float foeArmor = (float) e.getAttributeValue(Attributes.ARMOR);
        float foeTough = (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double myDps = armorReduced(me.hit(), foeArmor, foeTough) * me.speed();
        double ttk = myDps > 0 ? (e.getHealth() + e.getAbsorptionAmount()) / myDps : Double.POSITIVE_INFINITY;
        double ttd = timeToDie(me.hp(), me.soak(), physDps, aurumDps);
        double ratio = ttk / ttd;

        int level = ratio >= RED_RATIO ? RED : ratio >= YELLOW_RATIO ? YELLOW : GREEN;

        // Energía: si pocos golpes te dejan una pieza sin energía (= sin armadura), es peligroso aunque aguantes de vida
        double energyHits = hitsToDrain(me.pieces(), physHit, aurumHit);
        if (energyHits <= RED_ENERGY_HITS) level = RED;
        else if (energyHits <= YELLOW_ENERGY_HITS) level = Math.max(level, YELLOW);
        return level;
    }

    /** Segundos hasta que te mata: el daño físico va a la vida, el de Aurum primero gasta la barrera. */
    private static double timeToDie(double hp, double soak, double phys, double aurum) {
        if (aurum <= 0) return hp / phys;
        if (phys <= 0) return (hp + soak) / aurum;
        double tSoak = soak / aurum;          // cuándo se agota la barrera
        double tHp = hp / phys;               // si solo contara el daño físico
        if (tHp <= tSoak) return tHp;
        return (hp + soak) / (phys + aurum);
    }

    /**
     * Golpes (de la entidad) hasta que alguna pieza se queda sin energía. Replica los gastos reales:
     * ArmorHitEnergyHandler (físico: daño x ENERGY_LOSS_PER_DAMAGE entre las piezas, redondeando hacia arriba) y
     * ArmorBlightShield.wearArmor (Aurum: daño / piezas x desgaste, con la Resonancia de cada pieza).
     */
    private static double hitsToDrain(List<ItemStack> pieces, double physHit, double aurumHit) {
        double best = Double.POSITIVE_INFINITY;
        int n = Math.max(1, pieces.size());
        if (physHit > 0) {
            int total = Math.max(1, Math.round((float) (physHit * ModArmorItem.ENERGY_LOSS_PER_DAMAGE)));
            int perPiece = (total + n - 1) / n;
            for (ItemStack s : pieces) best = Math.min(best, (double) AurumArmor.getEnergy(s) / perPiece);
        }
        if (aurumHit > 0) {
            for (ItemStack s : pieces) {
                float resonance = s.getOrDefault(ModDataComponents.ARMOR_SKILLS.get(), ArmorSkillsData.defaultData())
                        .getResonanceBonusMultiplier();
                float ratio = Math.max(0.01F, EnergyConfig.Barrier.ENERGY_WEAR_PER_DAMAGE * (1F - resonance));
                int loss = Math.max(1, Math.round((float) (aurumHit / n * ratio)));
                best = Math.min(best, (double) AurumArmor.getEnergy(s) / loss);
            }
        }
        return best;
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static double meleeSeconds(LivingEntity e, @Nullable ThreatProfileLoader.Profile prof) {
        if (e instanceof Player) {
            AttributeInstance speed = e.getAttribute(Attributes.ATTACK_SPEED);
            return speed != null && speed.getValue() > 0 ? 1.0 / speed.getValue() : 0.5;
        }
        int ticks = prof != null ? prof.meleeInterval() : 20;   // los mobs vanilla pegan ~1 vez por segundo
        return ticks <= 0 ? 0 : ticks / 20.0;
    }

    /** Daño de mob a jugador según la dificultad (misma regla que Player.hurt). */
    private static double scaleDifficulty(double dmg, Difficulty d) {
        return switch (d) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(dmg / 2.0 + 1.0, dmg);
            case NORMAL -> dmg;
            case HARD -> dmg * 1.5;
        };
    }

    /** Fórmula de armadura de vanilla (CombatRules.getDamageAfterAbsorb sin encantamientos). */
    private static double armorReduced(double dmg, float armor, float toughness) {
        float d = (float) dmg;
        float f = 2.0F + toughness / 4.0F;
        float f1 = Mth.clamp(armor - d / f, armor * 0.2F, 20.0F);
        return d * (1.0F - f1 / 25.0F);
    }
}