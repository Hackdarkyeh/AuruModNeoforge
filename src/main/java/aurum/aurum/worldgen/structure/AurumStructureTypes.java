package aurum.aurum.worldgen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AurumStructureTypes {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, "aurum");

    public static final DeferredHolder<StructureType<?>, StructureType<AurumVillageStructure>> VILLAGE =
            STRUCTURE_TYPES.register("village", () -> explicitTyping(AurumVillageStructure.CODEC));

    // Helper para evitar ambiguedad de lambdas anidadas con los genericos.
    private static <S extends Structure> StructureType<S> explicitTyping(MapCodec<S> codec) {
        return () -> codec;
    }
}
