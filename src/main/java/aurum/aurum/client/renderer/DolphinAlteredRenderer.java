package aurum.aurum.client.renderer;

import aurum.aurum.client.model.DolphinAlteredModel;
import aurum.aurum.entity.DolphinAltered.DolphinAlteredCarryingItemLayer;
import aurum.aurum.entity.DolphinAltered.DolphinAlteredEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static aurum.aurum.Aurum.MODID;

public class DolphinAlteredRenderer extends MobRenderer<DolphinAlteredEntity, DolphinAlteredModel<DolphinAlteredEntity>> {
    private static final ResourceLocation DOLPHIN_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID,"textures/entities/dolphin_altered.png");

    public DolphinAlteredRenderer(EntityRendererProvider.Context p_173960_) {
        super(p_173960_, new DolphinAlteredModel<>(p_173960_.bakeLayer(ModelLayers.DOLPHIN)), 0.7F);
        this.addLayer(new DolphinAlteredCarryingItemLayer(this, p_173960_.getItemInHandRenderer()));
    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public ResourceLocation getTextureLocation(DolphinAlteredEntity entity) {
        return DOLPHIN_LOCATION;
    }
}

