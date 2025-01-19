package aurum.aurum.init;

import aurum.aurum.structures.DynamicStructures.Prison.DynamicPrisonStructure;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

import static aurum.aurum.Aurum.MODID;

public class ModStructures {

    // Registro del DeferredRegister para estructuras
    public static final DeferredRegister<Structure> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE, MODID);


    // Registro de la prisión dinámica
    public static final DeferredHolder<Structure, DynamicPrisonStructure> DYNAMIC_PRISON = STRUCTURES.register("dynamic_prison",
            () -> new DynamicPrisonStructure(new Structure.StructureSettings(    HolderSet.direct(), Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE
            )));

    public static void register(IEventBus eventBus) {
        STRUCTURES.register(eventBus);
    }
}

