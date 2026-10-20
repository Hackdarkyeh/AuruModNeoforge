// AurelitePickaxe.java - versión para pico de AureliteSword
package aurum.aurum.item.pickage;

import aurum.aurum.energy.ArmorAndWeapons.EnergyConfig;
import aurum.aurum.energy.ArmorAndWeapons.IEnergyWeapon;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.Swords.AureliteSword.EnergyData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class AurelitePickaxe extends PickaxeItem implements IEnergyWeapon {

    /**
     * Valores propios del pico. La capacidad, el nivel mínimo para energía oscura y los
     * multiplicadores de drenaje se toman de EnergyConfig.AureliteSword (son compartidos).
     */
    public static final class Config {
        /** Radio del área en la cara que miras: 1 = 3x3, 2 = 5x5... */
        public static final int BREAK_RADIUS = 1;
        public static final int DARK_ENERGY_CONSUMPTION_PER_BLOCK = 2;
        public static final int CLEAN_ENERGY_CONSUMPTION_PER_BLOCK = 1;
        public static final float DARK_ENERGY_SPEED_MULTIPLIER = 1.5F;
        public static final float CLEAN_ENERGY_SPEED_MULTIPLIER = 1.25F;

        private Config() {
        }
    }

    // Jugadores que están en mitad de un minado múltiple (evita que cada bloque extra
    // vuelva a disparar el minado en área y se cree una cadena infinita)
    private static final Set<UUID> MULTI_BREAKING = new HashSet<>();

    public AurelitePickaxe(Tier pTier, Properties pProperties) {
        super(pTier, pProperties
                .component(ModDataComponents.SWORD_ENERGY_DATA.get(),
                        new EnergyData(0, EnergyConfig.AureliteSword.MAX_ENERGY_CAPACITY, EnergyType.NONE))
                // Sin durabilidad: nunca se daña ni se rompe, y no sale la línea "Durabilidad" del tooltip.
                // La barra visual es la de energía (isBarVisible/getBarWidth más abajo).
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    // Método para obtener los datos de energía
    public static EnergyData getEnergyData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SWORD_ENERGY_DATA.get(),
                new EnergyData(0, EnergyConfig.AureliteSword.MAX_ENERGY_CAPACITY, EnergyType.NONE));
    }

    // Método para establecer los datos de energía
    public static void setEnergyData(ItemStack stack, EnergyData data) {
        stack.set(ModDataComponents.SWORD_ENERGY_DATA.get(), data);
    }

    // --- Implementación de IEnergyWeapon ---
    @Override
    public EnergyType getCurrentEnergyType(ItemStack stack) {
        return getEnergyData(stack).energyType();
    }

    @Override
    public void setEnergyType(ItemStack stack, EnergyType type) {
        EnergyData currentData = getEnergyData(stack);
        setEnergyData(stack, new EnergyData(
                currentData.currentEnergy(),
                currentData.maxCapacity(),
                type
        ));
    }

    @Override
    public int getCurrentEnergy(ItemStack stack) {
        return getEnergyData(stack).currentEnergy();
    }

    @Override
    public int getMaxEnergy(ItemStack stack) {
        return getEnergyData(stack).maxCapacity();
    }

    @Override
    public void setEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        int clampedAmount = Math.min(Math.max(0, amount), currentData.maxCapacity());
        setEnergyData(stack, new EnergyData(
                clampedAmount,
                currentData.maxCapacity(),
                currentData.energyType()
        ));
    }

    @Override
    public int addEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        int energyAdded = Math.min(amount, currentData.maxCapacity() - currentData.currentEnergy());
        int newEnergy = currentData.currentEnergy() + energyAdded;
        setEnergyData(stack, new EnergyData(
                newEnergy,
                currentData.maxCapacity(),
                currentData.energyType()
        ));
        return energyAdded;
    }

    @Override
    public boolean canReceiveEnergy(ItemStack stack) {
        return getCurrentEnergy(stack) < getMaxEnergy(stack);
    }

    // canUseDarkEnergy() se hereda de IEnergyWeapon (ya devuelve true)

    // --- Métodos para cargar energía específica ---
    public boolean addDarkEnergy(ItemStack stack, int amount) {
        return addDarkEnergy(stack, amount, null);
    }

    public boolean addDarkEnergy(ItemStack stack, int amount, @Nullable Player player) {
        if (player != null && (!canUseDarkEnergy(stack) || player.experienceLevel < EnergyConfig.AureliteSword.MIN_PLAYER_LEVEL_DARK_ENERGY)) {
            return false;
        }

        EnergyData currentData = getEnergyData(stack);
        if (currentData.energyType() != EnergyType.NONE && currentData.energyType() != EnergyType.DARK_ENERGY) {
            return false;
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentData.energyType() == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.DARK_ENERGY);
        }
        return energyAdded > 0;
    }

    public boolean addCleanEnergy(ItemStack stack, int amount) {
        EnergyData currentData = getEnergyData(stack);
        if (currentData.energyType() != EnergyType.NONE && currentData.energyType() != EnergyType.CLEAN_ENERGY) {
            return false;
        }

        int energyAdded = addEnergy(stack, amount);
        if (energyAdded > 0 && currentData.energyType() == EnergyType.NONE) {
            setEnergyType(stack, EnergyType.CLEAN_ENERGY);
        }
        return energyAdded > 0;
    }

    // --- Consumo de energía al picar (equivale a consumeEnergyForAttack de la espada) ---
    // Puede picar sin energía; al agotarse, la energía llega a 0 y el pico vuelve a tipo NONE
    public void consumeEnergyForMining(ItemStack stack) {
        EnergyData data = getEnergyData(stack);
        if (data.energyType() == EnergyType.NONE || data.currentEnergy() <= 0) {
            return;
        }

        int consumption = switch (data.energyType()) {
            case DARK_ENERGY -> (int) (Config.DARK_ENERGY_CONSUMPTION_PER_BLOCK *
                    EnergyConfig.AureliteSword.DARK_ENERGY_DRAIN_MULTIPLIER);
            case CLEAN_ENERGY -> (int) (Config.CLEAN_ENERGY_CONSUMPTION_PER_BLOCK *
                    EnergyConfig.AureliteSword.CLEAN_ENERGY_DRAIN_MULTIPLIER);
            default -> 0;
        };
        consumption = Math.max(1, consumption); // nunca 0, aunque el redondeo del multiplicador dé 0

        int remaining = data.currentEnergy() - consumption;
        if (remaining > 0) {
            setEnergy(stack, remaining);
        } else {
            // Si queda menos que el consumo, se gasta TODO lo que queda (antes se quedaba atascada
            // en 1-2 de energía para siempre) y el pico vuelve a tipo NONE para poder cargar otra energía
            setEnergy(stack, 0);
            setEnergyType(stack, EnergyType.NONE);
        }
    }

    // =====================================================================
    // Minado en área: 3x3 en la MISMA CARA que miras
    // =====================================================================

    /**
     * Bloques que se pican al romper `origin` mirando su cara `face` (incluye `origin`):
     * un cuadrado de (2*BREAK_RADIUS+1)^2 centrado en el bloque que apuntas, en el plano de la cara.
     * La usan tanto el servidor (para picar) como el cliente (para dibujar el resaltado).
     */
    public static List<BlockPos> getBreakArea(BlockPos origin, Direction face) {
        Direction.Axis[] planeAxes = switch (face.getAxis()) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
        Direction u = Direction.fromAxisAndDirection(planeAxes[0], Direction.AxisDirection.POSITIVE);
        Direction v = Direction.fromAxisAndDirection(planeAxes[1], Direction.AxisDirection.POSITIVE);

        int r = Config.BREAK_RADIUS;
        List<BlockPos> area = new ArrayList<>();
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                area.add(origin.relative(u, a).relative(v, b));
            }
        }
        return area;
    }

    /** El área 3x3 (y el contorno que la marca) solo funciona mientras el pico tiene energía cargada. */
    public static boolean hasAreaPower(ItemStack stack) {
        EnergyData data = getEnergyData(stack);
        return data.currentEnergy() > 0 && data.energyType() != EnergyType.NONE;
    }

    /** ¿Este bloque se puede picar a la vez que el principal? (mismo filtro en cliente y servidor) */
    public static boolean canBreakExtra(ItemStack stack, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getDestroySpeed(level, pos) >= 0 // irrompibles (bedrock...)
                && stack.isCorrectToolForDrops(state);
    }

    // Cara del bloque que está apuntando el jugador (el bloque ya se ha roto, usamos su forma antigua)
    private static Direction getMiningFace(Player player, BlockState state, Level level, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0D));
        BlockHitResult hit = state.getShape(level, pos).clip(eye, end, pos);
        return hit != null ? hit.getDirection() : Direction.orderedByNearest(player)[0].getOpposite();
    }

    // Se llama una vez por cada bloque roto (incluidos los extra): aquí se gasta la energía
    // y, si es el bloque que picas tú, se rompen los de alrededor en la misma cara (3x3).
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity); // no gasta durabilidad (UNBREAKABLE)

        if (level.isClientSide || stack.isEmpty() || state.getDestroySpeed(level, pos) == 0.0F) {
            return result;
        }

        boolean powered = hasAreaPower(stack); // se decide antes de gastar, para que el último golpe también cuente
        consumeEnergyForMining(stack);

        if (powered
                && miningEntity instanceof ServerPlayer serverPlayer
                && !serverPlayer.isCrouching() // agachado = pica solo 1 bloque
                && !MULTI_BREAKING.contains(serverPlayer.getUUID())
                && stack.isCorrectToolForDrops(state)) {
            Direction face = getMiningFace(serverPlayer, state, level, pos);
            breakExtraBlocks(stack, serverPlayer, level, pos, face);
        }
        return result;
    }

    private void breakExtraBlocks(ItemStack stack, ServerPlayer player, Level level, BlockPos origin, Direction face) {
        MULTI_BREAKING.add(player.getUUID());
        try {
            for (BlockPos target : getBreakArea(origin, face)) {
                if (stack.isEmpty() || !hasAreaPower(stack)) {
                    break; // se ha acabado la energía a mitad del área
                }
                if (target.equals(origin) || !canBreakExtra(stack, level, target)) {
                    continue;
                }
                // Respeta protecciones/eventos de rotura; llama a mineBlock (durabilidad y energía)
                player.gameMode.destroyBlock(target);
            }
        } finally {
            MULTI_BREAKING.remove(player.getUUID());
        }
    }

    // Velocidad de minado según la energía cargada (equivalente al daño por energía de la espada)
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);
        EnergyData data = getEnergyData(stack);
        if (data.currentEnergy() <= 0) {
            return baseSpeed;
        }
        return baseSpeed * switch (data.energyType()) {
            case DARK_ENERGY -> Config.DARK_ENERGY_SPEED_MULTIPLIER;
            case CLEAN_ENERGY -> Config.CLEAN_ENERGY_SPEED_MULTIPLIER;
            default -> 1.0F;
        };
    }

    // --- Métodos sobreescritos para UI ---
    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return true; // la barra de energía hace de barra de durabilidad: sin energía se ve vacía, no desaparece
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        int max = getMaxEnergy(pStack);
        return max <= 0 ? 0 : Math.round(13.0F * getCurrentEnergy(pStack) / (float) max);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return switch (getCurrentEnergyType(pStack)) {
            case DARK_ENERGY -> 0x6600CC;
            case CLEAN_ENERGY -> 0x33FF99;
            default -> 0x9933FF;
        };
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);

        EnergyData data = getEnergyData(pStack);
        String energyTypeName = switch (data.energyType()) {
            case DARK_ENERGY -> " Oscura";
            case CLEAN_ENERGY -> " Limpia";
            default -> "";
        };

        pTooltipComponents.add(Component.literal("Energía" + energyTypeName + ": " +
                data.currentEnergy() + " / " + data.maxCapacity()));

        // Mostrar multiplicador de velocidad actual
        float currentSpeed = switch (data.energyType()) {
            case DARK_ENERGY -> Config.DARK_ENERGY_SPEED_MULTIPLIER;
            case CLEAN_ENERGY -> Config.CLEAN_ENERGY_SPEED_MULTIPLIER;
            default -> 1.0F;
        };

        pTooltipComponents.add(Component.literal("Velocidad de minado: x" + currentSpeed)
                .withStyle(ChatFormatting.GRAY));
        int size = Config.BREAK_RADIUS * 2 + 1;
        if (hasAreaPower(pStack)) {
            pTooltipComponents.add(Component.literal("Pica en área " + size + "x" + size
                            + " (agáchate para picar 1)")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            pTooltipComponents.add(Component.literal("Sin energía: pica solo 1 bloque")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}