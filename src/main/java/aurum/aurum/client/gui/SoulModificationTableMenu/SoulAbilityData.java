package aurum.aurum.client.gui.SoulModificationTableMenu;

import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import java.util.Optional;

public record SoulAbilityData(
        int index,
        @Nullable String name, // <<< NUEVO ATRIBUTO
        @Nullable ResourceLocation abilityId,
        @Nullable Vec3i offset
) {

    // =========================================================================
    // 1. CODEC: Serialización para NBT (Guardado en disco)
    // =========================================================================

    // 1.1. CODEC para Vec3i (Sin cambios, pero incluido para completitud)
    public static final Codec<Vec3i> VEC3I_CODEC = RecordCodecBuilder.create(vecInstance -> vecInstance.group(
            Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
            Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
            Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
    ).apply(vecInstance, Vec3i::new));

    // 1.2. CODEC CORREGIDO para SoulAbilityData
    public static final Codec<SoulAbilityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(SoulAbilityData::index),
            // Serializa el String como opcional
            Codec.STRING.optionalFieldOf("name").forGetter(data -> Optional.ofNullable(data.name)),
            ResourceLocation.CODEC.optionalFieldOf("abilityId").forGetter(data -> Optional.ofNullable(data.abilityId)),
            VEC3I_CODEC.optionalFieldOf("offset").forGetter(data -> Optional.ofNullable(data.offset))
    ).apply(instance, (index, nameOpt, abilityIdOpt, offsetOpt) ->
            // Deserializa: Usa orElse(null) para restaurar el valor nulo si no está presente
            new SoulAbilityData(index, nameOpt.orElse(null), abilityIdOpt.orElse(null), offsetOpt.orElse(null))
    ));

    // =========================================================================
    // 2. STREAM CODEC: Serialización para Red
    // =========================================================================

    // 2.1. STREAM CODEC para Vec3i (Sin cambios)
    public static final StreamCodec<FriendlyByteBuf, Vec3i> VEC3I_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, Vec3i::getX,
            ByteBufCodecs.INT, Vec3i::getY,
            ByteBufCodecs.INT, Vec3i::getZ,
            Vec3i::new
    );

    // 2.2. STREAM CODEC CORREGIDO para SoulAbilityData
    public static final StreamCodec<FriendlyByteBuf, SoulAbilityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SoulAbilityData::index,
            // Serializa el String como opcional
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), data -> Optional.ofNullable(data.name()),
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), data -> Optional.ofNullable(data.abilityId()),
            ByteBufCodecs.optional(VEC3I_STREAM_CODEC), data -> Optional.ofNullable(data.offset()),
            (index, nameOpt, abilityIdOpt, offsetOpt) ->
                    // Deserializa: Usa orElse(null) para restaurar el valor nulo si no se recibió
                    new SoulAbilityData(index, nameOpt.orElse(null), abilityIdOpt.orElse(null), offsetOpt.orElse(null))
    );

    // =========================================================================
    // 3. MÉTODOS ADICIONALES ÚTILES
    // =========================================================================

    // Método para crear instancias sin valores nulos (opcional)
    public static SoulAbilityData of(int index, String name, ResourceLocation abilityId, Vec3i offset) {
        return new SoulAbilityData(index, name, abilityId, offset);
    }

    public static SoulAbilityData withName(int index, String name) {
        return new SoulAbilityData(index, name, null, null);
    }

    // Método que incluye el nombre
    public static SoulAbilityData withAbility(int index, String name, ResourceLocation abilityId) {
        return new SoulAbilityData(index, name, abilityId, null);
    }

    public static SoulAbilityData withOffset(int index, Vec3i offset) {
        return new SoulAbilityData(index, null, null, offset);
    }

    public static SoulAbilityData empty(int index) {
        return new SoulAbilityData(index, null, null, null);
    }

    // Métodos de verificación
    public boolean hasName() {
        return name != null;
    }

    public boolean hasAbility() {
        return abilityId != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }

    @Override
    public String toString() {
        return String.format("SoulAbilityData[index=%d, name=%s, abilityId=%s, offset=%s]",
                index, name, abilityId, offset);
    }
}