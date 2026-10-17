package aurum.aurum.block.engineering.PortalAurum;

import aurum.aurum.block.engineering.PedestalBlock.PedestalBlock;
import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntity;
import aurum.aurum.init.ModBlockEntities;
import aurum.aurum.init.ModBlocks;
import aurum.aurum.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractInoculatedObsidianBlockEntity extends BlockEntity {

    // Coordenadas relativas (Rellenar con los 40 bloques y la posición de los pedestales)
    private static final BlockPos[] FRAME_OFFSETS = new BlockPos[] {
            // Ejemplo de una base cuadrada o circular alrededor del centro (0,0,0)
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

    // Coordenadas exactas del interior vacío de tu marco de obsidiana
    // TODO: Ajusta estas coordenadas para que coincidan con los huecos de tu diseño
    private static final BlockPos[] INSIDE_PORTAL_OFFSETS = new BlockPos[] {
            new BlockPos(0, -2, -2), new BlockPos(-1, -2, -2), new BlockPos(1, -2, -2),
            new BlockPos(-2, -2, -1), new BlockPos(-1, -2, -1), new BlockPos(0, -2, -1), new BlockPos(1, -2, -1), new BlockPos(2, -2, -1),
            new BlockPos(1, -2, -0), new BlockPos(2, -2, -0), new BlockPos(-1, -2, 0), new BlockPos(-2, -2, 0),
            new BlockPos(-2, -2, 1), new BlockPos(-1, -2, 1), new BlockPos(0, -2, 1), new BlockPos(1, -2, 1), new BlockPos(2, -2, 1),
            new BlockPos(0, -2, 2), new BlockPos(-1, -2, 2), new BlockPos(1, -2, 2),
    };

    private static final BlockPos[] PEDESTAL_OFFSETS = new BlockPos[] {
            new BlockPos(0, -2, -5), // Pedestal Noroeste
            new BlockPos(-3, -2, -4), new BlockPos(3, -2, -4),
            new BlockPos(-4, -2, -3), new BlockPos(4, -2, -3),
            new BlockPos(-5, -2, 0), new BlockPos(5, -2, 0),
            new BlockPos(-4, -2, 3), new BlockPos(4, -2, 3),
            new BlockPos(-3, -2, 4), new BlockPos(3, -2, 4),
            new BlockPos(0, -2, 5)
    };

    private static final List<Item> REQUIRED_ITEMS = Arrays.asList(
            Items.DRAGON_HEAD,
            Items.ELYTRA,
            Items.NETHER_STAR,
            Items.NETHERITE_BLOCK,
            Items.GOLD_BLOCK,
            // TODO: Cambiar estos 7 por los definitivos cuando los pienses
            Items.DIAMOND_BLOCK, Items.EMERALD_BLOCK, Items.IRON_BLOCK,
            Items.AMETHYST_BLOCK, Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.TOTEM_OF_UNDYING
    );

    private final int PILLAR_RADIUS = 15; // Distancia de los estabilizadores al centro
    private final int PILLAR_HEIGHT = 6;

    private int crackingYLevel = PILLAR_HEIGHT - 1;


    // Máquina de estados del portal
    public enum PortalState {
        IDLE,               // Esperando estructura
        MISSING_PEDESTALS,  // Estructura OK, faltan pedestales/ítems
        RAID_ACTIVE,        // Oleadas en progreso
        OPENING_ANIMATION,  // Cinemática del dragón muerto
        PORTAL_OPEN         // Raid superada, portal activo
    }

    private int animationTimer = 0; // El reloj de nuestra cinemática

    private PortalState currentState = PortalState.IDLE;
    private int raidPhase = 0;
    private int enemiesToKill = 0;
    private int tickCounter = 0;

    // Definimos la BossBar del servidor (color morado/purple o el que prefieras, y estilo de muescas)
    private final net.minecraft.server.level.ServerBossEvent raidBossBar =
            new net.minecraft.server.level.ServerBossEvent(
                    net.minecraft.network.chat.Component.translatable("bossbar.aurum.invasion"),
                    net.minecraft.server.level.ServerBossEvent.BossBarColor.PURPLE,
                    net.minecraft.server.level.ServerBossEvent.BossBarOverlay.NOTCHED_10
            );

    public AbstractInoculatedObsidianBlockEntity(BlockPos pPos, BlockState pState) {
        super(ModBlockEntities.INOCULATED_OBSIDIAN_BLOCK_ENTITY.get(), pPos, pState);
        raidBossBar.setVisible(false);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractInoculatedObsidianBlockEntity pEntity) {
        pEntity.tickCounter++;

        if (pEntity.raidBossBar.isVisible() && pLevel instanceof ServerLevel serverLevel) {
            // Añade a los jugadores en un radio de 50 bloques para que vean la barra
            List<net.minecraft.server.level.ServerPlayer> players = serverLevel.getPlayers(player -> player.distanceToSqr(pPos.getX(), pPos.getY(), pPos.getZ()) <= 50 * 50);

            // Sincroniza los jugadores actuales con la barra
            // (ServerBossEvent gestiona automáticamente quién la ve y quién no)
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                boolean inRange = players.contains(player);
                boolean hasPlayer = pEntity.raidBossBar.getPlayers().contains(player);
                if (inRange && !hasPlayer) {
                    pEntity.raidBossBar.addPlayer(player);
                } else if (!inRange && hasPlayer) {
                    pEntity.raidBossBar.removePlayer(player);
                }
            }
        }

        switch (pEntity.currentState) {
            case IDLE:
                if (pEntity.tickCounter % 20 == 0) {
                    // Restringir exclusivamente al Overworld
                    if (pLevel.dimension() != Level.OVERWORLD) {
                        return; // Si no es el Overworld, se detiene y no hace nada
                    }

                    if (pEntity.checkFrame(pLevel, pPos)) {
                        pEntity.currentState = PortalState.MISSING_PEDESTALS;
                        pEntity.sync();
                        System.out.println("¡Estructura completa! Ahora faltan los pedestales con los ítems.");
                    }
                }
                break;

            case MISSING_PEDESTALS:
                if (pEntity.tickCounter % 20 == 0) {
                    // Si rompen el marco, volvemos a IDLE
                    if (!pEntity.checkFrame(pLevel, pPos)) {
                        pEntity.currentState = PortalState.IDLE;
                        pEntity.sync();
                        return;
                    }
                    System.out.println("Comprobando pedestales e ítems...");
                    if (pEntity.checkPedestalsAndItems(pLevel, pPos)) {
                        pEntity.startRaid(pLevel, pPos);
                        pEntity.sync();
                    }
                }
                break;

            case RAID_ACTIVE:
                pEntity.handleRaidLogic(pLevel, pPos, pState);
                break;

            case PORTAL_OPEN:
                // Lógica del portal abierto (partículas constantes, teletransporte, etc.)
                // Si la estructura indestructible se ve comprometida (ej. por comandos), apagar portal.
                break;
        }
        // Corrección: Usamos pEntity en lugar de this.
        if (pEntity.raidPhase == 4) {
            pEntity.checkAndCrackPillars(pLevel, pPos);
        }

        if (pEntity.currentState == PortalState.OPENING_ANIMATION) {
            pEntity.playOpeningCinematic(pLevel, pPos);
            return; // Detenemos la lógica de la raid, ahora todo es cinemática
        }
    }

    private void handleRaidLogic(Level pLevel, BlockPos pPos, BlockState pState) {
        // 1. Escaneamos enemigos SOLO 1 vez por segundo (cada 20 ticks).
        // Esto optimiza el servidor y evita el "punto ciego" al spawnear entidades masivas.
        if (this.tickCounter % 20 == 0) {
            AABB raidArea = new AABB(pPos).inflate(150);

            // 2. Usamos LivingEntity.class en lugar de Mob.class para no filtrar las partes del dragón
            List<net.minecraft.world.entity.LivingEntity> activeEnemies = pLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    raidArea,
                    entity -> entity.getTags().contains("aurum_raid_mob")
            );

            this.enemiesToKill = activeEnemies.size();

            // 3. Progresión de las fases (solo evaluamos si se ganó una vez por segundo)
            if (this.enemiesToKill == 0) {
                if (this.raidPhase < 4) {
                    this.raidPhase++;
                    spawnWave(pLevel, pPos, this.raidPhase);
                    this.sync();
                } else if (this.raidPhase == 4) {
                    this.currentState = PortalState.OPENING_ANIMATION;
                    this.animationTimer = 0;
                    this.sync();
                }
            }
        }

        // --- ACTUALIZAR PROGRESO DE LA BOSSBAR (Esto sí ocurre cada tick para ser fluido) ---
        this.raidBossBar.setName(net.minecraft.network.chat.Component.translatable(
                "bossbar.aurum.phase_progress",
                this.raidPhase,
                this.enemiesToKill
        ));

        float phaseProgress = (this.raidPhase - 1) / 4.0F;
        this.raidBossBar.setProgress(Math.clamp(phaseProgress + (1.0F - (this.enemiesToKill / 20.0F)) * 0.25F, 0.0F, 1.0F));

        // Parpadeo visual del bloque del núcleo basado en rondas y enemigos restantes
        int baseBlinkSpeed = 25 - (this.raidPhase * 5);
        int blinkInterval = Math.max(2, baseBlinkSpeed + (this.enemiesToKill * 2));

        if (this.tickCounter % blinkInterval == 0) {
            boolean isLit = pState.getValue(AbstractInoculatedObsidianBlock.LIT);
            pLevel.setBlock(pPos, pState.setValue(AbstractInoculatedObsidianBlock.LIT, !isLit), 3);
        }
    }

    private void startRaid(Level pLevel, BlockPos pPos) {
        this.currentState = PortalState.RAID_ACTIVE;
        this.raidPhase = 1;

        // --- GESTIÓN DE LA BOSSBAR CON TRADUCCIÓN ---
        this.raidBossBar.setVisible(true);
        this.raidBossBar.setProgress(0.0F);
        this.raidBossBar.setName(net.minecraft.network.chat.Component.translatable(
                "bossbar.aurum.phase_progress",
                this.raidPhase,
                this.enemiesToKill
        ));

        // 1. Blindar el marco
        lockFrame(pLevel, pPos);
        // 2. Activar pedestales
        activateAndConsumePedestals(pLevel, pPos);
        // 3. Aparece la primera oleada
        spawnWave(pLevel, pPos, this.raidPhase);

        this.sync();
    }

    private void activateAndConsumePedestals(Level level, BlockPos center) {
        for (BlockPos offset : PEDESTAL_OFFSETS) {
            BlockPos checkPos = center.offset(offset);
            BlockState state = level.getBlockState(checkPos);
            BlockEntity be = level.getBlockEntity(checkPos);

            if (be instanceof PedestalBlockEntity pedestalBe) {
                // Aprovechamos tu método clearContents() para vaciar el pedestal
                pedestalBe.clearContents();

                // Cambiamos el bloque al estado ENCHANTED = true
                // Esto cambia su textura, lo hace indestructible y bloquea pistones
                if (state.hasProperty(PedestalBlock.ENCHANTED)) {
                    level.setBlock(checkPos, state.setValue(PedestalBlock.ENCHANTED, true), 3);
                }
            }
        }
    }


    private boolean checkFrame(Level level, BlockPos center) {
        for (BlockPos offset : FRAME_OFFSETS) {
            BlockPos checkPos = center.offset(offset);
            BlockState state = level.getBlockState(checkPos);

            // El marco es válido si es OBSIDIAN (antes de empezar) O si ya es AURUM_PORTAL_BLOCK_INDESTRUCTIBLE (durante/después)
            boolean isValidObsidian = state.is(Blocks.OBSIDIAN);
            boolean isValidIndestructible = state.is(ModBlocks.AURUM_PORTAL_BLOCK_INDESTRUCTIBLE.get());

            if (!isValidObsidian && !isValidIndestructible) {
                // Genera partículas de error en el centro exacto del bloque problemático
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                            3, // Cantidad de partículas
                            0.2, 0.2, 0.2, // Dispersión en X, Y, Z
                            0.0);
                }
                return false; // Se detiene en el primer error que encuentra
            }
        }
        return true;
    }

    private void lockFrame(Level level, BlockPos center) {
        for (BlockPos offset : FRAME_OFFSETS) {
            BlockPos checkPos = center.offset(offset);

            // 1. Limpia la entidad de bloque existente
            level.removeBlockEntity(checkPos);

            // 2. Pon aire con flag 2 (actualización silenciosa en servidor)
            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 2);

            // 3. Coloca el bloque definitivo del marco con flag 3 (actualiza cliente y render)
            level.setBlock(checkPos, ModBlocks.AURUM_PORTAL_BLOCK_INDESTRUCTIBLE.get().defaultBlockState(), 3);
        }
    }


    private boolean checkPedestalsAndItems(Level level, BlockPos center) {
        // Hacemos una copia de la lista de ítems requeridos para ir tachándolos
        List<Item> missingItems = new ArrayList<>(REQUIRED_ITEMS);

        for (BlockPos offset : PEDESTAL_OFFSETS) {
            BlockPos checkPos = center.offset(offset);
            BlockEntity be = level.getBlockEntity(checkPos);

            if (be instanceof PedestalBlockEntity pedestalBe) {
                ItemStack stackInPedestal = pedestalBe.inventory.getStackInSlot(0);

                if (stackInPedestal.isEmpty()) {
                    return false; // Falta ítem
                }

                Item itemInPedestal = stackInPedestal.getItem();

                // Si el ítem está en la lista de los que faltan, lo tachamos (remove)
                if (missingItems.contains(itemInPedestal)) {
                    missingItems.remove(itemInPedestal);
                } else {
                    // Falla si el ítem no está en la lista o si ya lo tachamos (es un duplicado)
                    return false;
                }
            } else {
                return false; // Falla si rompieron el pedestal
            }
        }

        // Si la lista quedó vacía, significa que encontramos exactamente 1 de cada uno de los 12 ítems sin repetir
        return missingItems.isEmpty();
    }


    private void spawnWave(Level level, BlockPos center, int phase) {
        switch (phase) {
            case 1:
                // Ronda 1: Inicia la construcción parcial de los pilares (25% de altura)
                constructProgressivePillars(level, center, 1);

                // --- INVOCACIÓN DE FASE 1 ---
                // ... dentro de tu switch en spawnWave() ...

                // Escala la cantidad de enemigos según la ronda (ej: 8, 12, 16...)
                int playerCount = level.getEntitiesOfClass(Player.class, new AABB(center).inflate(50)).size();

                // 2. Por seguridad, asegurarnos de que el multiplicador nunca sea 0 (por si mueren o huyen justo al empezar)
                int multiplier = Math.max(1, playerCount);

                // 3. Aplicar el multiplicador a la fórmula
                int mobsToSpawn = (4 + (phase * 4)) * multiplier;
                for (int i = 0; i < mobsToSpawn; i++) {
                    // Pedimos el monstruo aleatorio basado en la fase
                    Mob enemy = getRandomMobForPhase(level, phase);

                    if (enemy != null) {
                        // 1. Calcular coordenadas en anillo (radio 10-25 bloques)
                        double angle = level.random.nextDouble() * 2 * Math.PI;
                        double distance = 10 + level.random.nextDouble() * 15;
                        int targetX = center.getX() + (int)(distance * Math.cos(angle));
                        int targetZ = center.getZ() + (int)(distance * Math.sin(angle));

                        BlockPos targetXZ = new BlockPos(targetX, center.getY(), targetZ);
                        BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetXZ);

                        enemy.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);

                        // 2. ¡VITAL! Etiquetar para que el núcleo registre las muertes
                        enemy.addTag("aurum_raid_mob");
                        enemy.setPersistenceRequired();

                        level.addFreshEntity(enemy);
                    }
                }
                break;
            case 2:
                // Ronda 2: Los pilares crecen un poco más
                constructProgressivePillars(level, center, 2);
                // TODO: Invocar enemigos de fase 2
                break;
            case 3:
                // Ronda 3: Los pilares casi alcanzan su tope
                constructProgressivePillars(level, center, 3);
                // TODO: Invocar enemigos de fase 3
                break;
            case 4:
                // 1. Los pilares alcanzan su altura máxima (100%) y aparecen los End Crystals
                constructProgressivePillars(level, center, 4);

                // 2. Invocar al Jefe Final
                Mob boss = ModEntities.ENDER_DRAGON_ALTERED.get().create(level);

                if (boss != null) {
                    // Lo hacemos aparecer justo en el centro del portal, pero a 20 bloques de altura para que inicie volando
                    boss.moveTo(center.getX() + 0.5, center.getY() + 20, center.getZ() + 0.5, 0.0F, 0.0F);

                    // ¡VITAL! Le ponemos la etiqueta de la raid. Al ser el único mob invocado en esta fase, enemiesToKill será 1.
                    boss.addTag("aurum_raid_mob");

                    // Evitamos que desaparezca si el jugador se aleja
                    boss.setPersistenceRequired();

                    // Lo añadimos al mundo
                    level.addFreshEntity(boss);
                }
                break;
        }
    }

    // Estructura de datos para definir cada uno de los 10 pilares
    // Multiplicador global: Si quieres que estén más lejos, sube este número (ej: 1.3 o 1.5). Si quieres acercarlos, bájalo (ej: 0.8).
    private static final double PILLAR_DISTANCE_MULTIPLIER = 2.1;

    // Estructura de datos para definir cada uno de los 10 pilares (ahora guardamos coordenadas base)
    private static class PillarConfig {
        final double baseX, baseZ; // Coordenadas base (dirección)
        final int finalWidth;      // Entre 5 y 11
        final int maxHeight;       // Altura máxima

        public PillarConfig(double baseX, double baseZ, int finalWidth, int maxHeight) {
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.finalWidth = finalWidth;
            this.maxHeight = maxHeight;
        }

        // Método automático para obtener la posición real aplicando el multiplicador global
        public BlockPos getOffset() {
            return new BlockPos(
                    (int) Math.round(baseX * PILLAR_DISTANCE_MULTIPLIER),
                    0,
                    (int) Math.round(baseZ * PILLAR_DISTANCE_MULTIPLIER)
            );
        }
    }

    // Definimos los 12 pilares con sus proporciones base
    private static final PillarConfig[] PILLARS = new PillarConfig[] {
            new PillarConfig(42, 0, 11, 26),
            new PillarConfig(36, 21, 9, 23),
            new PillarConfig(21, 36, 7, 20),
            new PillarConfig(0, 42, 10, 24),
            new PillarConfig(-21, 36, 8, 21),
            new PillarConfig(-36, 21, 6, 17),
            new PillarConfig(-42, 0, 5, 12),
            new PillarConfig(-36, -21, 7, 15),
            new PillarConfig(-21, -36, 8, 18),
            new PillarConfig(0, -42, 9, 22),
            new PillarConfig(21, -36, 7, 19),
            new PillarConfig(36, -21, 6, 14)     
    };

    private void constructProgressivePillars(Level pLevel, BlockPos center, int currentPhase) {
        for (int i = 0; i < PILLARS.length; i++) {
            PillarConfig pillar = PILLARS[i];
            BlockPos offset = pillar.getOffset();
            BlockPos targetXZ = center.offset(offset.getX(), 0, offset.getZ());
            BlockPos surfacePos = pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, targetXZ);

            // Progreso general según la fase (0.25 para Fase 1, 0.50 para Fase 2, etc.)
            float progress = currentPhase / 4.0F;
            int targetHeight = Math.max(3, (int) (pillar.maxHeight * progress));
            int baseRadius = pillar.finalWidth / 2;

            // Construir el pilar capa por capa con variaciones irregulares (cortes desuniformes)
            for (int y = 0; y < targetHeight; y++) {

                // Usamos una función trigonométrica mezclada con el índice del pilar y la altura 'y'
                // para que cada capa tenga un radio ligeramente distinto (crea muescas y salientes asimétricos)
                double irregularity = Math.sin(y * 0.7 + i) * 0.8 + Math.cos(y * 0.3) * 0.5;
                int currentLayerRadius = Math.max(2, (int) Math.round(baseRadius + irregularity));

                for (int x = -currentLayerRadius; x <= currentLayerRadius; x++) {
                    for (int z = -currentLayerRadius; z <= currentLayerRadius; z++) {

                        // Añadimos un pequeño desvío en las esquinas para romper la forma redonda perfecta
                        if (x * x + z * z <= (currentLayerRadius * currentLayerRadius) + (y % 2 == 0 ? 1 : 0)) {
                            BlockPos blockPos = surfacePos.above(y).offset(x, 0, z);

                            if (pLevel.getBlockState(blockPos).isAir() || pLevel.getBlockState(blockPos).is(Blocks.OBSIDIAN)) {
                                pLevel.setBlock(blockPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }

            // En la Fase 4, cuando alcanza su altura máxima definitiva, colocamos el End Crystal arriba
            if (currentPhase == 4) {
                // Usamos la altura máxima real del pilar para el cristal
                BlockPos topPos = surfacePos.above(pillar.maxHeight);
                AABB crystalArea = new AABB(topPos).inflate(2);
                boolean crystalExists = !pLevel.getEntitiesOfClass(EndCrystal.class, crystalArea).isEmpty();

                if (!crystalExists) {
                    EndCrystal crystal = EntityType.END_CRYSTAL.create(pLevel);
                    if (crystal != null) {
                        crystal.moveTo(topPos.getX() + 0.5, topPos.getY(), topPos.getZ() + 0.5, 0.0F, 0.0F);
                        crystal.setShowBottom(true);
                        crystal.addTag("aurum_crystal");
                        pLevel.addFreshEntity(crystal);
                    }
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.raidBossBar != null) {
            this.raidBossBar.removeAllPlayers();
            this.raidBossBar.setVisible(false);
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            // Marca el bloque para que se actualice en el cliente
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private void openPortal(Level pLevel, BlockPos pPos, BlockState pState) {
        this.currentState = PortalState.PORTAL_OPEN;
        pLevel.setBlock(pPos, pState.setValue(AbstractInoculatedObsidianBlock.LIT, true), 3);

        // Generar la plaga (convierte la hierba)
        spreadCorruption(pLevel, pPos);

        // TODO: Construir los bloques invisibles de teletransporte dentro de tu marco
    }

    private void spreadCorruption(Level pLevel, BlockPos center) {
        // Definimos los límites (radio de 15 bloques y 5 bloques de profundidad/altura)
        BlockPos posMin = center.offset(-PILLAR_RADIUS, -5, -PILLAR_RADIUS);
        BlockPos posMax = center.offset(PILLAR_RADIUS, 5, PILLAR_RADIUS);

        // Usamos un ServerLevel para poder spawnear partículas masivas sin lag
        ServerLevel serverLevel = pLevel instanceof ServerLevel ? (ServerLevel) pLevel : null;

        BlockPos.betweenClosedStream(posMin, posMax).forEach(pos -> {
            // Distancia circular perfecta (PILLAR_RADIUS al cuadrado)
            if (pos.distSqr(center) <= (PILLAR_RADIUS * PILLAR_RADIUS)) {
                BlockState currentState = pLevel.getBlockState(pos);

                boolean blockChanged = false;

                // Corromper Hierba
                if (currentState.is(Blocks.GRASS_BLOCK)) {
                    // TODO: Cambia AURUM_GRASS por tu bloque registrado
                    // pLevel.setBlock(pos, ModBlocks.AURUM_GRASS.get().defaultBlockState(), 3);
                    blockChanged = true;
                }
                // Corromper Tierra
                else if (currentState.is(Blocks.DIRT)) {
                    // pLevel.setBlock(pos, ModBlocks.AURUM_DIRT.get().defaultBlockState(), 3);
                    blockChanged = true;
                }
                // Corromper Piedra (Opcional, pero queda genial)
                else if (currentState.is(Blocks.STONE)) {
                    // pLevel.setBlock(pos, ModBlocks.AURUM_STONE.get().defaultBlockState(), 3);
                    blockChanged = true;
                }

                // Si el bloque se corrompió, soltamos una partícula de llama de alma hacia arriba
                if (blockChanged && serverLevel != null && pLevel.random.nextFloat() < 0.3F) {
                    // Solo el 30% de los bloques emiten partículas para no saturar la tarjeta gráfica
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            1, 0.0, 0.05, 0.0, 0.02);
                }
            }
        });
    }


    private Mob getRandomMobForPhase(Level level, int phase) {
        List<EntityType<? extends Mob>> availableMobs = new ArrayList<>();

        // Base para todas las rondas (Fase 1+)
        availableMobs.add(ModEntities.ZOMBIE_ALTERED.get());
        availableMobs.add(ModEntities.SPIDER_CORRUPTED.get());
        availableMobs.add(ModEntities.SKELETON_ALTERED.get());

        // A partir de la Fase 2: Se suman los magos
        if (phase >= 2) {
            // TODO: Sustituye MAGE_ALTERED por la entidad de mago de tu mod
            // availableMobs.add(ModEntities.MAGE_ALTERED.get());
        }

        // A partir de la Fase 3: Se suman gólems o pesos pesados
        if (phase >= 3) {
            availableMobs.add(ModEntities.COOPER_GOLEM.get());
            availableMobs.add(ModEntities.DOLPHIN_ALTERED.get());
        }

        // Elegimos una entidad al azar de la lista que hayamos construido
        EntityType<? extends Mob> chosenType = availableMobs.get(level.random.nextInt(availableMobs.size()));

        return chosenType.create(level);
    }

    public PortalState getCurrentState() {
        return this.currentState;
    }

    public static BlockPos[] getPedestalOffsets() {
        return PEDESTAL_OFFSETS;
    }
    // Recuerda guardar el currentState, raidPhase, etc., en saveAdditional y cargar en load


    private void checkAndCrackPillars(Level pLevel, BlockPos center) {
        // Ejecutamos el agrietamiento cada 10 ticks (medio segundo) para que se vea el progreso
        if (this.tickCounter % 10 != 0) return;

        for (PillarConfig pillar : PILLARS) {
            // CAMBIO AQUÍ: Obtenemos el offset escalado con la distancia global
            BlockPos offset = pillar.getOffset();
            BlockPos targetXZ = center.offset(offset.getX(), 0, offset.getZ());
            BlockPos surfacePos = pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, targetXZ);

            // Calculamos la posición exacta donde debería estar el cristal de este pilar
            BlockPos topPos = surfacePos.above(pillar.maxHeight);
            AABB crystalArea = new AABB(topPos).inflate(2);

            // Comprobamos si el cristal sigue vivo
            boolean crystalExists = !pLevel.getEntitiesOfClass(EndCrystal.class, crystalArea,
                    entity -> entity.getTags().contains("aurum_crystal")).isEmpty();

            // Si el jugador ha destruido el cristal, empezamos a agrietar de arriba a abajo
            if (!crystalExists) {
                crackPillarLayer(pLevel, surfacePos, pillar);
            }
        }
    }

    private void crackPillarLayer(Level pLevel, BlockPos surfacePos, PillarConfig pillar) {
        int radius = pillar.finalWidth / 2;
        boolean layerCracked = false;

        // Escaneamos desde la altura máxima hacia el suelo
        for (int y = pillar.maxHeight - 1; y >= 0; y--) {
            // Buscamos si en esta capa 'y' todavía hay obsidiana normal
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Manteniendo la forma cilíndrica original
                    if (x * x + z * z <= radius * radius + 1) {
                        BlockPos blockPos = surfacePos.above(y).offset(x, 0, z);

                        // Si encontramos obsidiana normal, la cambiamos
                        if (pLevel.getBlockState(blockPos).is(Blocks.OBSIDIAN)) {
                            // TODO: Cambiar Blocks.CRYING_OBSIDIAN por ModBlocks.CRACKED_OBSIDIAN.get()
                            pLevel.setBlock(blockPos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);

                            // Efecto de partículas para darle dramatismo
                            pLevel.levelEvent(2001, blockPos, Block.getId(Blocks.OBSIDIAN.defaultBlockState()));
                            layerCracked = true;
                        }
                    }
                }
            }

            // Si hemos agrietado una capa, detenemos el bucle para que el resto del pilar espere al siguiente tick.
            // Esto es lo que genera el efecto visual de cascada descendente.
            if (layerCracked) {
                break;
            }
        }
    }

    private void playOpeningCinematic(Level level, BlockPos center) {
        this.animationTimer++; // Sumamos 1 tick (20 ticks = 1 segundo)

        // FASE 1 (Tick 1 - Segundos 0 a 2): El Colapso del Alma
        if (this.animationTimer == 1) {
            // Sonido épico de muerte/activación
            level.playSound(null, center, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 1.0F, 0.5F);

            // Un rayo enorme de partículas en el núcleo
            // (En un mod real, aquí enviarías un paquete al cliente para hacer el rayo continuo)
            spawnCinematicParticles(level, center, ParticleTypes.REVERSE_PORTAL, 500, 3.0);
        }

        // FASE 2 (Tick 40 - Segundos 2 a 4): Sobrecarga a los Pedestales
        if (this.animationTimer == 40) {
            level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.5F);

            for (BlockPos offset : PEDESTAL_OFFSETS) {
                BlockPos pedestalPos = center.offset(offset);
                // Partículas de explosión de energía en cada pedestal
                spawnCinematicParticles(level, pedestalPos, ParticleTypes.END_ROD, 50, 0.5);
            }
        }

        // FASE 3 (Tick 80 - Segundos 4 a 6): La Expansión de la Plaga
        if (this.animationTimer == 80) {
            level.playSound(null, center, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.0F, 0.8F);

            // Llamas a tu método para expandir la flora oscura de Aurum
            spreadCorruption(level, center);
        }

        // FASE 4 (Tick 120 - Segundos 6 a 8): El Vórtice y el Final
        if (this.animationTimer == 120) {
            // Generar los bloques del portal dentro del marco (el agua/vórtice que te teletransporta)
            generatePortalBlocks(level, center);

            // Sonido glorioso del portal abriéndose (como el portal del End o Nether)
            level.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 1.0F);

            // Partículas hacia el cielo
            spawnCinematicParticles(level, center, ParticleTypes.DRAGON_BREATH, 1000, 5.0);
        }
        // FIN DE LA ANIMACIÓN (Tick 160 = 8 segundos)
        if (this.animationTimer >= 160) {
            this.currentState = PortalState.PORTAL_OPEN;
            this.animationTimer = 0;
            this.sync();
        }
    }

    private void generatePortalBlocks(Level pLevel, BlockPos center) {
        for (BlockPos offset : INSIDE_PORTAL_OFFSETS) {
            BlockPos targetPos = center.offset(offset);

            // Verificamos que no estemos rompiendo parte del marco por error
            if (pLevel.getBlockState(targetPos).canBeReplaced()) {
                // TODO: Reemplaza con tu bloque de portal real (el que te teletransporta a tu dimensión)
                pLevel.setBlock(targetPos, ModBlocks.AURUM_PORTAL_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    // Método auxiliar para generar nubes de partículas masivas
    private void spawnCinematicParticles(Level level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particle, int count, double spread) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    count, spread, spread, spread, 0.1);
        }
    }

// --- MÉTODOS DE GUARDADO Y SINCRONIZACIÓN EN RED ---

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Guardamos el estado actual y la fase como texto/número en el bloque
        tag.putString("PortalState", this.currentState.name());
        tag.putInt("RaidPhase", this.raidPhase);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Leemos el estado cuando el bloque se carga (o cuando el cliente recibe el paquete)
        if (tag.contains("PortalState")) {
            this.currentState = PortalState.valueOf(tag.getString("PortalState"));
        }
        if (tag.contains("RaidPhase")) {
            this.raidPhase = tag.getInt("RaidPhase");
        }
    }

    // Este método prepara los datos que se van a enviar al cliente
    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // Este método es el "cartero" que envía los datos al cliente cuando llamas a sync()
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }


    public static List<Item> getRequiredItems() {
        return REQUIRED_ITEMS;
    }


}
