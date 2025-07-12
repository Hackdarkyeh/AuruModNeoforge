package aurum.aurum.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class AurumBlightRainParticle extends TextureSheetParticle {
    // Configuración ajustable
    private static final float GRAVITY = 0.5F;
    private static final float BASE_SIZE = 0.2F;
    private static final int BASE_LIFETIME = 20;
    private static final float FADE_START = 0.8F; // 80% del lifetime

    private final SpriteSet sprites;

    public static AurumBlightRainParticleProvider provider(SpriteSet spriteSet) {
        return new AurumBlightRainParticleProvider(spriteSet);
    }

    public static class AurumBlightRainParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public AurumBlightRainParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AurumBlightRainParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }

    protected AurumBlightRainParticle(ClientLevel level, double x, double y, double z,
                                      double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;

        // Configuración física
        this.gravity = GRAVITY;
        this.friction = 0.98F;

        // Apariencia
        this.quadSize = BASE_SIZE * (0.8F + random.nextFloat() * 0.4F); // Variación de tamaño
        this.lifetime = (int)(BASE_LIFETIME * (0.8 + random.nextDouble() * 0.4));
        this.setSpriteFromAge(sprites);

        // Movimiento inicial
        this.xd = xd * 0.1;
        this.yd = yd * 1.5; // Acelera la caída
        this.zd = zd * 0.1;
    }

    @Override
    public void tick() {
        super.tick();

        // Actualizar sprite según edad
        this.setSpriteFromAge(sprites);

        // Efecto de desvanecimiento
        if (this.age >= this.lifetime * FADE_START) {
            this.alpha = 1.0F - (float)(this.age - this.lifetime * FADE_START) / (float)(this.lifetime * (1.0F - FADE_START));
        }

        // Verificar colisión con el suelo
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.onGround || this.level.getBlockState(pos).isSolid()) {
            this.remove();
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}