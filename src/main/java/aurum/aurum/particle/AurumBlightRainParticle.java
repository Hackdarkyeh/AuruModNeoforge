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
    // Estos valores son una aproximación basada en el comportamiento de la lluvia vanilla.
    // Es posible que necesites pequeños ajustes finos.
    private static final float GRAVITY = 1.0F; // La lluvia de Minecraft cae bastante rápido. Un valor más alto simula más gravedad.
    private static final float BASE_SIZE = 0.15F; // Las gotas de lluvia vanilla son pequeñas.
    private static final int BASE_LIFETIME = 15; // Vida útil relativamente corta para que desaparezcan al tocar el suelo o antes.
    private static final float FADE_START = 0.7F; // Empieza a desvanecerse un poco antes para un efecto más natural.

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
        this.friction = 0.98F; // Mantén este valor, es un buen estándar.

        // Apariencia
        this.quadSize = BASE_SIZE * (0.9F + random.nextFloat() * 0.2F); // Variación de tamaño mínima
        this.lifetime = (int)(BASE_LIFETIME * (0.9 + random.nextDouble() * 0.2)); // Variación de vida útil mínima
        this.setSpriteFromAge(sprites);

        // Movimiento inicial (ajustado para una caída más directa y rápida)
        this.xd = xd * 0.05; // Movimiento lateral muy reducido, casi vertical
        this.yd = yd * 2.0; // Acelera la caída para que parezca más una gota de lluvia
        this.zd = zd * 0.05; // Movimiento lateral muy reducido, casi vertical
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

        // Verificar colisión con el suelo (para que desaparezcan como las vanilla)
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        // Las partículas de lluvia vanilla no atraviesan bloques sólidos
        if (this.onGround || this.level.getBlockState(pos).isSolid() || this.level.getFluidState(pos).isSource()) {
            this.remove(); // Elimina la partícula al tocar el suelo o un bloque sólido/fuente de fluido
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        // Usa este tipo de renderizado, es común para partículas con sprites transparentes
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}