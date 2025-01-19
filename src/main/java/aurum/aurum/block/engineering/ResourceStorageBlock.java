package aurum.aurum.block.engineering;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public class ResourceStorageBlock extends Block {
    // Utilizaremos un contenedor simple para almacenar los recursos
    private final SimpleContainer inventory = new SimpleContainer(64); // Hasta 64 stacks de recursos

    public ResourceStorageBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f));
    }

    // Método para añadir un recurso al almacenamiento
    public void addResource(ItemStack stack) {
        inventory.addItem(stack);
    }

    // Método para obtener los recursos almacenados
    public SimpleContainer getInventory() {
        return inventory;
    }
}

