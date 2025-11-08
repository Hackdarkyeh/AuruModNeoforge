package aurum.aurum.init.GUI;

import aurum.aurum.client.gui.SoulModificationTableMenu.SoulModificationTableScreen;
import aurum.aurum.client.gui.ArmorTable.ArmorTableScreen;
import aurum.aurum.client.gui.DarkEnergyTable.DarkEnergyTableScreen;
import aurum.aurum.client.gui.EnergyGeneratorBlock.EnergyGeneratorScreen;
import aurum.aurum.client.gui.ExtractorBlock.ExtractorScreen;
import aurum.aurum.client.gui.Pedestal.PedestalScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static aurum.aurum.Aurum.MODID;
import static aurum.aurum.init.GUI.ModMenuType.ENERGY_GENERATOR_MENU;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = MODID)
public class ModScreens {
    @SubscribeEvent
    private static  void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ENERGY_GENERATOR_MENU.get(), EnergyGeneratorScreen::new);
        event.register(ModMenuType.EXTRACTOR_MENU.get(), ExtractorScreen::new);
        event.register(ModMenuType.ARMOR_TABLE_MENU.get(), ArmorTableScreen::new);
        event.register(ModMenuType.PEDESTAL_MENU.get(), PedestalScreen::new);
        event.register(ModMenuType.SOUL_MODIFICATION_TABLE.get(), SoulModificationTableScreen::new);
        event.register(ModMenuType.DARK_ENERGY_TABLE.get(), DarkEnergyTableScreen::new);
    }
}
