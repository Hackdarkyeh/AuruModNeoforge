package aurum.aurum.init;

import aurum.aurum.Soul.SoulData;
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





    // 3. Método para conectar esto con NeoForge
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }





}
