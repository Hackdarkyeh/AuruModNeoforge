
package aurum.aurum.entity.SpiderCorrupted;

import aurum.aurum.Aurum;
import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.entity.CooperGolem.ExtinguishFireGoal;
import aurum.aurum.init.ModAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public class SpiderCorruptedEntity extends Monster implements ICorruptedMob {
	@Override public LivingEntity self() { return this; }

	private static final ResourceLocation AURUM_HEALTH_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_health_bonus");
	private static final ResourceLocation AURUM_DAMAGE_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_damage_bonus");

	private int remainingPersistentAngerTime;
	private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
	@javax.annotation.Nullable
	private UUID persistentAngerTarget;

	public final AnimationState idleAnimationState = new AnimationState();
	private int idleAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> ATTACK_ANIMATION = SynchedEntityData.defineId(SpiderCorruptedEntity.class, EntityDataSerializers.BOOLEAN);

	public final AnimationState attackAnimationState = new AnimationState();
	public int attackAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> CHANNEL_RAYS_ANIMATION = SynchedEntityData.defineId(SpiderCorruptedEntity.class, EntityDataSerializers.BOOLEAN);


	public SpiderCorruptedEntity(EntityType<? extends Monster> type, Level world) {
		super(type, world);
        xpReward = 0;
		setNoAi(false);
	}

	@Override
	public void tick() {
		super.tick();

		if(this.level().isClientSide()){
			setupAnimationStates();
		}
	}

	private void setupAnimationStates(){
		if (this.idleAnimationTimeOut <= 0){
			this.idleAnimationTimeOut = this.random.nextInt(40) + 80;
			this.idleAnimationState.start(this.tickCount);
		}else{
			--this.idleAnimationTimeOut;
		}
		// detectar canSeeSky hace que no fufe






		if (this.isAttacking() && this.attackAnimationTimeOut <= 0) {
			this.attackAnimationTimeOut = 12;
			this.attackAnimationState.start(this.tickCount);
		}else{
			--this.attackAnimationTimeOut;
		}

		if (!this.isAttacking()) {
			this.attackAnimationState.stop();
		}
	}




	@Override
	protected void updateWalkAnimation(float pPartialTick) {
		float f;
		if (this.getPose() == Pose.STANDING){
			f = Math.min(pPartialTick * 6F, 1.0F);
		}else{
			f = 0f;
		}
		this.walkAnimation.update(f, 0.2f);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.@NotNull Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(ATTACK_ANIMATION, false);
		entityData.define(CHANNEL_RAYS_ANIMATION, false);
	}

	public void setAttacking(boolean attacking){
		this.entityData.set(ATTACK_ANIMATION, attacking);
	}
	public void setChannelRays(boolean channelRays){
		this.entityData.set(CHANNEL_RAYS_ANIMATION, channelRays);
	}

	public boolean isAttacking(){
		return this.entityData.get(ATTACK_ANIMATION);
	}


	@Override
	protected void registerGoals() {
		/*
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {

			@Override
			protected int getAttackInterval() {
				return super.getAttackInterval();
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 3.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		*/
		this.goalSelector.addGoal(6, new TemptGoal(this, 1.5D, Ingredient.of(Blocks.COPPER_BLOCK), false));
		this.goalSelector.addGoal(1, new SpiderCorruptedAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new ExtinguishFireGoal(this));
		this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
		this.goalSelector.addGoal(2, new MoveBackToVillageGoal(this, 0.6, false));
		this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6));
		//this.goalSelector.addGoal(5, new OfferFlowerGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		//this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public void aiStep() {
		super.aiStep();
	}


	@Override
	protected void dropCustomDeathLoot(ServerLevel p_345102_, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(p_345102_, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(Blocks.COPPER_BLOCK));
	}



	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.LIGHTNING_BOLT))
			return false;
		return super.hurt(damagesource, amount);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(ModAttributes.AURUM_IN_BLOOD, 1.0);
	}


	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
										MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
		spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
		rollAurumBlood();
		return spawnData;
	}

	private void rollAurumBlood() {
		float aurum = 1.0F + this.random.nextFloat() * 99.0F; // rango 1.0 - 100.0
		this.getAttribute(ModAttributes.AURUM_IN_BLOOD).setBaseValue(aurum);

		// Escala otras stats en función del aurum (ejemplo: vida y daño)
		this.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
				new AttributeModifier(AURUM_HEALTH_ID, (aurum - 1.0) * 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		this.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
				new AttributeModifier(AURUM_DAMAGE_ID, (aurum - 1.0) * 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

		this.setHealth(this.getMaxHealth()); // full HP tras aplicar el bonus
	}




}
