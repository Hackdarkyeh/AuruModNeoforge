package aurum.aurum;

import aurum.aurum.block.ArmorSystem.SoulModificationTable.SoulModificationTableBlockEntityRenderer;
import aurum.aurum.block.engineering.PedestalBlock.PedestalBlockEntityRenderer;
import aurum.aurum.block.engineering.PortalAurum.AurumPortalInside.AurumPortalBlockRenderer;
import aurum.aurum.block.engineering.PortalAurum.InoculatedObsidianBER;
import aurum.aurum.client.AurumDimensionEffects;
import aurum.aurum.client.renderer.*;
import aurum.aurum.init.ModBlockEntities;
import aurum.aurum.init.ModEntities;
import aurum.aurum.item.pickage.AurelitePickaxeHighlight;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import static aurum.aurum.init.ModBlocks.ENERGY_STORAGE_BLOCK;

// Segunda entrada @Mod con el mismo modid, que solo se instancia en el cliente
@Mod(value = Aurum.MODID, dist = Dist.CLIENT)
public class AurumClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public AurumClient(IEventBus modEventBus) {
        modEventBus.addListener(AurumClient::onClientSetup);
        modEventBus.addListener(AurumClient::registerBER);
        modEventBus.addListener(AurumClient::registerEffects);

        // Antes estaba en Aurum.java y rompía el servidor dedicado
        NeoForge.EVENT_BUS.register(AurelitePickaxeHighlight.class);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("NOMBRE DE MINECRAFT >> {}", Minecraft.getInstance().getUser().getName());
        LOGGER.info("Pack de recursos activos DE MINECRAFT >> {}", Minecraft.getInstance().getDownloadedPackSource());

        EntityRenderers.register(ModEntities.COOPER_GOLEM.get(), CooperGolemRenderer::new);
        EntityRenderers.register(ModEntities.SORE_BOSS.get(), SoreRenderer::new);
        EntityRenderers.register(ModEntities.ZOMBIE_ALTERED.get(), ZombieAlteredRender::new);
        EntityRenderers.register(ModEntities.SPIDER_CORRUPTED.get(), SpiderAlteredRender::new);
        EntityRenderers.register(ModEntities.SKELETON_ALTERED.get(), SkeletonAlteredRenderer::new);
        EntityRenderers.register(ModEntities.DOLPHIN_ALTERED.get(), DolphinAlteredRenderer::new);
        EntityRenderers.register(ModEntities.ENDER_DRAGON_ALTERED.get(), EnderDragonAlteredRenderer::new);
        EntityRenderers.register(ModEntities.VEX_ALTERED.get(), VexAlteredRenderer::new);
        EntityRenderers.register(ModEntities.DIVINE_ARMOR.get(), DivineArmorRenderer::new);
        EntityRenderers.register(ModEntities.BLAZE_CORRUPTED.get(), BlazeCorruptedRenderer::new);
        EntityRenderers.register(ModEntities.AURUM_VILLAGER.get(), AurumVillagerRenderer::new);
        EntityRenderers.register(ModEntities.CORRUPTED_FIREBALL.get(),
                ctx -> new ThrownItemRenderer<>(ctx, 0.75F, true));

        ItemBlockRenderTypes.setRenderLayer(ENERGY_STORAGE_BLOCK.get(), RenderType.translucent());
        EntityRenderers.register(ModEntities.EVOKER_ALTERED.get(), EvokerAlteredRenderer::new);
        EntityRenderers.register(ModEntities.EVOKER_FANGS_ALTERED.get(), EvokerFangsAlteredRenderer::new);
    }

    private static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOUL_MODIFICATION_TABLE_BLOCK_ENTITY.get(), SoulModificationTableBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INOCULATED_OBSIDIAN_BLOCK_ENTITY.get(), InoculatedObsidianBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AURUM_PORTAL_BLOCK_ENTITY.get(), AurumPortalBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENERGY_STORAGE_BLOCK.get(), EnergyStorageBlockEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AURUM_ARROW.get(), AurumArrowRenderer::new);

    }

    private static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "dimension_1"), new AurumDimensionEffects());
    }
}