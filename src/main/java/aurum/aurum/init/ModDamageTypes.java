package aurum.aurum.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.*;


import static aurum.aurum.Aurum.MODID;

public class ModDamageTypes {



    public static final ResourceKey<DamageType> AURUM_BLIGHT_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "aurum_blight_damage"));



}
