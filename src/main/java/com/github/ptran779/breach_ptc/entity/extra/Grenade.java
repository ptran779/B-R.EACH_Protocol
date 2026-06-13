package com.github.ptran779.breach_ptc.entity.extra;

import com.github.ptran779.breach_ptc.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import static com.github.ptran779.breach_ptc.server.EffectInit.*;

public class Grenade extends Entity {
	protected Entity source;
	public static final float GRAVITY = -0.04f;
	protected int fuseTick = 60;
	public boolean landed = false;

	private static final EntityDataAccessor<Integer> DATA_TYPE_ID =
		SynchedEntityData.defineId(Grenade.class, EntityDataSerializers.INT);
	public Grenade(EntityType<? extends Grenade> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public void setCaster(Entity source) {this.source = source;}

	protected void defineSynchedData() {
		this.entityData.define(DATA_TYPE_ID, GrenadeType.FRAG.ordinal());
	}
	public void setGrenadeType(GrenadeType type) {
		this.entityData.set(DATA_TYPE_ID, type.ordinal());
	}

	public GrenadeType getGrenadeType() {
		return GrenadeType.values()[this.entityData.get(DATA_TYPE_ID)];
	}

	protected void addAdditionalSaveData(CompoundTag pCompound) {
		pCompound.putInt("GrenadeType", this.getGrenadeType().ordinal());
		pCompound.putInt("Fuse", this.fuseTick);
	}
	protected void readAdditionalSaveData(CompoundTag pCompound) {
		this.setGrenadeType(GrenadeType.values()[pCompound.getInt("GrenadeType")]);
		this.fuseTick = pCompound.getInt("Fuse");
	}

	public enum GrenadeType {
		FRAG, INCENDIARY, EMP, CORROSIVE, CRYO, FLASHBANG
	}

	public void tick() {
		super.tick();
		this.setOldPosAndRot();
		if (!this.onGround()) {
			this.setDeltaMovement(this.getDeltaMovement().add(0, GRAVITY, 0));
		} else {
			landed = true;
			this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
		}
		this.move(MoverType.SELF, this.getDeltaMovement());

		if (!level().isClientSide) {
			if (fuseTick-- <= 0) {
				switch (getGrenadeType()) {
					case FRAG: {
						fragBoom();
						break;
					}
					case INCENDIARY: {
						incendiaryBoom();
						break;
					}
					case EMP: {
						empBoom();
						break;
					}
					case CORROSIVE: {
						corrosiveBoom();
						break;
					}
					case CRYO: {
						cryoBoom();
						break;
					}
					case FLASHBANG: {
						flashBangBoom();
						break;
					}
				}
				discard();
			}
		}
	}

	private void fragBoom() {
		level().explode(source, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.NONE);
		ServerLevel serverLevel = (ServerLevel) level();
		serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 100, 3.0, 2.0, 3.0, 0.15);
	}
	private void incendiaryBoom() {
		ServerLevel serverLevel = (ServerLevel) level();
		AABB area =
			new AABB(this.getX() - 4, this.getY() - 4, this.getZ() - 4, this.getX() + 4, this.getY() + 4, this.getZ() + 4);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (Utils.rayCastHit(this.position(), entity.position(), (ServerLevel) level())) continue;
			MobEffectInstance existing = entity.getEffect(THERMAL_BLEED.get());
			int newAmp = existing != null ? Math.min(existing.getAmplifier() + 1, 4) : 0;
			entity.addEffect(new MobEffectInstance(THERMAL_BLEED.get(), 200, newAmp));
		}
		// forest fire
		for (int i = 0; i < 20; i++) {
			int ox = random.nextInt(9) - 4; // -4 to +4
			int oz = random.nextInt(9) - 4;
			BlockPos firePos = BlockPos.containing(this.getX() + ox, this.getY(), this.getZ() + oz);
			// walk down until we hit a solid block
			while (serverLevel.getBlockState(firePos).isAir() && firePos.getY() > serverLevel.getMinBuildHeight()) {
				firePos = firePos.below();
			}
			BlockPos above = firePos.above();
			if (serverLevel.getBlockState(above).isAir()) {
				serverLevel.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
			}
		}
		serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 120, 3.0, 2.0, 3.0, 0.15);
		serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 50, 2.0, 1.5, 2.0, 0.0);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 2.0F,
			0.8F);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F,
			1.0F);
	}
	private void empBoom() {
		ServerLevel serverLevel = (ServerLevel) level();
		AABB area =
			new AABB(this.getX() - 6, this.getY() - 6, this.getZ() - 6, this.getX() + 6, this.getY() + 6, this.getZ() + 6);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (Utils.rayCastHit(this.position(), entity.position(), (ServerLevel) level())) continue;
			entity.addEffect(new MobEffectInstance(EMP_DISRUPTOR.get(), 100, 0));
		}
		serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 160, 4.0, 4.0, 4.0,
			0.3);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
			SoundSource.BLOCKS, 1.5F, 1.8F);
	}
	private void corrosiveBoom() {
		ServerLevel serverLevel = (ServerLevel) level();
		AABB area =
			new AABB(this.getX() - 4, this.getY() - 4, this.getZ() - 4, this.getX() + 4, this.getY() + 4, this.getZ() + 4);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (Utils.rayCastHit(this.position(), entity.position(), (ServerLevel) level())) continue;
			entity.addEffect(new MobEffectInstance(CORROSIVE_SLUDGE.get(), 200, 0));
		}
		serverLevel.sendParticles(ParticleTypes.DRIPPING_HONEY, this.getX(), this.getY(), this.getZ(), 100, 2.5, 2.0, 2.5,
			0.05);
		serverLevel.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, this.getX(), this.getY(), this.getZ(), 60, 3.0, 2.0,
			3.0, 0.0);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 2.0F,
			0.5F);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS,
			1.5F, 1.0F);
	}
	private void cryoBoom() {
		ServerLevel serverLevel = (ServerLevel) level();
		AABB area =
			new AABB(this.getX() - 6, this.getY() - 6, this.getZ() - 6, this.getX() + 6, this.getY() + 6, this.getZ() + 6);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (Utils.rayCastHit(this.position(), entity.position(), (ServerLevel) level())) continue;
			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
			entity.setTicksFrozen(344);
		}
		serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 160, 4.0, 4.0, 4.0, 0.1);
		serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY(), this.getZ(), 60, 3.0, 3.0, 3.0,
			0.2);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.POWDER_SNOW_PLACE, SoundSource.BLOCKS,
			2.0F, 0.8F);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F,
			1.2F);
	}
	private void flashBangBoom() {
		ServerLevel serverLevel = (ServerLevel) level();
		AABB area =
			new AABB(this.getX() - 8, this.getY() - 8, this.getZ() - 8, this.getX() + 8, this.getY() + 8, this.getZ() + 8);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (Utils.rayCastHit(this.position(), entity.position(), (ServerLevel) level())) continue;
			entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
			entity.addEffect(new MobEffectInstance(FLASH_BANG.get(), 160, 0));
		}
		serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 100, 5.0, 5.0, 5.0, 0.0);
		level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
			SoundSource.BLOCKS, 3.0F, 1.5F);
	}
}
