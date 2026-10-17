package aurum.aurum.client.renderer;

import aurum.aurum.client.model.SkeletonAlteredModel;
import aurum.aurum.entity.SkeletonAltered.AbstractAlteredSkeleton;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static aurum.aurum.Aurum.MODID;


@OnlyIn(Dist.CLIENT)
public class SkeletonAlteredRenderer<T extends AbstractAlteredSkeleton> extends HumanoidMobRenderer<T, SkeletonAlteredModel<T>> {
    private static final ResourceLocation SKELETON_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID,"textures/entities/skeleton_altered.png");

    public SkeletonAlteredRenderer(EntityRendererProvider.Context p_174380_) {
        this(p_174380_, ModelLayers.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
    }

    public SkeletonAlteredRenderer(EntityRendererProvider.Context context, ModelLayerLocation skeletonLayer, ModelLayerLocation innerModelLayer, ModelLayerLocation outerModelLayer) {
        this(context, innerModelLayer, outerModelLayer, new SkeletonAlteredModel<>(context.bakeLayer(skeletonLayer)));
    }

    public SkeletonAlteredRenderer(EntityRendererProvider.Context context, ModelLayerLocation skeletonLayer, ModelLayerLocation innerModelLayer, SkeletonAlteredModel<T> model) {
        super(context, model, 0.5F);
        this.addLayer(
                new HumanoidArmorLayer<>(
                        this, new SkeletonAlteredModel<>(context.bakeLayer(skeletonLayer)), new SkeletonAlteredModel<>(context.bakeLayer(innerModelLayer)), context.getModelManager()
                )
        );
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(T entity) {
        return SKELETON_LOCATION;
    }

    protected boolean isShaking(T entity) {
        return entity.isShaking();
    }
}

