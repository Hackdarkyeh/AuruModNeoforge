package aurum.aurum.init;


import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;

public class ModBloques {

    // Crea un Registro Diferido para mantener los ModBloques que se registrarán bajo el espacio de nombres "aurum"
    //public static final DeferredRegister<MapCodec<? extends Block>> BLOCKS_REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCK_REGISTRY = DeferredRegister.createBlocks(MODID);

    // Crea un Registro Diferido para mantener los ModItems que se registrarán bajo el espacio de nombres "aurum"
    // Crea un Registro Diferido para mantener las Pestañas de Modo Creativo que se registrarán bajo el espacio de nombres "examplemod"
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Crea un nuevo Bloque con el id "aurum:example_block", combinando el espacio de nombres y la ruta
    //public static final Supplier<Block> WITHERED_GRASS_BLOCK = BLOCKS_REGISTRY.register("withered_grass_block", Withered_grass_block::new);
    public static final DeferredBlock<Block> WITHERED_DIRT_BLOCK = registerBlock("withered_dirt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> WITHERED_GRASS_BLOCK = registerBlock("withered_grass_block",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE)));


    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS_REGISTRY.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCK_REGISTRY.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    /*
    public static final DeferredBlock<Block> WITHERED_DIRT_BLOCK = BLOCK_REGISTRY.registerBlock(
            "withered_dirt_block",
            Block::new, // The factory that the properties will be passed into.
            BlockBehaviour.Properties.of() // The properties to use.
    );

    public static final DeferredBlock<Block> WITHERED_GRASS_BLOCK = BLOCK_REGISTRY.registerBlock(
            "withered_grass_block",
            Block::new, // The factory that the properties will be passed into.
            BlockBehaviour.Properties.of() // The properties to use.
    );
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<Withered_dirt_block>> COMPLEX_CODEC_WITHERED_DIRT_BLOCK = BLOCKS_REGISTRY.register(
            "withered_dirt_block",
            () -> RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Codec.INT.fieldOf("value").forGetter(Withered_dirt_block::getValue),
                            BlockBehaviour.propertiesCodec() // represents the BlockBehavior.Properties parameter
                    ).apply(instance, Withered_dirt_block::new)
            )
    );

    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<Withered_grass_block>> COMPLEX_CODEC_WITHERED_GRASS_BLOCK = BLOCKS_REGISTRY.register(
            "withered_grass_block",
            () -> RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Codec.INT.fieldOf("value").forGetter(Withered_grass_block::getValue),
                            BlockBehaviour.propertiesCodec() // represents the BlockBehavior.Properties parameter
                    ).apply(instance, Withered_grass_block::new)
            )
    );

     */



    // Crea un nuevo BlockItem con el id "aurum:example_block", combinando el espacio de nombres y la ruta
    //public static final RegistryObject<Item> WITHERED_GRASS_BLOCK_ITEM = ITEMS.register("withered_grass_block", () -> new BlockItem(WITHERED_GRASS_BLOCK.get(), new Item.Properties()));
}
