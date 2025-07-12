package aurum.aurum.entity.SoreBoss;

import aurum.aurum.entity.ai.SoreGoals.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class SoreBossEntity extends Monster implements NeutralMob {
    // Sistema de ira
    private int angerLevel;
    private UUID targetUuid;

    // BossBar
    private final ServerBossEvent bossBar;
    public final AnimationState invokeStart = new AnimationState();
    public final AnimationState invokeEnd = new AnimationState();
    public final AnimationState invoke = new AnimationState();


    // Fases del boss (0-100%)
    private int currentPhase = 1;
    private static final int PHASE_CHANGE_HP = 250; // Cambia de fase cada 250 HP

    public int getCurrentPhase() {
        return currentPhase;
    }
    public SoreBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.bossBar = (ServerBossEvent) new ServerBossEvent(
                Component.literal("Sore"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        ).setDarkenScreen(true);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        updatePhase();
    }

    private void updatePhase() {
        int newPhase = 4 - (int)(this.getHealth() / PHASE_CHANGE_HP);
        newPhase = Mth.clamp(newPhase, 1, 4);

        if (newPhase != currentPhase) {
            currentPhase = newPhase;
            onPhaseChange(currentPhase);
        }
    }

    protected void onPhaseChange(int newPhase) {

        switch (newPhase) {
            case 1 -> bossBar.setName(Component.literal("Sore - Fase Inicial").withStyle(ChatFormatting.GREEN));
            case 2 -> bossBar.setName(Component.literal("Sore - Fase Defensiva").withStyle(ChatFormatting.YELLOW));
            case 3 -> bossBar.setName(Component.literal("Sore - Fase Agresiva").withStyle(ChatFormatting.RED));
            case 4-> bossBar.setName(Component.literal("Sore").withStyle(ChatFormatting.DARK_RED));
        }
    }

    // Implementación de NeutralMob (similar a Enderman)
    @Override public int getRemainingPersistentAngerTime() { return angerLevel; }
    @Override public void setRemainingPersistentAngerTime(int time) { angerLevel = time; }
    @Override public UUID getPersistentAngerTarget() { return targetUuid; }
    @Override public void setPersistentAngerTarget(UUID target) { targetUuid = target; }
    @Override public void startPersistentAngerTimer() { angerLevel = 400; } // 20 segundos

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 25.0)
                .add(Attributes.ARMOR, 35.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    public float getPhaseDamageMultiplier() {
        return switch (this.currentPhase) {
            case 1 -> 1.0f; // Fase final: 200% de daño
            case 2 -> 1.5f; // Fase agresiva: 150%
            case 3 -> 1.2f; // Fase defensiva: 120%
            case 4 -> 2.0f; // Fase inicial: 100%
            default -> 1.0f;
        };
    }

    @Override
    public void registerGoals() {
        // Comportamientos específicos por fase
        this.goalSelector.addGoal(3, new SorePhase1Goal(this));
        this.goalSelector.addGoal(3, new SorePhase2Goal(this));
        this.goalSelector.addGoal(3, new SorePhase3Goal(this));
        this.goalSelector.addGoal(3, new SorePhase4Goal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new RetaliateAgainstAttackersGoal(this));
    }
}
