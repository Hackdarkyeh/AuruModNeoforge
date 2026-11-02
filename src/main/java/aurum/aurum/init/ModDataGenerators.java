package aurum.aurum.init;

import aurum.aurum.worldgen.ModAurumConfiguredFeatures;
import aurum.aurum.worldgen.ModAurumPlacedFeatures;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.core.RegistrySetBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static aurum.aurum.Aurum.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(),
                new ModDamageTagsProvider(generator.getPackOutput(), lookupProvider, existingFileHelper));

        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModAurumConfiguredFeatures::bootstrap)
                .add(Registries.PLACED_FEATURE, ModAurumPlacedFeatures::bootstrap)
                .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);

        generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, builder, Set.of(MODID)));

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider));

        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));

        ModBlockTagProvider blockTags = generator.addProvider(event.includeServer(),
                new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(packOutput, lookupProvider, blockTags.contentsGetter(), existingFileHelper));



    }
}