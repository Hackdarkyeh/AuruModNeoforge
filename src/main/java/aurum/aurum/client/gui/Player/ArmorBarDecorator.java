package aurum.aurum.client;

import aurum.aurum.Aurum;
import aurum.aurum.item.ArmorItem.AurumArmor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/** Barra de energía sobre el icono de cualquier armadura del mod. Se oculta con la energía al máximo. */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ArmorBarDecorator {

    @SubscribeEvent
    public static void onRegister(RegisterItemDecorationsEvent event) {
        IItemDecorator energyBar = (g, font, stack, x, y) -> {
            if (!AurumArmor.is(stack)) return false;
            int max = AurumArmor.getMaxEnergy(stack);
            int cur = AurumArmor.getEnergy(stack);
            if (max <= 0 || cur >= max) return false; // con la energía al máximo no se dibuja

            int w = Math.round(13F * cur / max);
            int color = switch (AurumArmor.getType(stack)) {
                case DARK_ENERGY -> 0x6600CC;
                case CLEAN_ENERGY -> 0x33FF99;
                default -> 0x9933FF;
            };
            g.fill(RenderType.guiOverlay(), x + 2, y + 13, x + 15, y + 15, 0xFF000000);
            g.fill(RenderType.guiOverlay(), x + 2, y + 13, x + 2 + w, y + 14, 0xFF000000 | color);
            return true;
        };
        BuiltInRegistries.ITEM.stream().filter(i -> i instanceof ArmorItem).forEach(i -> event.register(i, energyBar));
    }
}