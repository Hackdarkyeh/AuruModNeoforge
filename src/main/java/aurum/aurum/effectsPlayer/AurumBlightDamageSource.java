package aurum.aurum.effectsPlayer;

import aurum.aurum.init.ModDamageTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.*;


import net.minecraft.core.Holder;


public class AurumBlightDamageSource extends DamageSource {

    public AurumBlightDamageSource(Holder<DamageType> p_270475_) {
        super(p_270475_);
    }



    public static DamageSource createAurumBlightDamageSource(RegistryAccess registryAccess) {
        Holder<DamageType> aurumBlightType = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.AURUM_BLIGHT_DAMAGE);
        return new DamageSource(aurumBlightType);
    }


}
