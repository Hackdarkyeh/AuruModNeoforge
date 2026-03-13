package aurum.aurum.item;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.item.ArmorItem.ArmorExpansions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static aurum.aurum.init.ModComponents.SOUL_COMPONENT;

public class SoulExpansions extends Item {
    private static final int MAX_SLOTS = 8;

    private final int idTier;
    private final int honor;
    private final int maxWight;
    private List<SoulAbilityData> currentData;

    public SoulExpansions(int idTier, int honor, int maxWight) {
        super(new Properties().stacksTo(1));
        this.idTier = idTier;
        this.honor = honor;
        this.maxWight = maxWight;
    }

    // --- Métodos de Propiedad ---
    public int getPeso() { return honor; }
    public int getMaxWight() { return maxWight; }
    public int getId() { return idTier; }

    // Método mejorado para obtener habilidades del alma
    public List<SoulAbilityData> getSoulData(ItemStack soulStack) {
        SoulData data = soulStack.get(SOUL_COMPONENT.get());

        if (data == null) {
            // Inicializar con datos vacíos
            SoulData emptyData = SoulData.createEmpty();
            soulStack.set(SOUL_COMPONENT.get(), emptyData);
            return new ArrayList<>(emptyData.abilities());
        }

        return new ArrayList<>(data.abilities());
    }

    // Método mejorado para actualizar NBT
    public void updateStackNBT(ItemStack soulStack, List<SoulAbilityData> currentData) {
        // Asegurarse de que tenemos exactamente 8 slots
        List<SoulAbilityData> validatedData = validateAndFixData(currentData);
        SoulData newData = new SoulData(validatedData);
        soulStack.set(SOUL_COMPONENT.get(), newData);
    }

    // Validar y corregir datos para tener siempre 8 slots
    private List<SoulAbilityData> validateAndFixData(List<SoulAbilityData> data) {
        List<SoulAbilityData> fixedData = new ArrayList<>();

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < data.size()) {
                SoulAbilityData existing = data.get(i);
                // Asegurar que el índice sea correcto
                fixedData.add(new SoulAbilityData(i, existing.name(), existing.abilityId(), existing.offset()));
            } else {
                // Añadir slot vacío
                fixedData.add(new SoulAbilityData(i,  null,null, null));
            }
        }
        return fixedData;
    }

    public ArmorExpansions getAbilityFromId(ResourceLocation id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        return item.isPresent() && item.get() instanceof ArmorExpansions ability ? ability : null;
    }

    public boolean isAbilityUnique(ItemStack soulStack, ResourceLocation id) {
        List<SoulAbilityData> data = getSoulData(soulStack);
        return data.stream()
                .noneMatch(d -> Objects.equals(d.abilityId(), id));
    }

    // Método mejorado para añadir habilidad
    public boolean setAbility(ItemStack soulStack, int index, ArmorExpansions newAbility, Vec3i offset) {
        if (index < 0 || index >= MAX_SLOTS) {
            System.out.println("Invalid index: " + index);
            return false;
        }

        currentData = getSoulData(soulStack);

        // Verificar unicidad
        if (!isAbilityUnique(soulStack, newAbility.getId())) {
            System.out.println("Ability already exists: " + newAbility.getId());
            return false;
        }

        // Verificar peso
        if (!checkWeightLimit(currentData, newAbility)) {
            System.out.println("Weight limit exceeded for ability: " + newAbility.getId());
            return false;
        }

        // Actualizar datos
        SoulAbilityData newData = new SoulAbilityData(index,newAbility.getName(), newAbility.getId(), offset);
        currentData.set(index, newData);
        updateStackNBT(soulStack, currentData);

        System.out.println("Successfully added ability: " + newAbility.getId() + " at index " + index);
        return true;
    }

    // Método mejorado para remover habilidad
    public boolean removeAbility(ItemStack soulStack, int index) {
        if (index < 0 || index >= MAX_SLOTS) {
            return false;
        }

        currentData = getSoulData(soulStack);
        currentData.set(index, new SoulAbilityData(index, null, null, null));
        updateStackNBT(soulStack, currentData);

        System.out.println("Removed ability from index: " + index);
        return true;
    }

    /**
     * Añade una expansión desde un pedestal al alma.
     * Valida unicidad, peso y encuentra el primer slot disponible.
     */
    public boolean addExpansionFromPedestal(ItemStack soulStack, ArmorExpansions newAbility, Vec3i pedestalOffset) {
        // Validar que la habilidad sea única
        if (!isAbilityUnique(soulStack, newAbility.getId())) {
            System.out.println("Ability already exists in soul: " + newAbility.getId());
            return false;
        }

        // Obtener los datos actuales del alma
        List<SoulAbilityData> currentData = getSoulData(soulStack);

        // Validar peso
        if (!checkWeightLimit(currentData, newAbility)) {
            System.out.println("Weight limit exceeded for ability: " + newAbility.getId());
            return false;
        }

        // Encontrar el primer slot vacío
        int emptySlotIndex = -1;
        for (int i = 0; i < MAX_SLOTS; i++) {
            SoulAbilityData data = currentData.get(i);
            if (data.abilityId() == null) {
                emptySlotIndex = i;
                break;
            }
        }

        // Si no hay slots vacíos, fallo
        if (emptySlotIndex == -1) {
            System.out.println("No empty slots available in soul");
            return false;
        }

        // Usar setAbility para asignar la expansión en el slot vacío
        return setAbility(soulStack, emptySlotIndex, newAbility, pedestalOffset);
    }

    // Método mejorado para verificar peso
    private boolean checkWeightLimit(List<SoulAbilityData> currentData, ArmorExpansions newAbility) {
        int currentWeight = honor;

        for (SoulAbilityData data : currentData) {
            if (data.abilityId() != null) {
                ArmorExpansions existingAbility = getAbilityFromId(data.abilityId());
                if (existingAbility != null) {
                    currentWeight += existingAbility.getWeight();
                }
            }
        }

        int newTotalWeight = currentWeight + newAbility.getWeight();
        boolean withinLimit = newTotalWeight <= maxWight;

        System.out.println("Weight check - Current: " + currentWeight + ", New: " + newAbility.getWeight() +
                ", Total: " + newTotalWeight + ", Limit: " + maxWight + ", Within limit: " + withinLimit);

        return withinLimit;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {

        // --- Expansiones con manejo de null ---
        String[] tooltipKeys = {
                "expansion1", "expansion2", "expansion3", "expansion4",
                "expansion5", "expansion6", "expansion7", "expansion8"
        };

        // Asegúrate de que currentData no es null antes de intentar acceder a él
        if (currentData != null) {
            for (int i = 0; i < tooltipKeys.length; i++) {
                // Verifica que el índice 'i' sea válido en la lista 'currentData'
                if (i < currentData.size()) {
                    // Asegúrate de que la lista currentData está tipada correctamente (List<SoulAbilityData>)
                    SoulAbilityData expansionValue = currentData.get(i);
                    String expansionKey = "item.aurum.soul_expansions.tooltip." + tooltipKeys[i];
                    String key2 = "ability.aurum.";
                    MutableComponent expansionNameComponent;

                    if (expansionValue.name() != null) {
                        // 1. El valor 'name' contiene la CLAVE DE TRADUCCIÓN del efecto (e.g., "ability.aurum.resistance_to_damage")
                        String abilityTranslationKey = expansionValue.name();

                        // 2. Creamos el Component traducible para el nombre de la habilidad.
                        // Usamos un color distintivo, por ejemplo, GOLD o AQUA.
                        expansionNameComponent = Component.translatable(key2.concat(abilityTranslationKey))
                                .withStyle(ChatFormatting.GOLD);

                        // 3. Agregamos el Component de la línea de expansión, pasándole el Component del nombre
                        // Traduce: "Expansión de alma X: " + [Componente Traducido de la Habilidad]
                        pTooltipComponents.add(Component.translatable(expansionKey, expansionNameComponent));

                        // NOTA: Quita la línea duplicada que tenías: pTooltipComponents.add(Component.translatable(key, Component.literal(expansionName)));

                    } else {
                        // Si el valor ES null: "Sin asignar"

                        // Opción 1: Clave de traducción para el estado "Sin asignar" (Recomendada)
                        // Se traduce: "Expansión de alma X: Sin asignar"

                        // 1. Creamos el Componente para "Sin asignar"
                        expansionNameComponent = Component.translatable("tooltip.aurum.unassigned_slot")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

                        // 2. Agregamos el Component de la línea de expansión, pasándole el Component "Sin asignar"
                        pTooltipComponents.add(Component.translatable(expansionKey, expansionNameComponent));
                    }
                }
            }
        }
    }


}