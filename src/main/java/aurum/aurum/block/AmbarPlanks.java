package aurum.aurum.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AmbarPlanks extends Block {

    // Constructor que permite pasar propiedades personalizadas desde el registro
    public AmbarPlanks(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // Constructor por defecto con las características estándar de tablones de madera
    public AmbarPlanks() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE) // Color al verse en un mapa
                .strength(2.0F, 3.0F)            // Dureza y resistencia a explosiones
                .sound(SoundType.WOOD)           // Sonido al pisar/romper
                .ignitedByLava()                 // Permite que se queme
        );
    }
}