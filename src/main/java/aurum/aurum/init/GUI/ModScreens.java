package aurum.aurum.init.GUI;

import aurum.aurum.client.gui.EnergyGeneratorBlock.EnergyGeneratorScreen;
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
    }
}
