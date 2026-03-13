package aurum.aurum.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Generador de puentes dinámicos para la fortaleza.
 * Coloca piezas en matriz 3x3 con diferentes alturas Y.
 */
public class FortressBridgeGenerator {

    // ========== CONFIGURACIÓN DE PIEZAS POR DIRECCIÓN ==========

    // ESTE (X positivo)
    private static final BridgePattern BRIDGE_EAST = new BridgePattern(
        // Columna izquierda (Z negativo)
        "aurum:puente_fortaleza_z_zona_alta",        // z_alta
        "aurum:puente_fortaleza_z_zona_medio",       // z_medio
        // Centro
        "aurum:puente_fortaleza_zona_alta",          // alta
        "aurum:puente_fortaleza_medio",              // medio
        "aurum:puente_fortaleza_inferior",           // inferior
        // Columna derecha (Z positivo)
        "aurum:puente_fortaleza_zz_zona_alta",      // zz_medio
        "aurum:puente_fortaleza_zzona_medio_e",      // zz_alta
        // Offsets verticales
        32,  // offsetAlta (piezas de arriba)
        0,   // offsetMedio (nivel base)
        -32  // offsetInferior (piezas de abajo)
    );

    // OESTE (X negativo)
    private static final BridgePattern BRIDGE_WEST = new BridgePattern(
        // Columna izquierda (Z negativo)
        "aurum:puente_fortaleza_z_zona_alta",        // z_alta
        "aurum:puente_fortaleza_z_zona_medio",       // z_medio
        // Centro
        "aurum:puente_fortaleza_zona_alta",          // alta
        "aurum:puente_fortaleza_medio",              // medio
        "aurum:puente_fortaleza_inferior",           // inferior
        // Columna derecha (Z positivo)
        "aurum:puente_fortaleza_zzona_alta",      // zz_medio
        "aurum:puente_fortaleza_zzona_medio_o",      // zz_alta
        // Offsets verticales
            32,  // offsetAlta
        0,   // offsetMedio
        -32  // offsetInferior
    );

    // NORTE (Z negativo)
    private static final BridgePattern BRIDGE_NORTH = new BridgePattern(
        // Columna izquierda (Z negativo)
        "aurum:puente_fortaleza_z_zona_alta",        // z_alta
        "aurum:puente_fortaleza_z_zona_medio_n",     // z_medio
        // Centro
        "aurum:puente_fortaleza_zona_alta",          // alta
        "aurum:puente_fortaleza_z_zona_medio",       // medio
        "aurum:puente_fortaleza_inferior",           // inferior
        // Columna derecha (Z positivo)
        "aurum:puente_fortaleza_zzona_alta",        // zz_medio
        "aurum:puente_fortaleza_zzona_medio",         // zz_alta
        // Offsets verticales
            32,  // offsetAlta
        0,   // offsetMedio
        -32  // offsetInferior
    );

    // SUR (Z positivo)
    private static final BridgePattern BRIDGE_SOUTH = new BridgePattern(
        // Columna izquierda (Z negativo)
        "aurum:puente_fortaleza_z_zona_alta",        // z_alta
        "aurum:puente_fortaleza_z_zona_medio_s",     // z_medio
        // Centro
        "aurum:puente_fortaleza_zona_alta",          // alta
        "aurum:puente_fortaleza_z_zona_medio",       // medio
        "aurum:puente_fortaleza_inferior",           // inferior
        // Columna derecha (Z positivo)
        "aurum:puente_fortaleza_zzona_medio",        // zz_medio
        "aurum:puente_fortaleza_zzona_alta",         // zz_alta
        // Offsets verticales
            32,  // offsetAlta
        0,   // offsetMedio
        -32  // offsetInferior
    );

    // Centro (conecta los 4 puentes)
    private static final String BRIDGE_CENTER = "aurum:puente_fortaleza_inferior";

    private static final int MAX_BRIDGE_LENGTH = 200;
    private static final int SECTION_LENGTH = 16;

    /**
     * Clase para encapsular el patrón de puente: estructura 3x3 con alturas
     */
    private static class BridgePattern {
        String z_alta, z_medio;
        String alta, medio, inferior;
        String zz_alta, zz_medio;
        int offsetAlta, offsetMedio, offsetInferior;

        BridgePattern(String z_alta, String z_medio,
                      String alta, String medio, String inferior,
                      String zz_alta, String zz_medio,
                      int offsetAlta, int offsetMedio, int offsetInferior) {
            this.z_alta = z_alta;
            this.z_medio = z_medio;
            this.alta = alta;
            this.medio = medio;
            this.inferior = inferior;
            this.zz_alta = zz_alta;
            this.zz_medio = zz_medio;
            this.offsetAlta = offsetAlta;
            this.offsetMedio = offsetMedio;
            this.offsetInferior = offsetInferior;
        }
    }

    /**
     * Genera puentes en las 4 direcciones desde la fortaleza
     */
    public static void generateBridgesFromBounds(ServerLevel level, BoundingBox box) {
        StructureTemplateManager templateManager = level.getStructureManager();
        int sectionLength = resolveSectionLength(templateManager);

        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;

        // Usar Y fija para la base de los puentes
        int baseY = 60;

        BlockPos eastStart = new BlockPos(box.maxX() + 1, baseY, centerZ);
        BlockPos westStart = new BlockPos(box.minX() - 1, baseY, centerZ);
        BlockPos southStart = new BlockPos(centerX, baseY, box.maxZ() + 1);
        BlockPos northStart = new BlockPos(centerX, baseY, box.minZ() - 1);

        generateBridgeFromSide(level, templateManager, eastStart, Direction.EAST, BRIDGE_EAST, sectionLength, baseY, box);
        generateBridgeFromSide(level, templateManager, westStart, Direction.WEST, BRIDGE_WEST, sectionLength, baseY, box);
        generateBridgeFromSide(level, templateManager, southStart, Direction.SOUTH, BRIDGE_SOUTH, sectionLength, baseY, box);
        generateBridgeFromSide(level, templateManager, northStart, Direction.NORTH, BRIDGE_NORTH, sectionLength, baseY, box);
    }


    /**
     * Mantiene el método original para llamadas manuales; redirige a bounding box detectado.
     */
    public static void generateBridges(ServerLevel level, BlockPos fortressCenter, int fortressY) {
        BlockPos[] bounds = findStructureBounds(level, fortressCenter);
        if (bounds == null) {
            return;
        }
        BoundingBox box = new BoundingBox(bounds[0].getX(), bounds[0].getY(), bounds[0].getZ(),
                bounds[1].getX(), bounds[1].getY(), bounds[1].getZ());
        generateBridgesFromBounds(level, box);
    }

    /**
     * Genera un puente desde un lado colocando TODAS las piezas de la matriz 3x3 en cada sección
     *
     * En cada sección coloca:
     * - z_alta, alta, zz_alta   @ Y+16 (arriba)
     * - z_medio, medio, zz_medio @ Y+0 (base)
     * - inferior                 @ Y-16 (abajo)
     */
    private static void generateBridgeFromSide(ServerLevel level, StructureTemplateManager templateManager,
                                               BlockPos start, Direction direction, BridgePattern pattern,
                                               int sectionLength, int yBase, BoundingBox dungeonBox) {
        System.out.println("[FortressBridge] ========================================");
        System.out.println("[FortressBridge] Puente desde " + start + " dir=" + direction);
        System.out.println("[FortressBridge] Offsets: ALTA=" + pattern.offsetAlta +
                          ", MEDIO=" + pattern.offsetMedio +
                          ", INFERIOR=" + pattern.offsetInferior);
        System.out.println("[FortressBridge] ========================================");

        BlockPos currentPos = new BlockPos(start.getX(), yBase, start.getZ());
        int distanceTraveled = 0;
        boolean firstSection = true;

        // Generar el puente por secciones
        while (distanceTraveled < MAX_BRIDGE_LENGTH) {
            // Si la siguiente sección está dentro del bounding box de la dungeon, parar
            if (isInsideDungeonBox(currentPos, direction, sectionLength, dungeonBox)) {
                System.out.println("[FortressBridge] Puente llegó a la dungeon en: " + currentPos);
                break;
            }
            if (!firstSection && hasReachedSolidGround(level, currentPos, direction)) {
                System.out.println("[FortressBridge] Puente alcanzó tierra sólida a distancia: " + distanceTraveled);
                break;
            }

            System.out.println("[FortressBridge] --- Sección " + distanceTraveled + " @ " + currentPos + " ---");

            // NIVEL ALTO (Y+16)
            BlockPos posAlta = currentPos.offset(0, pattern.offsetAlta, 0);
            placeBridgePiece(level, templateManager, posAlta, pattern.z_alta, direction);
            System.out.println("[FortressBridge]   z_alta  @ " + posAlta + " (Y+" + pattern.offsetAlta + ")");

            placeBridgePiece(level, templateManager, posAlta, pattern.alta, direction);
            System.out.println("[FortressBridge]   alta    @ " + posAlta + " (Y+" + pattern.offsetAlta + ")");

            placeBridgePiece(level, templateManager, posAlta, pattern.zz_alta, direction);
            System.out.println("[FortressBridge]   zz_alta @ " + posAlta + " (Y+" + pattern.offsetAlta + ")");

            // NIVEL MEDIO (Y+0)
            BlockPos posMedio = currentPos.offset(0, pattern.offsetMedio, 0);
            placeBridgePiece(level, templateManager, posMedio, pattern.z_medio, direction);
            System.out.println("[FortressBridge]   z_medio @ " + posMedio + " (Y+" + pattern.offsetMedio + ")");

            placeBridgePiece(level, templateManager, posMedio, pattern.medio, direction);
            System.out.println("[FortressBridge]   medio   @ " + posMedio + " (Y+" + pattern.offsetMedio + ")");

            placeBridgePiece(level, templateManager, posMedio, pattern.zz_medio, direction);
            System.out.println("[FortressBridge]   zz_medio@ " + posMedio + " (Y+" + pattern.offsetMedio + ")");

            // NIVEL BAJO (Y-16)
            BlockPos posInferior = currentPos.offset(0, pattern.offsetInferior, 0);
            placeBridgePiece(level, templateManager, posInferior, pattern.inferior, direction);
            System.out.println("[FortressBridge]   inferior@ " + posInferior + " (Y" + pattern.offsetInferior + ")");

            // Avanzar a la siguiente posición
            currentPos = currentPos.relative(direction, sectionLength);
            distanceTraveled += sectionLength;
            firstSection = false;
        }

        System.out.println("[FortressBridge] ========================================");
        System.out.println("[FortressBridge] Puente finalizado. Distancia total: " + distanceTraveled);
        System.out.println("[FortressBridge] ========================================");
    }


    // Verifica si la siguiente sección del puente está dentro del bounding box de la dungeon
    private static boolean isInsideDungeonBox(BlockPos pos, Direction direction, int sectionLength, BoundingBox box) {
        BlockPos nextPos = pos.relative(direction, sectionLength);
        return box.isInside(nextPos);
    }

    /**
     * Busca los límites de la estructura buscando sus bloques no-aire
     */
    private static BlockPos[] findStructureBounds(ServerLevel level, BlockPos center) {
        int searchRadius = 150;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean found = false;

        // Buscar bloques que pertenezcan a la estructura
        for (int x = center.getX() - searchRadius; x <= center.getX() + searchRadius; x++) {
            for (int y = center.getY() - 50; y <= center.getY() + 50; y++) {
                for (int z = center.getZ() - searchRadius; z <= center.getZ() + searchRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // Si es un bloque de estructura (no es aire, no es bedrock)
                    if (!state.isAir() && state.getBlock() != net.minecraft.world.level.block.Blocks.BEDROCK) {
                        // Verificar si es un bloque de la estructura (aproximado)
                        if (isStructureBlock(state)) {
                            found = true;
                            minX = Math.min(minX, x);
                            maxX = Math.max(maxX, x);
                            minY = Math.min(minY, y);
                            maxY = Math.max(maxY, y);
                            minZ = Math.min(minZ, z);
                            maxZ = Math.max(maxZ, z);
                        }
                    }
                }
            }
        }

        if (!found) {
            return null;
        }

        return new BlockPos[]{
            new BlockPos(minX, minY, minZ),
            new BlockPos(maxX, maxY, maxZ)
        };
    }

    /**
     * Determina si un bloque pertenece a la estructura
     */
    private static boolean isStructureBlock(BlockState state) {
        // Los bloques de la estructura deberían ser bloques típicos: piedra, ladrillo, etc.
        String blockName = state.getBlock().getName().getString();

        // Excluir agua, lava, plantas, etc.
        return !blockName.contains("air") &&
               !blockName.contains("water") &&
               !blockName.contains("lava") &&
               !blockName.contains("tall_") &&
               !blockName.contains("flower") &&
               !blockName.contains("grass") &&
               !blockName.contains("vine");
    }


    /**
     * Coloca una pieza del puente
     */
    private static void placeBridgePiece(ServerLevel level, StructureTemplateManager templateManager,
                                         BlockPos pos, String pieceName, Direction direction) {
        ResourceLocation pieceLocation = ResourceLocation.parse(pieceName);
        Optional<StructureTemplate> templateOpt = templateManager.get(pieceLocation);

        if (templateOpt.isEmpty()) {
            System.err.println("[FortressBridge] ERROR: No encontrado: " + pieceName);
            return;
        }

        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(getRotationForDirection(direction))
                .setIgnoreEntities(false);

        try {
            template.placeInWorld(level, pos, pos, settings, level.random, 2);
        } catch (Exception e) {
            System.err.println("[FortressBridge] ERROR al colocar " + pieceName + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si alcanzó tierra sólida
     */
    private static boolean hasReachedSolidGround(ServerLevel level, BlockPos bridgePos, Direction direction) {
        int bridgeWidth = 5;
        Direction perpendicular = direction.getClockWise();

        for (int i = -bridgeWidth / 2; i <= bridgeWidth / 2; i++) {
            BlockPos checkPos = bridgePos.relative(perpendicular, i);

            if (level.getBlockState(checkPos.relative(direction)).canOcclude() ||
                level.getBlockState(checkPos.below()).canOcclude() ||
                level.getBlockState(checkPos.above()).canOcclude()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene la rotación para una dirección
     */
    private static Rotation getRotationForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Rotation.NONE;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case EAST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static int resolveSectionLength(StructureTemplateManager templateManager) {
        Optional<StructureTemplate> templateOpt = templateManager.get(ResourceLocation.parse(BRIDGE_EAST.medio));
        if (templateOpt.isEmpty()) {
            return SECTION_LENGTH;
        }
        Vec3i size = templateOpt.get().getSize();
        // Usar X por defecto (puede ajustarse según necesidad)
        return size.getX();
    }
}
