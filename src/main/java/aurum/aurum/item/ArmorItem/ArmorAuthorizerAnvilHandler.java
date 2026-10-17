package aurum.aurum.item.ArmorItem;


import aurum.aurum.init.ModDataComponents;
import aurum.aurum.init.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;


// ── EN PurifierItem.java (o el item especial que crearás) ─────────────────────
// Al usar el item sobre una armadura en el inventario o con clic derecho,
// añade el DataComponent ARMOR_AUTHORIZED = true

// Ejemplo con AnvilUpdateEvent (el jugador pone armadura + tu item en yunque):

// En una clase separada, ej: ArmorAuthorizerAnvilHandler.java
@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class ArmorAuthorizerAnvilHandler {

    // El item que autoriza armaduras — cámbialo por el que vayas a crear
    // Por ahora usamos PURIFIER como placeholder
    private static boolean isAuthorizerItem(ItemStack stack) {
        return stack.getItem() == ModItems.PURIFIER.get();
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left  = event.getLeft();   // la armadura
        ItemStack right = event.getRight();  // el item autorizador

        // Solo si el item de la derecha es nuestro autorizador
        if (!isAuthorizerItem(right)) return;

        // Solo si el de la izquierda es una armadura (ArmorItem)
        if (!(left.getItem() instanceof net.minecraft.world.item.ArmorItem)) return;

        // Ya está autorizada — no hacer nada
        if (left.has(ModDataComponents.ARMOR_AUTHORIZED.get())) return;

        // Creamos el output: la misma armadura con el DataComponent añadido
        ItemStack output = left.copy();
        output.set(ModDataComponents.ARMOR_AUTHORIZED.get(), true);

        event.setOutput(output);
        event.setCost(5);           // coste en niveles de experiencia
        event.setMaterialCost(1);   // consume 1 del item autorizador
    }
}
