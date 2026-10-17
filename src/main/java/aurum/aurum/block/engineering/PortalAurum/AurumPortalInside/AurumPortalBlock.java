package aurum.aurum.block.engineering.PortalAurum.AurumPortalInside;

import aurum.aurum.init.ModBlocks;
import aurum.aurum.worldgen.Dimension.AurumDimension;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AurumPortalBlock extends BaseEntityBlock {
    public static final MapCodec<AurumPortalBlock> CODEC = simpleCodec(AurumPortalBlock::new);

    // Forma visual del portal (12 píxeles de alto)
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    private static final BlockPos[] FRAME_OFFSETS = new BlockPos[] {
            new BlockPos(-1, -3, -2), new BlockPos(0, -3, -2), new BlockPos(1, -3, -2),
            new BlockPos(-2, -3, -1), new BlockPos(-1, -3, -1), new BlockPos(0, -3, -1), new BlockPos(1, -3, -1), new BlockPos(2, -3, -1),
            new BlockPos(-2, -3, 0), new BlockPos(-1, -3, 0), new BlockPos(0, -3, 0), new BlockPos(1, -3, 0), new BlockPos(2, -3, 0),
            new BlockPos(-2, -3, 1), new BlockPos(-1, -3, 1), new BlockPos(0, -3, 1), new BlockPos(1, -3, 1), new BlockPos(2, -3, 1),
            new BlockPos(-1, -3, 2), new BlockPos(0, -3, 2), new BlockPos(1, -3, 2),
            new BlockPos(-1, -2, -3), new BlockPos(0, -2, -3), new BlockPos(1, -2, -3),
            new BlockPos(-2, -2, -2), new BlockPos(2, -2, -2),
            new BlockPos(-3, -2, -1), new BlockPos(3, -2, -1),
            new BlockPos(-3, -2, 0), new BlockPos(3, -2, 0),new BlockPos(0, -2, 0),
            new BlockPos(-3, -2, 1), new BlockPos(3, -2, 1),
            new BlockPos(-2, -2, 2), new BlockPos(2, -2, 2),
            new BlockPos(-1, -2, 3), new BlockPos(0, -2, 3), new BlockPos(1, -2, 3),
            new BlockPos(0, -1, 0), new BlockPos(0, -1, 0)
    };

    public AurumPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }



    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        if (!pState.is(ModBlocks.AURUM_PORTAL_BLOCK.get())) {
            return null;
        }
        return new AurumPortalBlockEntity(pPos, pState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ¡VITAL! Quitamos la colisión física. Esto hace que el jugador caiga "dentro" del vórtice,
    // garantizando que el método entityInside se dispare siempre que lo pise.
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // 1. Filtros básicos
        if (level.isClientSide || entity.isPassenger() || entity.isVehicle()) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (entity.isOnPortalCooldown()) {
                return;
            }

            // Usamos el nombre EXACTO que nos dio el servidor ("dimension1")
            ResourceKey<Level> aurumDimKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("aurum", "dimension1")
            );

            // 2. Definir dimensión de destino
            ResourceKey<Level> targetDimensionKey = (serverLevel.dimension() == aurumDimKey)
                    ? Level.OVERWORLD
                    : aurumDimKey;

            ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDimensionKey);

            if (targetLevel == null) {
                System.out.println("[ERROR AURUM] ¡La dimensión de destino (" + targetDimensionKey.location() + ") es NULL y no se pudo encontrar!");
                // --- DEBUG: Imprimir TODAS las dimensiones cargadas ---
                System.out.println("--- DIMENSIONES DISPONIBLES EN EL SERVIDOR ---");
                for (ResourceKey<Level> key : serverLevel.getServer().levelKeys()) {
                    System.out.println("- " + key.location());
                }
                System.out.println("----------------------------------------------");
                return;
            }

            // 3. LA CORRECCIÓN: Comprobamos los permisos pasando el nivel de origen y el de destino
            if (!entity.canChangeDimensions(serverLevel, targetLevel)) {
                return;
            }

            System.out.println("[AURUM PORTAL] Teletransportando a " + entity.getName().getString() + " a la dimensión: " + targetDimensionKey.location());

            // 4. Ejecutar viaje
            entity.setPortalCooldown();

            BlockPos destinationPos = findSafePositionAndBuild(targetLevel, entity);

            DimensionTransition transition = new DimensionTransition(
                    targetLevel,
                    new Vec3(destinationPos.getX() + 0.5, destinationPos.getY(), destinationPos.getZ() + 0.5),
                    entity.getDeltaMovement(),
                    entity.getYRot(),
                    entity.getXRot(),
                    DimensionTransition.DO_NOTHING
            );

            entity.changeDimension(transition);
        }
    }

    private BlockPos findSafePositionAndBuild(ServerLevel targetLevel, Entity entity) {
        int x = (int) Math.floor(entity.getX());
        int z = (int) Math.floor(entity.getZ());
        BlockPos searchCenter = new BlockPos(x, entity.getBlockY(), z);

        // 1. Escaneamos un radio de 16 bloques buscando un portal existente
        BlockPos existingPortal = findExistingPortal(targetLevel, searchCenter, 16);
        if (existingPortal != null) {
            // ¡Si ya existe uno, te enviamos allí directamente y cancelamos la construcción!
            return existingPortal;
        }

        // 2. Si no existe ninguno cerca, calculamos la superficie para construir uno nuevo

        BlockPos surfacePos = targetLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockPos portalCenter = surfacePos.above(1);
        // Evitar que el portal se genere en el vacío
        if (surfacePos.getY() <= targetLevel.getMinBuildHeight() + 5) {
            portalCenter = new BlockPos(x, 70, z);
        }

        // 3. Construimos el portal apagado nuevo
        buildInactivePortal(targetLevel, portalCenter);

        return portalCenter;
    }

    private BlockPos findExistingPortal(ServerLevel level, BlockPos center, int radius) {
        // Escaneamos un área alrededor del jugador
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                // Escaneamos de arriba a abajo para encontrar estructuras de superficie rápido
                for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // CASO A: El portal ya fue activado por el jugador
                    if (state.is(ModBlocks.AURUM_PORTAL_BLOCK.get())) {
                        return pos;
                    }

                    // CASO B: El portal sigue apagado.
                    // Buscamos si este bloque es Obsidiana y si tiene la forma del centro de tu marco (Y = -3)
                    // (Cambia Blocks.OBSIDIAN por el bloque de tu marco si usas uno custom)
                    if (state.is(Blocks.OBSIDIAN)) {
                        if (isInactivePortalBase(level, pos)) {
                            // Devolvemos la posición 3 bloques más arriba para que el jugador
                            // aparezca en el centro del hueco, no incrustado en el suelo
                            return pos.above(3);
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isInactivePortalBase(ServerLevel level, BlockPos basePos) {
        // Comprobamos que el bloque actual y sus 4 vecinos formen la cruz de la base (-3) de tu array.
        // Esto asegura que no estemos detectando un bloque de obsidiana aleatorio puesto por el jugador.
        return level.getBlockState(basePos.offset(1, 0, 0)).is(Blocks.OBSIDIAN) &&
                level.getBlockState(basePos.offset(-1, 0, 0)).is(Blocks.OBSIDIAN) &&
                level.getBlockState(basePos.offset(0, 0, 1)).is(Blocks.OBSIDIAN) &&
                level.getBlockState(basePos.offset(0, 0, -1)).is(Blocks.OBSIDIAN) &&
                level.getBlockState(basePos.offset(0, 1, 0)).is(Blocks.AIR); // El interior del portal debe estar vacío
    }
    private void buildInactivePortal(ServerLevel level, BlockPos centerPos) {
        // 1. Excavamos un "cilindro" de aire que calca la forma redonda de tu portal.
        // La fórmula dx^2 + dz^2 <= 10 cubre exactamente el radio de 3 bloques de tus FRAME_OFFSETS.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 10) {
                    // Limpiamos desde el fondo del cuenco (Y = -2) hasta 4 bloques por encima de la superficie
                    for (int dy = -2; dy <= 4; dy++) {
                        level.setBlock(centerPos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // 2. Construimos el marco del portal encajándolo en el hueco recién hecho
        for (BlockPos offset : FRAME_OFFSETS) {
            BlockPos framePos = centerPos.offset(offset);

            // Recuerda cambiar Blocks.OBSIDIAN por ModBlocks.TU_BLOQUE.get() si es necesario
            level.setBlock(framePos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }
}