package aurum.aurum.init;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.entity.AurumVillager.AurumContract;
import aurum.aurum.entity.AurumVillager.AurumLibrarianData;
import aurum.aurum.entity.AurumVillager.AurumVillagerLevel;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static aurum.aurum.Aurum.MODID;


public class ModAttachments {
    // 1. Creamos el REGISTRADOR (la lista de cosas que vamos a añadir a los jugadores)
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // 2. Registramos el "espacio" para el alma
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulData.PlayerSoulContainer>> ACTIVE_SOUL =
            ATTACHMENT_TYPES.register("active_soul", () -> AttachmentType.builder(SoulData.PlayerSoulContainer::empty)
                    .serialize(SoulData.PlayerSoulContainer.CODEC)
                    .copyOnDeath()
                    .build());


    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> AURUM_BLOOD =
            ATTACHMENT_TYPES.register("aurum_blood", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    /** Exposición acumulada a Aurum (radiación ambiental). Aparte de AURUM_BLOOD. Se reinicia al morir. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> AURUM_EXPOSURE =
            ATTACHMENT_TYPES.register("aurum_exposure", () -> AttachmentType.builder(() -> 0.0f)
                    .serialize(Codec.FLOAT)
                    .build());

    /** Contrato de alquiler de un aldeano de Aurum (va en el propio aldeano). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AurumContract>> VILLAGER_CONTRACT =
            ATTACHMENT_TYPES.register("villager_contract", () -> AttachmentType.builder(() -> AurumContract.EMPTY)
                    .serialize(AurumContract.CODEC)
                    .build());

    /** Nivel y experiencia de un aldeano de Aurum (todos tienen). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AurumVillagerLevel>> VILLAGER_LEVEL =
            ATTACHMENT_TYPES.register("villager_level", () -> AttachmentType.builder(() -> AurumVillagerLevel.DEFAULT)
                    .serialize(AurumVillagerLevel.CODEC)
                    .build());

    /** Ofertas del bibliotecario, en el propio aldeano. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AurumLibrarianData>> LIBRARIAN_DATA =
            ATTACHMENT_TYPES.register("librarian_data", () -> AttachmentType.builder(AurumLibrarianData::fresh)
                    .serialize(AurumLibrarianData.CODEC)
                    .build());


    // 3. Método para conectar esto con NeoForge
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

}
