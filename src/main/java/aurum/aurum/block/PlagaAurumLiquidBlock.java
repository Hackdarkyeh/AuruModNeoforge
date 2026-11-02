package aurum.aurum.block;

import aurum.aurum.init.ModEffects;
import aurum.aurum.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PlagaAurumLiquidBlock extends LiquidBlock {

    private static final int AMPLIFIER = 4;            // nivel V
    private static final int BASE_DURATION = 100;      // 5 s al entrar
    private static final int MAX_DURATION = 12000;     // tope: 10 min
    private static final int INTERVAL = 5;             // se actualiza cada 5 ticks
    private static final int GAIN = 2;                 // cada segundo dentro suma 2 s de efecto
    private static final String KEY = "AurumBlightLastTick";

    public PlagaAurumLiquidBlock() {
        super(ModFluids.AURUM_ROSA.value(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;

        // Una sola actualización por intervalo, aunque la hitbox toque varios bloques de fluido
        CompoundTag data = living.getPersistentData();
        int diff = living.tickCount - data.getInt(KEY);
        if (diff >= 0 && diff < INTERVAL) return;
        data.putInt(KEY, living.tickCount);

        MobEffectInstance current = living.getEffect(ModEffects.AurumBlightEffect);
        int duration = current == null
                ? BASE_DURATION
                : Math.min(current.getDuration() + INTERVAL * (1 + GAIN), MAX_DURATION);

        living.addEffect(new MobEffectInstance(ModEffects.AurumBlightEffect, duration, AMPLIFIER, false, true, true));
    }
}