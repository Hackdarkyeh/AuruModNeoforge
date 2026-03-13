package aurum.aurum.block.ArmorSystem.SoulModificationTable;

import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntity;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.item.ArmorItem.ArmorExpansions;
import aurum.aurum.item.SoulExpansions;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractSoulModificationTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    private static final int SLOTS_COUNT = 9;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_COUNT, ItemStack.EMPTY);
    private static int SOULSLOT = 0;

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) { return 0; }
        @Override
        public void set(int p_58433_, int p_58434_) {}
        @Override
        public int getCount() { return 0; }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected AbstractSoulModificationTableBlockEntity(
            BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState
    ) {
        super(pType, pPos, pBlockState);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
    }

    private static int getFirstEmptySlot(List<SoulAbilityData> soulDataList) {
        if (soulDataList.size() < 8) {
            return soulDataList.size();
        }
        for (int i = 0; i < 8; i++) {
            SoulAbilityData data = soulDataList.get(i);
            if (data != null && data.abilityId() == null) {
                return i;
            }
        }
        return -1;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractSoulModificationTableBlockEntity pBlockEntity) {
        if (!pLevel.isClientSide) {
            // Obtener alma: si la BlockEntity concreta tiene un inventory propio, usarlo
            ItemStack soulStack = pBlockEntity.items.get(SOULSLOT);

            Map<Vec3i, PedestalBlockEntity> nearbyPedestals = pBlockEntity.getNearbyPedestalsByOffset(pLevel, pPos);
            boolean soulWasModified = false;

            if (soulStack.isEmpty() || !(soulStack.getItem() instanceof SoulExpansions soulItem)) {
                //pBlockEntity.clearNearbyPedestals(pLevel, pPos);
                return;
            }

            List<SoulAbilityData> soulDataList = new ArrayList<>(soulItem.getSoulData(soulStack));
            for (Map.Entry<Vec3i, PedestalBlockEntity> entry : nearbyPedestals.entrySet()) {
                Vec3i pedestalOffset = entry.getKey();
                PedestalBlockEntity pedestalEntity = entry.getValue();
                ItemStack itemInPedestal = pedestalEntity.inventory.getStackInSlot(0);

                SoulAbilityData existingData = soulDataList.stream()
                        .filter(data -> data.offset() != null && data.offset().equals(pedestalOffset))
                        .findFirst().orElse(null);

                if (!itemInPedestal.isEmpty() && itemInPedestal.getItem() instanceof ArmorExpansions newAbility) {
                    ResourceLocation newAbilityId = newAbility.getId();

                    if (existingData != null) {
                        if (existingData.abilityId() == null || !existingData.abilityId().equals(newAbilityId)) {
                            if(soulItem.setAbility(soulStack, existingData.index(), newAbility, pedestalOffset)){
                                soulWasModified = true;
                            }else{
                                // Fallo al asignar, dropear y limpiar
                                BlockPos pedPos = pedestalEntity.getBlockPos();
                                Containers.dropItemStack(pLevel, pedPos.getX(), pedPos.getY() + 1, pedPos.getZ(), itemInPedestal.copy());
                                pedestalEntity.inventory.setStackInSlot(0, ItemStack.EMPTY);
                                pedestalEntity.setChanged();
                            }
                        }
                    } else {
                        if (soulItem.isAbilityUnique(soulStack, newAbilityId)) {
                            int emptySlotIndex = getFirstEmptySlot(soulDataList);
                            if (emptySlotIndex != -1) {
                                if(soulItem.setAbility(soulStack, emptySlotIndex, newAbility, pedestalOffset)){
                                    soulWasModified = true;
                                    // mantener la lista sincronizada para próximas iteraciones
                                    soulDataList = new ArrayList<>(soulItem.getSoulData(soulStack));
                                }else{
                                    // Fallo al asignar, dropear y limpiar
                                    BlockPos pedPos = pedestalEntity.getBlockPos();
                                    Containers.dropItemStack(pLevel, pedPos.getX(), pedPos.getY() + 1, pedPos.getZ(), itemInPedestal.copy());
                                    pedestalEntity.inventory.setStackInSlot(0, ItemStack.EMPTY);
                                    pedestalEntity.setChanged();
                                }
                            }
                        }else {
                            // 🛑 Caso 2: Asignación Fallida (Duplicidad O Slots Llenos)

                            // 1. Dropear el ítem de vuelta al mundo.
                            //    (Reemplazamos dropExpansion con la llamada nativa)
                            BlockPos pedPos = pedestalEntity.getBlockPos();
                            // Usar la posición del pedestal +1 en Y para que caiga sobre el bloque
                            Containers.dropItemStack(pLevel, pedPos.getX(), pedPos.getY() + 1, pedPos.getZ(), itemInPedestal.copy());

                            // 2. Vaciar el slot del pedestal para que no interfiera en futuros ticks.
                            pedestalEntity.inventory.setStackInSlot(0, ItemStack.EMPTY);
                            pedestalEntity.setChanged();
                        }
                    }
                } else if (itemInPedestal.isEmpty() && existingData != null && existingData.abilityId() != null) {
                    if(soulItem.removeAbility(soulStack, existingData.index())){
                        soulWasModified = true;
                        soulDataList = new ArrayList<>(soulItem.getSoulData(soulStack));
                    }
                }
            }

            // Sincronizar ALMA -> PEDESTAL (asegura que pedestales muestren lo guardado)
            for (SoulAbilityData data : soulDataList) {
                if (data.abilityId() != null && data.offset() != null) {
                    BlockPos pedestalPos = pPos.offset(data.offset());
                    if (!pLevel.isLoaded(pedestalPos)) continue;
                    BlockEntity entity = pLevel.getBlockEntity(pedestalPos);
                    if (entity instanceof PedestalBlockEntity pedestalEntity) {
                        ArmorExpansions abilityItem = soulItem.getAbilityFromId(data.abilityId());
                        if (abilityItem != null) {
                            ItemStack expectedStack = new ItemStack(abilityItem.asItem());
                            ItemStack actualStack = pedestalEntity.inventory.getStackInSlot(0);
                            if (actualStack.isEmpty() || actualStack.getItem() != expectedStack.getItem()) {
                                pedestalEntity.inventory.setStackInSlot(0, expectedStack);
                                pedestalEntity.setChanged();
                            }
                        }
                    }
                }
            }

            if (soulWasModified) {
                pBlockEntity.setChanged();
                soulItem.updateStackNBT(soulStack, new ArrayList<>(soulItem.getSoulData(soulStack)));
            }

            if (!soulWasModified && !soulStack.isEmpty() && soulStack.getItem() instanceof SoulExpansions soulItem1) {
                // Buscar si hay una expansión colocada sin asignar
                for (Map.Entry<Vec3i, PedestalBlockEntity> entry : nearbyPedestals.entrySet()) {
                    Vec3i pedestalOffset = entry.getKey();
                    PedestalBlockEntity pedestalEntity = entry.getValue();
                    ItemStack itemInPedestal = pedestalEntity.inventory.getStackInSlot(0);

                    // Si hay una expansión en el pedestal
                    if (!itemInPedestal.isEmpty() && itemInPedestal.getItem() instanceof ArmorExpansions) {

                        // Y no hay datos existentes para este offset (aún no se asignó)
                        boolean isAssigned = soulItem1.getSoulData(soulStack).stream()
                                .anyMatch(data -> data.offset() != null && data.offset().equals(pedestalOffset));

                        if (!isAssigned) {
                            // Forzar el guardado y un nuevo tick para que la lógica de asignación corra
                            // Esto resolverá la falla de detección del primer tick.
                            pBlockEntity.setChanged();
                            break; // Salir del bucle, ya hemos marcado para actualizar
                        }
                    }
                }
            }

// Lógica de limpieza al final
            pBlockEntity.clearNearbyPedestals(pLevel, pPos);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return pDirection == Direction.DOWN && pIndex == 1 ? pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex); // Alma que se retira (puede ser vacía)
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (pIndex == 0 && !flag) {
            this.setChanged();
            if (this.level != null && !this.level.isClientSide) {

                if (pStack.isEmpty()) {
                    // Alma extraída: limpiar todos los pedestales
                    this.clearNearbyPedestals(this.level, this.worldPosition);

                } else if (pStack.getItem() instanceof SoulExpansions) {
                    // Alma insertada:
                    // 1. Dropear expansiones conflictivas (duplicadas)
                    // 2. Sincronizar los pedestales con el alma
                    this.dropConflictingExpansions(this.level, this.worldPosition, pStack);
                    this.syncPedestalsFromSoul(this.level, this.worldPosition, pStack);
                }
            }
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {
        if (pRecipe != null) {
            ResourceLocation resourcelocation = pRecipe.id();
            this.recipesUsed.addTo(resourcelocation, 1);
        }
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel pLevel, Vec3 pPopVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();
        for (Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            pLevel.getRecipeManager().byKey(entry.getKey()).ifPresent(p_300839_ -> {
                list.add((RecipeHolder<?>)p_300839_);
                createExperience(pLevel, pPopVec, entry.getIntValue(), ((AbstractCookingRecipe)p_300839_.value()).getExperience());
            });
        }
        return list;
    }

    private static void createExperience(ServerLevel pLevel, Vec3 pPopVec, int pRecipeIndex, float pExperience) {
        int i = Mth.floor((float)pRecipeIndex * pExperience);
        float f = Mth.frac((float)pRecipeIndex * pExperience);
        if (f != 0.0F && Math.random() < (double)f) {
            i++;
        }
        ExperienceOrb.award(pLevel, pPopVec, i);
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }

    // Buscar pedestales alrededor y devolver mapa offset->blockentity
    public Map<Vec3i, PedestalBlockEntity> getNearbyPedestalsByOffset(Level pLevel, BlockPos pPos) {
        Map<Vec3i, PedestalBlockEntity> nearbyPedestals = new HashMap<>();
        final int[][] OFFSETS = new int[][]{
                {0, 1},   // N
                {0, -1},  // S
                {1, 0},   // E
                {-1, 0},  // O
                {1, 1},   // NE
                {-1, 1},  // NO
                {1, -1},  // SE
                {-1, -1}  // SO
        };
        final int MAX_DISTANCE = 5;

        for (int[] offset : OFFSETS) {
            int dx = offset[0] * MAX_DISTANCE;
            int dz = offset[1] * MAX_DISTANCE;
            BlockPos targetPos = pPos.offset(dx, 0, dz);
            if (!pLevel.isLoaded(targetPos)) continue;
            BlockEntity blockEntity = pLevel.getBlockEntity(targetPos);
            if (blockEntity instanceof PedestalBlockEntity pedestalEntity) {
                Vec3i relativeOffset = new Vec3i(dx, 0, dz);
                nearbyPedestals.put(relativeOffset, pedestalEntity);
                // NO break: queremos recolectar todos los pedestales posibles
            }
        }
        return nearbyPedestals;
    }


    public void clearNearbyPedestals(Level pLevel, BlockPos pPos) {
        Map<Vec3i, PedestalBlockEntity> nearbyPedestals = this.getNearbyPedestalsByOffset(pLevel, pPos);

        // Obtener el alma actual (si la BE concreta tiene su propio inventory úsalo)
        ItemStack soulStack;
        if (this instanceof SoulModificationTableBlockEntity concrete) {
            soulStack = concrete.inventory.getStackInSlot(0);
        } else {
            soulStack = this.items.get(SOULSLOT);
        }

        // Si no hay alma válida, la tabla no debe hacer limpieza en los pedestales.
        if (soulStack == null || soulStack.isEmpty() || !(soulStack.getItem() instanceof SoulExpansions soul)) {
            return;
        }

        // --- LÓGICA DE LIMPIEZA SÓLO DE EXPANSIONES NO ASIGNADAS (ALMA PRESENTE) ---

        // Construir conjunto de offsets ocupados por la alma (habilidad != null)
        Set<Vec3i> soulOccupiedOffsets = new HashSet<>();
        List<SoulAbilityData> soulData = soul.getSoulData(soulStack);

        // Validación: asegurarse de que soulData tiene datos válidos
        if (soulData == null || soulData.isEmpty()) {
            return;
        }

        for (SoulAbilityData d : soulData) {
            // Validar que el offset y abilityId NO sean null antes de añadir
            if (d != null && d.offset() != null && d.abilityId() != null) {
                soulOccupiedOffsets.add(d.offset());
            }
        }

        // Dropear solo los ítems de expansión que no están asignados en el alma.
        for (Map.Entry<Vec3i, PedestalBlockEntity> entry : nearbyPedestals.entrySet()) {
            Vec3i offset = entry.getKey();
            PedestalBlockEntity pedestalEntity = entry.getValue();
            ItemStack itemInPedestal = pedestalEntity.inventory.getStackInSlot(0);

            if (!itemInPedestal.isEmpty() && itemInPedestal.getItem() instanceof ArmorExpansions) {
                // Si la alma tiene una habilidad asignada para este offset, NO dropear
                if (soulOccupiedOffsets.contains(offset)) {
                    continue;
                }
                // Si no está asignado, dropear y limpiar el slot
                BlockPos pedPos = pedestalEntity.getBlockPos();
                Containers.dropItemStack(pLevel, pedPos.getX(), pedPos.getY(), pedPos.getZ(), itemInPedestal.copy());
                pedestalEntity.inventory.setStackInSlot(0, ItemStack.EMPTY);
                pedestalEntity.setChanged();
            }
        }
    }

    // Sincroniza pedestales de acuerdo a los datos del alma
    protected void syncPedestalsFromSoul(Level level, BlockPos pPos, ItemStack soulStack) {
        if (!(soulStack.getItem() instanceof SoulExpansions soul)) return;
        List<SoulAbilityData> data = soul.getSoulData(soulStack);

        // Validación: asegurarse de que data es válida
        if (data == null || data.isEmpty()) {
            return;
        }

        // **PASO 1: SINCRONIZACIÓN (SOLO DE LO QUE ESTÁ EN EL ALMA CON HABILIDAD ASIGNADA)**
        for (SoulAbilityData d : data) {
            // Solo sincronizar si la habilidad realmente está asignada (no es null)
            if (d.abilityId() != null && d.offset() != null) {
                BlockPos pedestalPos = pPos.offset(d.offset());
                if (!level.isLoaded(pedestalPos)) continue;
                BlockEntity entity = level.getBlockEntity(pedestalPos);

                if (entity instanceof PedestalBlockEntity pedestalEntity) {
                    ArmorExpansions abilityItem = soul.getAbilityFromId(d.abilityId());
                    if (abilityItem != null) {
                        ItemStack expected = new ItemStack(abilityItem.asItem());
                        ItemStack actual = pedestalEntity.inventory.getStackInSlot(0);

                        // Comprobación solo por el TIPO de ÍTEM, no por NBT/Componentes.
                        boolean isCorrectItemType = !actual.isEmpty() && actual.getItem() == expected.getItem();

                        // Si el pedestal está vacío O no tiene el ítem de Expansión correcto (solo por tipo)
                        if (actual.isEmpty() || !isCorrectItemType) {

                            // Si hay un ítem incorrecto, lo dropeamos
                            if (!actual.isEmpty() && !isCorrectItemType) {
                                BlockPos pedPos = pedestalEntity.getBlockPos();
                                Containers.dropItemStack(level, pedPos.getX(), pedPos.getY(), pedPos.getZ(), actual.copy());
                            }

                            // Colocar la expansión del alma
                            pedestalEntity.inventory.setStackInSlot(0, expected);
                            pedestalEntity.setChanged();
                        }
                    }
                }
            }
        }

        // **PASO 2: LIMPIEZA DE EXPANSIONES NO ASIGNADAS EN PEDESTALES**
        this.clearNearbyPedestals(level, pPos);
    }

    /**
     * Dropea expansiones conflictivas cuando se inserta un alma.
     * Detecta duplicadas (misma expansión en el alma y en pedestal) y las expulsa.
     */
    protected void dropConflictingExpansions(Level level, BlockPos pPos, ItemStack soulStack) {
        if (!(soulStack.getItem() instanceof SoulExpansions soul)) return;

        List<SoulAbilityData> soulData = soul.getSoulData(soulStack);
        if (soulData == null || soulData.isEmpty()) {
            return;
        }

        // Construir un set de IDs de expansiones que ya existen en el alma
        Set<ResourceLocation> soulAbilityIds = new HashSet<>();
        for (SoulAbilityData d : soulData) {
            if (d.abilityId() != null) {
                soulAbilityIds.add(d.abilityId());
            }
        }

        // Revisar todos los pedestales cercanos
        Map<Vec3i, PedestalBlockEntity> nearbyPedestals = this.getNearbyPedestalsByOffset(level, pPos);
        for (Map.Entry<Vec3i, PedestalBlockEntity> entry : nearbyPedestals.entrySet()) {
            PedestalBlockEntity pedestalEntity = entry.getValue();
            ItemStack itemInPedestal = pedestalEntity.inventory.getStackInSlot(0);

            if (!itemInPedestal.isEmpty() && itemInPedestal.getItem() instanceof ArmorExpansions expansion) {
                ResourceLocation expansionId = expansion.getId();

                // Si esta expansión ya existe en el alma, dropearlo (conflicto)
                if (soulAbilityIds.contains(expansionId)) {
                    BlockPos pedPos = pedestalEntity.getBlockPos();
                    Containers.dropItemStack(level, pedPos.getX(), pedPos.getY() + 1, pedPos.getZ(), itemInPedestal.copy());
                    pedestalEntity.inventory.setStackInSlot(0, ItemStack.EMPTY);
                    pedestalEntity.setChanged();
                }
            }
        }
    }



}
