package aurum.aurum.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class AurumDimensionEffects extends DimensionSpecialEffects {

    public AurumDimensionEffects() {
        // cloudLevel, forceBrightLightmap, skyType, forceDarkLightmap, constantAmbientLight
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float daylight) {
        // Morado/magenta fijo, sin importar la hora ni el brillo ambiente
        return new Vec3(0.35, 0.05, 0.45);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return true; // niebla siempre activa, como en el Nether
    }
}
