package aurum.aurum.Soul;

import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.init.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record SoulData(
        int idTier,
        int honor,
        int maxHonor,   // <-- NUEVO: Campo añadido
        int maxWeight,
        List<SoulAbilityData> abilities
) {
    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("tier").forGetter(SoulData::idTier),
            Codec.INT.fieldOf("honor").forGetter(SoulData::honor),
            Codec.INT.fieldOf("max_honor").forGetter(SoulData::maxHonor), // <-- NUEVO: Guardado en NBT/Json
            Codec.INT.fieldOf("max_weight").forGetter(SoulData::maxWeight),
            SoulAbilityData.CODEC.listOf().fieldOf("abilities").forGetter(SoulData::abilities)
    ).apply(inst, SoulData::new));

    // Actualizado para recibir también el maxHonor al crearse vacío
    public static SoulData createEmpty(int tier, int honor, int maxWeight, int maxHonor) {
        List<SoulAbilityData> emptySlots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            emptySlots.add(new SoulAbilityData(i, null, null, null, 0));
        }
        return new SoulData(tier, honor, maxHonor, maxWeight, emptySlots);
    }

    public int idTier() { return idTier; }
    public int honor() { return honor; }
    public int maxHonor() { return maxHonor; } // <-- NUEVO: Getter
    public int maxWeight() { return maxWeight; }
    public List<SoulAbilityData> abilities() { return abilities; }

    public record PlayerSoulContainer(ItemStack soulStack) {
        public static final Codec<PlayerSoulContainer> CODEC = ItemStack.OPTIONAL_CODEC
                .xmap(PlayerSoulContainer::new, PlayerSoulContainer::soulStack)
                .fieldOf("soul_stack")
                .codec();

        public static PlayerSoulContainer empty() {
            return new PlayerSoulContainer(ItemStack.EMPTY);
        }

        /**
         * Obtiene los datos de la alma activa dentro del contenedor.
         * Si no hay ningún alma equipada o no tiene componentes de datos válidos,
         * devuelve una instancia vacía por defecto con un tier mínimo de 1 y maxHonor de 0 por seguridad.
         */
        public SoulData activeSoul() {
            if (this.soulStack.isEmpty()) {
                // tier: 1, honor: 0, maxWeight: 0, maxHonor: 0
                return SoulData.createEmpty(1, 0, 0, 0);
            }
            return this.soulStack.getOrDefault(
                    ModDataComponents.SOUL_COMPONENT.get(),
                    SoulData.createEmpty(1, 0, 0, 0)
            );
        }
    }
}