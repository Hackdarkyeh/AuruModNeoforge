package aurum.aurum.Soul;

import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;

import java.util.ArrayList;
import java.util.List;

// Ejemplo de la clase que contendrá el estado del alma, serializable
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf; // Importación necesaria para el StreamCodec
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

// Clase que se almacenará en el DataComponent
public record SoulData(List<SoulAbilityData> abilities) {

    // Codec principal (para TO_NBT, TO_JSON, etc. - Codec)
    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SoulAbilityData.CODEC.listOf().fieldOf("abilities").forGetter(SoulData::abilities)
            ).apply(instance, SoulData::new)
    );

    // StreamCodec principal (para serialización de red - StreamCodec)
    // Dentro de SoulAbilityData.java
    public static final StreamCodec<FriendlyByteBuf, Vec3i> VEC3I_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, Vec3i::getX,
                    ByteBufCodecs.INT, Vec3i::getY,
                    ByteBufCodecs.INT, Vec3i::getZ,
                    Vec3i::new
            );
    // Dentro de SoulAbilityData.java

    public static final StreamCodec<FriendlyByteBuf, SoulAbilityData> STREAM_CODEC =
            StreamCodec.composite(
                    // CAMPO 1: Index (int)
                    ByteBufCodecs.INT, SoulAbilityData::index,

                    // CAMPO 2: Name (String, Opcional)
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), data -> Optional.ofNullable(data.name()),

                    // CAMPO 2: Ability ID (ResourceLocation, Opcional)
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), data -> Optional.ofNullable(data.abilityId()),

                    // CAMPO 3: Offset (Vec3i, Opcional)
                    ByteBufCodecs.optional(VEC3I_STREAM_CODEC), data -> Optional.ofNullable(data.offset()),

                    // **********************************************
                    // EL RESTO DE LA APLICACIÓN (Función constructora)
                    // **********************************************
                    (index,nameOpt, abilityIdOpt, offsetOpt) -> new SoulAbilityData(
                            index,
                            // Convierte Optional<ResourceLocation> a @Nullable ResourceLocation
                            nameOpt.orElse(null),
                            abilityIdOpt.orElse(null),
                            // Convierte Optional<Vec3i> a @Nullable Vec3i
                            offsetOpt.orElse(null)
                    )
            );

    // ... (rest of the class)
    /**
     * Helper para inicializar un SoulData con 8 slots vacíos
     */
    public static SoulData createEmpty() {
        // Crear 8 slots vacíos
        List<SoulAbilityData> emptySlots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            emptySlots.add(new SoulAbilityData(i, null, null, null));
        }
        return new SoulData(emptySlots);
    }
}