package aurum.aurum.client.renderer;

import aurum.aurum.Aurum;
import aurum.aurum.client.model.AAModModelLayers;
import aurum.aurum.client.model.VexAlteredModel;
import aurum.aurum.entity.VexAltered.VexAlteredEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;


public class VexAlteredRenderer extends MobRenderer<VexAlteredEntity, VexAlteredModel> {
    private static final ResourceLocation VEX_LOCATION = ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/entities/vex/vex_altered.png");
    private static final ResourceLocation VEX_CHARGING_LOCATION = ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/entities/vex/vex_altered_angry.png");

    public VexAlteredRenderer(EntityRendererProvider.Context p_174435_) {
        super(p_174435_, new VexAlteredModel(p_174435_.bakeLayer(AAModModelLayers.VEX_ALTERED_LAYER)), 0.3F);
        this.addLayer(new ItemInHandLayer<>(this, p_174435_.getItemInHandRenderer()));
    }

    protected int getBlockLightLevel(VexAlteredEntity entity, BlockPos pos) {
        return 15;
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(VexAlteredEntity entity) {
        return entity.isCharging() ? VEX_CHARGING_LOCATION : VEX_LOCATION;
    }
}