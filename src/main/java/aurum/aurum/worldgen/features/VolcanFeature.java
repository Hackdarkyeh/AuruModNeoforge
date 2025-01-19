package aurum.aurum.worldgen.features;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.Blocks;

// Importaciones necesarias
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

// Definimos la clase VolcanoFeature
public class VolcanFeature extends Feature<NoneFeatureConfiguration> {

    // Constructor que usa NoneFeatureConfiguration porque no necesitamos parámetros complejos
    public VolcanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    // Lógica de la generación del volcán
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos position = context.origin(); // Posición inicial para generar el volcán
        WorldGenLevel world = context.level(); // Nivel donde se genera

        // Generación de un volcán básico, simplemente con anillos de piedra y lava
        int radius = 5; // Radio básico del volcán

        // Crear la base de piedra
        for (int y = 0; y < 10; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Poner bloques de piedra en forma circular
                    if (x * x + z * z <= radius * radius) {
                        world.setBlock(new BlockPos(position.getX() + x, position.getY() + y, position.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
            radius--; // Reducir el radio conforme subimos para darle forma de volcán
        }

        // Añadir lava en el centro
        world.setBlock(new BlockPos(position.getX(), position.getY() + 10, position.getZ()), Blocks.LAVA.defaultBlockState(), 2);
        return true; // Retorna true si la generación es exitosa
    }
}


