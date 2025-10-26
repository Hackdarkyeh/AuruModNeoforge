
package aurum.aurum.entity;

import aurum.aurum.energy.EnergyStorage;
import aurum.aurum.entity.ai.CooperGolemAttackGoal;
import aurum.aurum.entity.ai.ExtinguishFireGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public class CooperGolemEntity extends AbstractGolem implements NeutralMob {

	private final EnergyStorage energyStorage;
	private boolean shouldChannelLightning;

	private int remainingPersistentAngerTime;
	private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
	@javax.annotation.Nullable
	private UUID persistentAngerTarget;

	public final AnimationState idleAnimationState = new AnimationState();
	private int idleAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> ATTACK_ANIMATION = SynchedEntityData.defineId(CooperGolemEntity.class, EntityDataSerializers.BOOLEAN);

	public final AnimationState attackAnimationState = new AnimationState();
	public int attackAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> CHANNEL_RAYS_ANIMATION = SynchedEntityData.defineId(CooperGolemEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState channelraysAnimationState = new AnimationState();
	public int channelraysAnimationTimeOut = 0;


	public CooperGolemEntity(EntityType<? extends AbstractGolem> type, Level world) {
		super(type, world);
        this.energyStorage = new EnergyStorage(1000, 0,0,0);
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
        shouldChannelLightning = isEntityExposedToThundering(this);
		// detectar canSeeSky hace que no fufe
		if (shouldChannelLightning && this.channelraysAnimationTimeOut <= 0) {
			this.channelraysAnimationTimeOut = 24;
			this.channelraysAnimationState.start(this.tickCount);
			setChannelRays(true);
		}else{
			--this.channelraysAnimationTimeOut;
		}



		if (!this.level().isThundering() || !this.level().canSeeSky(this.blockPosition())) {
			this.channelraysAnimationState.stop();
			setChannelRays(false);
		}


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

	/**
	 * Verifica si una entidad está expuesta a la lluvia.
	 * @param entity La entidad a verificar.
	 * @return true si la entidad está expuesta a la lluvia, false en caso contrario.
	 */
	public static boolean isEntityExposedToThundering(Entity entity) {
		Level world = entity.level();
		BlockPos pos = entity.blockPosition();

		// Verifica si el clima en el mundo está lloviendo
		if (world.isThundering()) {
			// Verifica si la entidad está en un bioma donde la lluvia es posible
			if (!world.isRainingAt(pos)) {
				return false;
			}

			// Verifica si hay un bloque sólido arriba de la entidad
			BlockPos abovePos = pos.above();
			while (world.isEmptyBlock(abovePos) && abovePos.getY() < world.getMaxBuildHeight()) {
				abovePos = abovePos.above();
			}
			// Si no hay bloques sólidos sobre la entidad, entonces está expuesta a la lluvia
			return world.canSeeSky(pos) && world.isRainingAt(pos);
		}

		return false;
	}

	private void trySummonLightning() {
		// Definir la probabilidad de que caiga un rayo (en este caso, 100%)
		double lightningProbability = 0.01;

		// Comprobar si la probabilidad se cumple y si se ejecuta en el servidor
		if (!this.level().isClientSide && this.random.nextDouble() < lightningProbability) {

			BlockPos pos = this.blockPosition();
			LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(this.level());

			if (lightningBolt != null) {
				// Mover el rayo a la posición deseada
				lightningBolt.moveTo(pos.getX(), pos.getY(), pos.getZ());

				// Añadir la entidad rayo al nivel (mundo)
				this.level().addFreshEntity(lightningBolt);

				System.out.println("Rayo invocado en: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			} else {
				System.out.println("El rayo no pudo ser creado.");
			}
		} else {
			System.out.println("El rayo no fue invocado debido a que la probabilidad no se cumplió o no estaba en el lado del servidor.");
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
		this.goalSelector.addGoal(1, new CooperGolemAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new ExtinguishFireGoal(this));
		this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
		this.goalSelector.addGoal(2, new MoveBackToVillageGoal(this, 0.6, false));
		this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6));
		//this.goalSelector.addGoal(5, new OfferFlowerGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		//this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
		this.targetSelector
				.addGoal(
						3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, p_28879_ -> p_28879_ instanceof Enemy && !(p_28879_ instanceof Creeper))
				);
		//this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));

	}

	@Override
	public void aiStep() {
		super.aiStep();


		/*if (this.offerFlowerTick > 0) {
			this.offerFlowerTick--;
		}*/
		// Invocar un rayo con cierta probabilidad si la animación está activa
		if (isEntityExposedToThundering(this)) {
			trySummonLightning();
		}

		if (!this.level().isClientSide) {
			this.updatePersistentAnger((ServerLevel)this.level(), true);
		}
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
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 15);
		builder = builder.add(Attributes.ARMOR, 5);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.5);
		return builder;

				//.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public int getRemainingPersistentAngerTime() {
		return this.remainingPersistentAngerTime;
	}


	@Override
	public void setRemainingPersistentAngerTime(int p_28859_) {
		this.remainingPersistentAngerTime = p_28859_;
	}

	@Nullable
	@Override
	public UUID getPersistentAngerTarget() {
		return this.persistentAngerTarget;
	}


	@Override
	public void setPersistentAngerTarget(@javax.annotation.Nullable UUID p_28855_) {
		this.persistentAngerTarget = p_28855_;
	}

	@Override
	public void startPersistentAngerTimer() {
		this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
	}

	@Override
	public void thunderHit(ServerLevel serverLevel, LightningBolt lightningBolt) {
		super.thunderHit(serverLevel, lightningBolt);

		// Código personalizado que quieres ejecutar cuando el golem es impactado por un rayo.
		// Por ejemplo, incrementar energía o iniciar otra animación:

		// Ejemplo: Aumentar la energía del golem cuando es impactado por un rayo
		this.energyStorage.addEnergy(100, false);


		// También podrías causar daño, cambiar atributos, etc.
	}

	// Método para obtener el nivel de energía actual como porcentaje
	public int getEnergyPercentage() {
		return (int) ((this.energyStorage.getEnergyStored() / (double) this.energyStorage.getCapacity()) * 100);
	}

	// Método para comprobar si está canalizando rayos
	public boolean isChanneling() {
		return isEntityExposedToThundering(this);
	}



	}
