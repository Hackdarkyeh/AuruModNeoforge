package aurum.aurum.particle;


import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class AurumBlightRainParticle extends TextureSheetParticle {

    public static AurumBlightRainParticleProvider provider(SpriteSet spriteSet) {
        return new AurumBlightRainParticleProvider(spriteSet);
    }

    public static class AurumBlightRainParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public AurumBlightRainParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new AurumBlightRainParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;


    protected AurumBlightRainParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.gravity = 30.0F; // Gravedad aplicada a la partícula
        this.friction = 0.2F; // Fricción que afecta el movimiento
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize *= 1.5F; // Tamaño de la partícula
        this.lifetime = 360; // Duración de la partícula
        this.setSize(0.01F, 0.01F); // Tamaño de la partícula
        this.pickSprite(spriteSet);
        this.spriteSet = spriteSet;
    }

    @Override
    public void tick() {
        this.setSpriteFromAge(spriteSet);
        super.tick();
        int groundHeight = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int) x, 0, (int) z)).getY();


        if (y <= groundHeight) {
            this.remove(); // Remover la partícula cuando su vida termine
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE; // Tipo de renderizado
    }

}



