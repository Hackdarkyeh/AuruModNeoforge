package aurum.aurum.client.renderer;

import aurum.aurum.Aurum;
import aurum.aurum.entity.SkeletonAltered.AurumArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AurumArrowRenderer extends ArrowRenderer<AurumArrow> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "textures/entities/projectiles/aurum_arrow.png");

    public AurumArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(AurumArrow entity) {
        return TEXTURE;
    }
}