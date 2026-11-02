package aurum.aurum.client.renderer;

import aurum.aurum.entity.AurumVillager.AurumVillagerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

import static aurum.aurum.Aurum.MODID;

/**
 * Renderer del aldeano de Aurum. Usa el modelo humanoide vanilla (capa PLAYER, textura 64x64)
 * para no registrar capas nuevas, y añade la capa de armadura para que lleve la armadura del mod.
 * Más adelante se puede cambiar por un modelo de Blockbench.
 */
public class AurumVillagerRenderer extends HumanoidMobRenderer<AurumVillagerEntity, HumanoidModel<AurumVillagerEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entities/aurum_villager.png");

    public AurumVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);

        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(AurumVillagerEntity entity) {
        return TEXTURE;
    }
}