package aurum.aurum.client.gui.Armor;

import aurum.aurum.item.ArmorItem.ArmorExpansions;
import aurum.aurum.item.ArmorItem.SoulExpansions;
import aurum.aurum.tagsProvider.ModTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSoulArmorMenu extends AbstractContainerMenu {
    public static final int SOUL_CORE_SLOT = 0;
    public static final int SLOT_COUNT = 1; // 9 habilidades + 1 núcleo de alma
    private static final int DATA_COUNT = 2; // 0: Peso usado, 1: Alma máxima

    private final Container container;
    private final ContainerData data;
    protected final Player player;

    protected AbstractSoulArmorMenu(MenuType<?> menuType, int containerId, Inventory playerInventory) {
        this(menuType, containerId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    protected AbstractSoulArmorMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(menuType, containerId);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.player = playerInventory.player;
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        // 🔹 Slot del núcleo de alma (determina el peso máximo)
        this.addSlot(new Slot(container, SOUL_CORE_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isSoulCore(stack); // Solo permite núcleos de alma
            }
        });

        this.addDataSlots(data);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    // 🔹 Método para verificar si un item es una habilidad válida
    public boolean isAbility(ItemStack stack) {
        return stack.is(ModTags.Items.ABILITY_ITEMS); // Solo permite objetos que sean habilidades
    }

    public boolean isSoulCore(ItemStack stack) {
        return stack.is(ModTags.Items.SOUL_ITEMS);
    }

    // 🔹 Método para calcular el peso actual de habilidades equipadas
    private void updateWeight() {
        int totalWeight = 0;
        for (int i = 1; i < SLOT_COUNT; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                totalWeight += getAbilityWeight(stack);
            }
        }
        data.set(0, totalWeight);
    }

    // 🔹 Método para obtener el peso de una habilidad (personalizable)
    private int getAbilityWeight(ItemStack stack) {
        if (isAbility(stack)) {
            ArmorExpansions ability = (ArmorExpansions) stack.getItem();
            return ability.getWeight(); // Devuelve el peso de la habilidad
        }
        return 0;
    }

    // 🔹 Devuelve el peso actual
    public int getCurrentWeight() {
        return data.get(0);
    }

    // 🔹 Devuelve la capacidad máxima de alma
    public int getMaxSoulCapacity() {
        ItemStack soulCore = container.getItem(SOUL_CORE_SLOT);
        if (!soulCore.isEmpty() && isSoulCore(soulCore)) {
            return ((SoulExpansions) soulCore.getItem()).getMaxWight();
        }
        return 0;
    }

    // 🔹 Devuelve el progreso del peso en porcentaje
    public float getWeightProgress() {
        return Mth.clamp((float) getCurrentWeight() / (float) getMaxSoulCapacity(), 0.0F, 1.0F);
    }

    // 🔹 Métodos para posicionar los slots de habilidades en forma de estrella
    private int getSlotX(int index) {
        int[] positionsX = {50, 70, 100, 130, 150, 130, 100, 70, 50};
        return positionsX[index];
    }

    private int getSlotY(int index) {
        int[] positionsY = {30, 10, 5, 10, 30, 50, 60, 50, 30};
        return positionsY[index];
    }
}

