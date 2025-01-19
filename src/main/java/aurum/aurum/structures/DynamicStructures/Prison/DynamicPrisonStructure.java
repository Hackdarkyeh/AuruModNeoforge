package aurum.aurum.structures.DynamicStructures.Prison;


import aurum.aurum.init.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class DynamicPrisonStructure extends Structure {

    public DynamicPrisonStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    // Este método define la lógica para generar tu estructura.
    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Posición inicial de la estructura
        BlockPos startPos = new BlockPos(context.chunkPos().getMinBlockX(), 40, context.chunkPos().getMinBlockZ());

        return Optional.of(new GenerationStub(startPos, (builder) -> {
            // Aquí defines qué piezas de la estructura se deben generar.
            builder.addPiece(new PrisonPiece(startPos));
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DYNAMIC_PRISON.get().type();
    }
}

