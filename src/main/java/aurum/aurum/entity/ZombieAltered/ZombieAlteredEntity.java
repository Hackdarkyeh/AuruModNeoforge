package aurum.aurum.entity.ZombieAltered;

import aurum.aurum.Aurum;
import aurum.aurum.entity.ICorruptedMob;
import aurum.aurum.entity.CooperGolem.ExtinguishFireGoal;
import aurum.aurum.init.ModAttributes;
import aurum.aurum.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ZombieAlteredEntity extends Monster implements ICorruptedMob {
	@Override public LivingEntity self() { return this; }

	// ===== Aurum en sangre =====
	private static final ResourceLocation AURUM_HEALTH_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_health_bonus_zombie");
	private static final ResourceLocation AURUM_DAMAGE_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "aurum_damage_bonus_zombie");

	private int remainingPersistentAngerTime;
	private static final net.minecraft.util.valueproviders.UniformInt PERSISTENT_ANGER_TIME =
			net.minecraft.util.TimeUtil.rangeOfSeconds(20, 39);
	@Nullable
	private UUID persistentAngerTarget;

	public final AnimationState idleAnimationState = new AnimationState();
	private int idleAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> ATTACK_ANIMATION =
			SynchedEntityData.defineId(ZombieAlteredEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState attackAnimationState = new AnimationState();
	public int attackAnimationTimeOut = 0;

	private static final EntityDataAccessor<Boolean> CHANNEL_RAYS_ANIMATION =
			SynchedEntityData.defineId(ZombieAlteredEntity.class, EntityDataSerializers.BOOLEAN);

	// ===== Sistema baby (completo, con dimensiones e isBaby real) =====
	private static final ResourceLocation SPEED_MODIFIER_BABY_ID =
			ResourceLocation.withDefaultNamespace("baby_zombie_altered_speed");
	private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(
			SPEED_MODIFIER_BABY_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
	);
	private static final EntityDataAccessor<Boolean> DATA_BABY_ID =
			SynchedEntityData.defineId(ZombieAlteredEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDimensions BABY_DIMENSIONS =
			EntityType.ZOMBIE.getDimensions().scale(0.5F).withEyeHeight(0.93F);

	// ===== Refuerzos (llamadas de ayuda) =====
	private static final ResourceLocation REINFORCEMENT_CALLER_CHARGE_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "reinforcement_caller_charge");
	private static final AttributeModifier ZOMBIE_REINFORCEMENT_CALLEE_CHARGE = new AttributeModifier(
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "reinforcement_callee_charge"),
			-0.05F, AttributeModifier.Operation.ADD_VALUE
	);
	private static final ResourceLocation LEADER_BONUS_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "leader_zombie_altered_bonus");
	private static final ResourceLocation RANDOM_SPAWN_BONUS_ID =
			ResourceLocation.fromNamespaceAndPath(Aurum.MODID, "zombie_altered_random_spawn_bonus");

	private boolean canBreakDoors;
	private final BreakDoorGoal breakDoorGoal =
			new BreakDoorGoal(this, difficulty -> difficulty == Difficulty.HARD);

	public ZombieAlteredEntity(EntityType<? extends Monster> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			setupAnimationStates();
		}
	}

	private void setupAnimationStates() {
		if (this.idleAnimationTimeOut <= 0) {
			this.idleAnimationTimeOut = this.random.nextInt(40) + 80;
			this.idleAnimationState.start(this.tickCount);
		} else {
			--this.idleAnimationTimeOut;
		}

		if (this.isAttacking() && this.attackAnimationTimeOut <= 0) {
			this.attackAnimationTimeOut = 12;
			this.attackAnimationState.start(this.tickCount);
		} else {
			--this.attackAnimationTimeOut;
		}

		if (!this.isAttacking()) {
			this.attackAnimationState.stop();
		}
	}

	@Override
	protected void updateWalkAnimation(float pPartialTick) {
		float f = (this.getPose() == Pose.STANDING) ? Math.min(pPartialTick * 6F, 1.0F) : 0f;
		this.walkAnimation.update(f, 0.2f);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.@NotNull Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(ATTACK_ANIMATION, false);
		entityData.define(CHANNEL_RAYS_ANIMATION, false);
		entityData.define(DATA_BABY_ID, false);
	}

	public void setAttacking(boolean attacking) { this.entityData.set(ATTACK_ANIMATION, attacking); }
	public void setChannelRays(boolean channelRays) { this.entityData.set(CHANNEL_RAYS_ANIMATION, channelRays); }
	public boolean isAttacking() { return this.entityData.get(ATTACK_ANIMATION); }

	// ===== IA mejorada: ataque directo a jugadores + aldeanos + iron golems + refuerzos =====
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(6, new TemptGoal(this, 1.5D, Ingredient.of(Blocks.COPPER_BLOCK), false));
		this.goalSelector.addGoal(1, new ZombieAlteredAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new ExtinguishFireGoal(this));
		this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
		this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	@Override
	public void aiStep() {
		if (this.isAlive()) {
			// Sin sensibilidad al sol (isSunSensitive() = false), así que este bloque nunca aplica daño,
			// pero se deja la estructura por si en el futuro quieres activarlo.
			boolean flag = this.isSunSensitive() && this.isSunBurnTick();
			if (flag) {
				ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
				if (!itemstack.isEmpty()) {
					if (itemstack.isDamageableItem()) {
						Item item = itemstack.getItem();
						itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
						if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
							this.onEquippedItemBroken(item, EquipmentSlot.HEAD);
							this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
						}
					}
					flag = false;
				}
				if (flag) this.igniteForSeconds(8.0F);
			}
		}
		super.aiStep();
	}

	protected boolean isSunSensitive() {
		return false;
	}

	// ===== Baby real =====
	@Override
	public boolean isBaby() {
		return this.getEntityData().get(DATA_BABY_ID);
	}

	@Override
	public void setBaby(boolean childZombie) {
		this.getEntityData().set(DATA_BABY_ID, childZombie);
		if (this.level() != null && !this.level().isClientSide) {
			AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
			attributeinstance.removeModifier(SPEED_MODIFIER_BABY_ID);
			if (childZombie) {
				attributeinstance.addTransientModifier(SPEED_MODIFIER_BABY);
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		if (DATA_BABY_ID.equals(key)) {
			this.refreshDimensions();
		}
		super.onSyncedDataUpdated(key);
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
	}

	@Override
	protected int getBaseExperienceReward() {
		if (this.isBaby()) {
			this.xpReward = (int) ((double) this.xpReward * 2.5);
		}
		return super.getBaseExperienceReward();
	}

	// ===== Rotura de puertas =====
	public boolean canBreakDoors() {
		return this.canBreakDoors;
	}

	public void setCanBreakDoors(boolean canBreakDoors) {
		if (net.minecraft.world.entity.ai.util.GoalUtils.hasGroundPathNavigation(this)) {
			if (this.canBreakDoors != canBreakDoors) {
				this.canBreakDoors = canBreakDoors;
				((net.minecraft.world.entity.ai.navigation.GroundPathNavigation) this.getNavigation()).setCanOpenDoors(canBreakDoors);
				if (canBreakDoors) {
					this.goalSelector.addGoal(1, this.breakDoorGoal);
				} else {
					this.goalSelector.removeGoal(this.breakDoorGoal);
				}
			}
		} else if (this.canBreakDoors) {
			this.goalSelector.removeGoal(this.breakDoorGoal);
			this.canBreakDoors = false;
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(level, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(Blocks.COPPER_BLOCK));

		// Drop de cabeza si lo mata un Creeper cargado (igual que vanilla)
		if (source.getEntity() instanceof Creeper creeper && creeper.canDropMobsSkull()) {
			ItemStack skull = new ItemStack(Items.ZOMBIE_HEAD);
			creeper.increaseDroppedSkulls();
			this.spawnAtLocation(skull);
		}
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.LIGHTNING_BOLT)) return false;
		if (!super.hurt(damagesource, amount)) return false;
		if (!(this.level() instanceof ServerLevel serverlevel)) return false;

		// ===== Sistema de refuerzos (llamada de ayuda) =====
		LivingEntity target = this.getTarget();
		if (target == null && damagesource.getEntity() instanceof LivingEntity le) {
			target = le;
		}

		if (target != null
				&& this.level().getDifficulty() == Difficulty.HARD
				&& this.random.nextFloat() < this.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
				&& this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {

			int i = Mth.floor(this.getX());
			int j = Mth.floor(this.getY());
			int k = Mth.floor(this.getZ());
			ZombieAlteredEntity reinforcement = new ZombieAlteredEntity(ModEntities.ZOMBIE_ALTERED.get(), this.level());
			for (int l = 0; l < 50; l++) {
				int i1 = i + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
				int j1 = j + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
				int k1 = k + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
				BlockPos blockpos = new BlockPos(i1, j1, k1);
				EntityType<?> entitytype = reinforcement.getType();

				if (SpawnPlacements.isSpawnPositionOk(entitytype, this.level(), blockpos)
						&& SpawnPlacements.checkSpawnRules(entitytype, serverlevel, MobSpawnType.REINFORCEMENT, blockpos, this.level().random)) {

					reinforcement.setPos(i1, j1, k1);
					if (!this.level().hasNearbyAlivePlayer(i1, j1, k1, 7.0)
							&& this.level().isUnobstructed(reinforcement)
							&& this.level().noCollision(reinforcement)
							&& !this.level().containsAnyLiquid(reinforcement.getBoundingBox())) {

						reinforcement.setTarget(target);
						reinforcement.finalizeSpawn(serverlevel,
								this.level().getCurrentDifficultyAt(reinforcement.blockPosition()),
								MobSpawnType.REINFORCEMENT, null);
						serverlevel.addFreshEntityWithPassengers(reinforcement);

						AttributeInstance attributeinstance = this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
						AttributeModifier existing = attributeinstance.getModifier(REINFORCEMENT_CALLER_CHARGE_ID);
						double current = existing != null ? existing.amount() : 0.0;
						attributeinstance.removeModifier(REINFORCEMENT_CALLER_CHARGE_ID);
						attributeinstance.addPermanentModifier(new AttributeModifier(
								REINFORCEMENT_CALLER_CHARGE_ID, current - 0.05, AttributeModifier.Operation.ADD_VALUE));

						reinforcement.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
								.addPermanentModifier(ZOMBIE_REINFORCEMENT_CALLEE_CHARGE);
						break;
					}
				}
			}
		}

		return true;
	}

	// ===== Conversión de aldeanos =====
	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity entity) {
		boolean flag = super.killedEntity(level, entity);
		if ((level.getDifficulty() == Difficulty.NORMAL || level.getDifficulty() == Difficulty.HARD)
				&& entity instanceof Villager villager
				&& net.neoforged.neoforge.event.EventHooks.canLivingConvert(entity, EntityType.ZOMBIE_VILLAGER, timer -> {})) {

			if (level.getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
				return flag;
			}

			net.minecraft.world.entity.monster.ZombieVillager zombievillager =
					villager.convertTo(EntityType.ZOMBIE_VILLAGER, false);
			if (zombievillager != null) {
				zombievillager.finalizeSpawn(level,
						level.getCurrentDifficultyAt(zombievillager.blockPosition()),
						MobSpawnType.CONVERSION,
						new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(false, true));
				zombievillager.setVillagerData(villager.getVillagerData());
				zombievillager.setGossips(villager.getGossips().store(NbtOps.INSTANCE));
				zombievillager.setTradeOffers(villager.getOffers().copy());
				zombievillager.setVillagerXp(villager.getVillagerXp());
				net.neoforged.neoforge.event.EventHooks.onLivingConvert(entity, zombievillager);
				if (!this.isSilent()) {
					level.levelEvent(null, 1026, this.blockPosition(), 0);
				}
				flag = false;
			}
		}
		return flag;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.ZOMBIE_HURT; }

	@Override
	public SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }

	@Override
	protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }

	protected SoundEvent getStepSound() { return SoundEvents.ZOMBIE_STEP; }

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		this.playSound(this.getStepSound(), 0.15F, 1.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 35.0)
				.add(Attributes.MOVEMENT_SPEED, 0.23F)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
				.add(ModAttributes.AURUM_IN_BLOOD, 1.0);
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		super.populateDefaultEquipmentSlots(random, difficulty);
		if (random.nextFloat() < (this.level().getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
			int i = random.nextInt(3);
			if (i == 0) {
				this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			} else {
				this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
			}
		}
	}

	// ===== Spawn: baby aleatorio + refuerzos base + aurum =====
	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
										MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
		RandomSource randomsource = level.getRandom();
		spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
		float f = difficulty.getSpecialMultiplier();

		this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * f);

		if (spawnGroupData == null) {
			boolean isBabySpawn = randomsource.nextFloat() < 0.05F;
			spawnGroupData = new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(isBabySpawn, true);
		}

		if (spawnGroupData instanceof net.minecraft.world.entity.monster.Zombie.ZombieGroupData data) {
			if (data.isBaby) {
				this.setBaby(true);
			}
			this.setCanBreakDoors(randomsource.nextFloat() < f * 0.1F);
			this.populateDefaultEquipmentSlots(randomsource, difficulty);
			this.populateDefaultEquipmentEnchantments(level, randomsource, difficulty);
		}

		this.handleAttributes(f);
		rollAurumBlood();

		return spawnGroupData;
	}

	protected void handleAttributes(float difficulty) {
		this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(this.random.nextDouble() * 0.1F);

		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrReplacePermanentModifier(
				new AttributeModifier(RANDOM_SPAWN_BONUS_ID, this.random.nextDouble() * 0.05F, AttributeModifier.Operation.ADD_VALUE));

		double d0 = this.random.nextDouble() * 1.5 * difficulty;
		if (d0 > 1.0) {
			this.getAttribute(Attributes.FOLLOW_RANGE).addOrReplacePermanentModifier(
					new AttributeModifier(RANDOM_SPAWN_BONUS_ID, d0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}

		if (this.random.nextFloat() < difficulty * 0.05F) {
			this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).addOrReplacePermanentModifier(
					new AttributeModifier(LEADER_BONUS_ID, this.random.nextDouble() * 0.25 + 0.5, AttributeModifier.Operation.ADD_VALUE));
			this.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
					new AttributeModifier(LEADER_BONUS_ID, this.random.nextDouble() * 3.0 + 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			this.setCanBreakDoors(true);
		}
	}

	// ===== Aurum en sangre =====
	private void rollAurumBlood() {
		float aurum = 1.0F + this.random.nextFloat() * 99.0F; // ajusta el rango/tope por dimensión cuando lo definas aquí también
		this.getAttribute(ModAttributes.AURUM_IN_BLOOD).setBaseValue(aurum);

		this.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
				new AttributeModifier(AURUM_HEALTH_ID, (aurum - 1.0) * 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		this.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
				new AttributeModifier(AURUM_DAMAGE_ID, (aurum - 1.0) * 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

		this.setHealth(this.getMaxHealth());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("IsBaby", this.isBaby());
		compound.putBoolean("CanBreakDoors", this.canBreakDoors());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.setBaby(compound.getBoolean("IsBaby"));
		this.setCanBreakDoors(compound.getBoolean("CanBreakDoors"));
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		return !(stack.is(Items.EGG) && this.isBaby() && this.isPassenger()) && super.canHoldItem(stack);
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		return !stack.is(Items.GLOW_INK_SAC) && super.wantsToPickUp(stack);
	}
}