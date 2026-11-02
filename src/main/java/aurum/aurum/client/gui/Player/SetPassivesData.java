package aurum.aurum.client.gui.Player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Niveles de pasivas elegidos por el jugador. Existen aunque la barrera esté caída (entonces no se aplican). */
public record SetPassivesData(int health, int jump, int luminescence, int speed) {

    public static final int EXTRA_POINTS = 2;
    public static final int HEALTH = 0, JUMP = 1, LUMINESCENCE = 2, SPEED = 3;
    public static final int[] MIN = {1, 0, 0, 0};
    public static final int[] MAX = {3, 2, 2, 2};
    public static final SetPassivesData DEFAULT = new SetPassivesData(1, 0, 0, 0);

    public static final Codec<SetPassivesData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("health").forGetter(SetPassivesData::health),
            Codec.INT.fieldOf("jump").forGetter(SetPassivesData::jump),
            // optional: los datos guardados antes del cambio (clave "night_vision") siguen cargando, con luminiscencia 0
            Codec.INT.optionalFieldOf("luminescence", 0).forGetter(SetPassivesData::luminescence),
            Codec.INT.fieldOf("speed").forGetter(SetPassivesData::speed)
    ).apply(i, SetPassivesData::new));

    public static final StreamCodec<ByteBuf, SetPassivesData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetPassivesData::health,
            ByteBufCodecs.VAR_INT, SetPassivesData::jump,
            ByteBufCodecs.VAR_INT, SetPassivesData::luminescence,
            ByteBufCodecs.VAR_INT, SetPassivesData::speed,
            SetPassivesData::new);

    public SetPassivesData {
        health = clamp(health, HEALTH);
        jump = clamp(jump, JUMP);
        luminescence = clamp(luminescence, LUMINESCENCE);
        speed = clamp(speed, SPEED);
    }

    private static int clamp(int v, int i) { return Math.max(MIN[i], Math.min(MAX[i], v)); }

    public int get(int index) {
        return switch (index) { case 0 -> health; case 1 -> jump; case 2 -> luminescence; case 3 -> speed; default -> 0; };
    }

    /** Devuelve una copia con ese nivel cambiado (o this si el índice no existe). */
    public SetPassivesData with(int index, int level) {
        return switch (index) {
            case 0 -> new SetPassivesData(level, jump, luminescence, speed);
            case 1 -> new SetPassivesData(health, level, luminescence, speed);
            case 2 -> new SetPassivesData(health, jump, level, speed);
            case 3 -> new SetPassivesData(health, jump, luminescence, level);
            default -> this;
        };
    }

    public int spentPoints() {
        return (health - MIN[0]) + (jump - MIN[1]) + (luminescence - MIN[2]) + (speed - MIN[3]);
    }
}