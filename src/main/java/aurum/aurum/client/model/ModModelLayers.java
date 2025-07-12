package aurum.aurum.client.model;

import aurum.aurum.Aurum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {
    public static  final ModelLayerLocation COOPER_GOLEM_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "cooper_golem_layer"),
            "main"
    );
    public static final ModelLayerLocation SORE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "sore_layer"),
            "main"
    );
}
