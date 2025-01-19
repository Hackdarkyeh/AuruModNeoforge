package aurum.aurum.structures.DynamicStructures.Prison;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class PrisonPiece extends StructurePiece {

    public PrisonPiece(BlockPos pos) {
        super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, 0, new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 30, pos.getY() + 10, pos.getZ() + 30));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {

    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos blockPos) {
        // Aquí generas los bloques de la prisión.
        for (int x = 0; x < 30; x++) {
            for (int z = 0; z < 30; z++) {
                for (int y = 0; y < 10; y++) {
                    world.setBlock(new BlockPos(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }

        // Generar puertas aleatorias
        if (random.nextBoolean()) {
            world.setBlock(new BlockPos(blockPos.getX() + 5, blockPos.getY() + 1, blockPos.getZ() + 5), Blocks.IRON_DOOR.defaultBlockState(), 3);
        }
    }
}



