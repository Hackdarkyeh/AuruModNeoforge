package aurum.aurum.client.renderer;

import aurum.aurum.client.model.AAModModelLayers;
import aurum.aurum.client.model.BlazeCorruptedModel;
import aurum.aurum.entity.BlazeCorrupted.BlazeCorruptedEntity;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Blaze;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static aurum.aurum.Aurum.MODID;

@OnlyIn(Dist.CLIENT)
public class BlazeCorruptedRenderer extends MobRenderer<BlazeCorruptedEntity, BlazeCorruptedModel<BlazeCorruptedEntity>> {
    private static final ResourceLocation BLAZE_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID,"textures/entities/blaze_corrupted.png");

    public BlazeCorruptedRenderer(EntityRendererProvider.Context p_173933_) {
        super(p_173933_, new BlazeCorruptedModel<>(p_173933_.bakeLayer(AAModModelLayers.BLAZE_CORRUPTED_LAYER)), 0.5F);
    }

    protected int getBlockLightLevel(BlazeCorruptedEntity entity, BlockPos pos) {
        return 15;
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(BlazeCorruptedEntity entity) {
        return BLAZE_LOCATION;
    }
}
