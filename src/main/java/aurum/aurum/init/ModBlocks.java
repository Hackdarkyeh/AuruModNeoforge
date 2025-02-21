package aurum.aurum.init;


import aurum.aurum.block.*;
import aurum.aurum.block.engineering.*;
import aurum.aurum.block.engineering.EnergyGeneratorBlock.EnergyGeneratorBlock;
import aurum.aurum.block.engineering.EnergyStorageBlock.EnergyStorageBlock;
import aurum.aurum.block.engineering.ExtractorBlock.ExtractorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


import javax.annotation.Nullable;
import java.util.function.Supplier;

import static aurum.aurum.Aurum.MODID;

public class ModBlocks {

    // Crea un Registro Diferido para mantener los ModBlocks que se registrarán bajo el espacio de nombres "aurum"
    //public static final DeferredRegister<MapCodec<? extends Block>> BLOCKS_REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCK_REGISTRY = DeferredRegister.createBlocks(MODID);

    // Crea un Registro Diferido para mantener los ModItems que se registrarán bajo el espacio de nombres "aurum"
    // Crea un Registro Diferido para mantener las Pestañas de Modo Creativo que se registrarán bajo el espacio de nombres "examplemod"
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Crea un nuevo Bloque con el id "aurum:example_block", combinando el espacio de nombres y la ruta
    //public static final Supplier<Block> WITHERED_GRASS_BLOCK = BLOCKS_REGISTRY.register("withered_grass_block", Withered_grass_block::new);
    public static final DeferredBlock<Block> WITHERED_DIRT_BLOCK = registerBlock("withered_dirt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> WITHERED_GRASS_BLOCK = registerBlock("withered_grass_block", WitheredGrassBlock::new);

    public static final DeferredBlock<Block> ANCIENT_AURUM_DIRT_BLOCK = registerBlock("ancient_aurum_dirt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> ANCIENT_AURUM_GRASS_BLOCK= registerBlock("ancient_aurum_grass_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> PORTAl_BLOCK_AURUM = BLOCK_REGISTRY.register("aurum_portal_block", PortalBlock::new);


    /*
    public static final DeferredBlock<Block> WITHERED_GRASS_BLOCK = registerBlock("withered_grass_block",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3f).requiresCorrectToolForDrops().sound(SoundType.GRASS)));
    */

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS_REGISTRY.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    public static final DeferredBlock<Block> PLAGUE_AURUM_BLOCK = BLOCK_REGISTRY.register("plague_aurum_block", PlagueAurumBlock::new);



    public static final DeferredBlock<Block> PIPE_BLOCK = registerBlock("pipe", PipeBlock::new);
    public static final DeferredBlock<Block> PANEL_BLOCK = registerBlock("panel", PipeBlock::new);
    public static final DeferredBlock<Block> EXTRACTOR_BLOCK = registerBlock("extractor", ExtractorBlock::new);
    public static final DeferredBlock<Block> ENERGY_STORAGE_BLOCK = registerBlock("energy_storage_block", EnergyStorageBlock::new);
    public static final DeferredBlock<Block> RESOURCE_STORAGE_BLOCK = registerBlock("resource_storage", ResourceStorageBlock::new);
    public static final DeferredBlock<Block> ENERGY_GENERATOR_BLOCK = registerBlock("energy_generator_block",  EnergyGeneratorBlock::new);




    public static final DeferredBlock<Block> AURELITE_ORE = registerBlock("aurelite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHER_ORE)
                    .isValidSpawn((state, world, pos, type) -> false) // Impide que las entidades puedan spawnear en el bloque
                    .destroyTime(Float.MAX_VALUE) // Aumenta el tiempo de rotura, lo que lo hace casi irrompible
            ) {
                @Override
                public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
                    // Este método solo permitirá que el bloque se rompa en modo creativo
                    return player.isCreative();
                }

                @Override
                public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
                    if (player.isCreative()) {
                        // Permite que el bloque se rompa y caiga normalmente solo si el jugador está en modo creativo
                        super.playerDestroy(world, player, pos, state, blockEntity, stack);
                    } else {
                        // En caso contrario, evita que el bloque suelte ítems
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
    );









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
