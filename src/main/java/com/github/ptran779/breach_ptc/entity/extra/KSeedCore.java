package com.github.ptran779.breach_ptc.entity.extra;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.block_entity.KSeedCoreBE;
import com.github.ptran779.breach_ptc.server.BlockInit;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Collections;

public class KSeedCore extends LivingEntity {
	public static final int FLAP_DEPLOY_HEIGHT = 50;
	private static final EntityDataAccessor<Integer>
		DEPLOYED_TIME = SynchedEntityData.defineId(KSeedCore.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean>
		SPAWN_AGENT = SynchedEntityData.defineId(KSeedCore.class, EntityDataSerializers.BOOLEAN);

	public KSeedCore(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public int getDeployedTime(){return entityData.get(DEPLOYED_TIME);}
	public void setDeployedTime(int time){entityData.set(DEPLOYED_TIME, time);}
	public boolean getSpawnAgent(){return entityData.get(SPAWN_AGENT);}
	public void setSpawnAgent(boolean in){entityData.set(SPAWN_AGENT, in);}

	protected void defineSynchedData(){
		super.defineSynchedData();
		entityData.define(DEPLOYED_TIME, -1);
		entityData.define(SPAWN_AGENT, false);
	}
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("deployedTime", this.getDeployedTime());
		tag.putBoolean("spawnAgent", this.getSpawnAgent());
	}
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setDeployedTime(tag.getInt("deployedTime"));
		this.setSpawnAgent(tag.getBoolean("spawnAgent"));
	}

	public void tick(){
		super.tick();
		this.setDeltaMovement(0,Math.max(getDeltaMovement().y, -1.6),0);
		if (!level().isClientSide) {
			/// make sure you stay alive :)
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof LivingEntity rider) {
					// Invisibility pulse every 2 seconds
					if (tickCount % 40 == 0) {
						rider.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
						rider.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,    60, 3, false, false));
						rider.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false));
						rider.addEffect(new MobEffectInstance(MobEffects.REGENERATION,    60, 2, false, false));
						rider.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,      60, 3, false, false));
						rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
					}
				}
			}

			((ServerLevel) level()).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,getX() ,getY(),getZ(), 1,0,0,0, 0.01); // small upward drift
			// lower the refresh rate since I dont really need this heavy
			if (getDeployedTime() < 0){
				if (tickCount % 5 == 0) {
					BlockPos below = this.blockPosition();
					int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, below.getX(), below.getZ());
					if (getBlockY() - groundY <= FLAP_DEPLOY_HEIGHT) {setDeployedTime(tickCount);}
				}
			} else {
				if (tickCount-getDeployedTime() > 20){
					if (tickCount % 4 == 0) level().playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1F, 0.8F + random.nextFloat() * 0.4F);
					for (int i=0; i<2; i++) {
						((ServerLevel) level()).sendParticles(ParticleTypes.FLAME,getX() + 0.8 + i*0.3, getY()+2.4 - i*0.4, getZ(),
							1, 0, 0, 0.1, 0.01);
					}
					for (int i=0; i<2; i++) {
						((ServerLevel) level()).sendParticles(ParticleTypes.FLAME,getX() - 0.8 - i*0.3, getY()+2.4 - i*0.4, getZ(),
							1, 0, 0, 0.1, 0.01);
					}
					for (int i=0; i<2; i++) {
						((ServerLevel) level()).sendParticles(ParticleTypes.FLAME,getX(), getY()+2.4 - i*0.4, getZ() - 0.8 - i*0.3,
							1, 0.1, 0, 0, 0.01);
					}
					for (int i=0; i<2; i++) {
						((ServerLevel) level()).sendParticles(ParticleTypes.FLAME,getX(), getY()+2.4 - i*0.4, getZ() + 0.8 + i*0.3,
							1, 0.1, 0, 0, 0.01);
					}
					this.setDeltaMovement(0,Math.max(getDeltaMovement().y, -0.5),0);
				}
			}
			if (this.onGround()) {triggerCrash();}
		}
	}
	private void triggerCrash() {
		Level level = this.level();
		((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1F, 0.8F + random.nextFloat() * 0.4F);
		level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1F, 0.8F + random.nextFloat() * 0.4F);

		BlockPos botPos = this.blockPosition().below();
		BlockPos bePos = botPos.below();

		// 1. Clear landing site
		level.setBlock(botPos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(bePos, Blocks.AIR.defaultBlockState(), 3);

		// 2. Place HellPod BE base
		level.setBlock(this.blockPosition(), BlockInit.K_SEED_CORE.get().defaultBlockState(), 3);
		if(getSpawnAgent()){
			KSeedCoreBE be = (KSeedCoreBE) level.getBlockEntity(blockPosition());
			if (be != null) {
				be.setAgentData(Utils.generateRandomAgent(level()));
				be.setChanged();
				level.sendBlockUpdated(blockPosition(), be.getBlockState(), be.getBlockState(), 3);
			}
		}

		// 3. Transfer tagger or passengers (optional)
		if (!this.getPassengers().isEmpty()) {
			this.getPassengers().forEach(entity -> {
				entity.stopRiding();
				entity.teleportTo(bePos.getX() + 0.5, bePos.getY() + 0.1, bePos.getZ() + 0.5);
				if(entity instanceof Player player){
					// get the block and set setRiderUUID
					KSeedCoreBE be = (KSeedCoreBE) level.getBlockEntity(blockPosition());
					if (be != null) {
						be.setRiderUUID(player.getUUID());
						be.setChanged();
						level.sendBlockUpdated(blockPosition(), be.getBlockState(), be.getBlockState(), 3);
						// give invis and slowness 10 haste 10?
						int duration = (int)(18 * 20);
						player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,    duration, 0, false, false));
						player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,    duration, 9, false, false));
						player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 9, false, false));
					}
				}
			});
		}
		this.discard(); // Remove falling entity
	}

	public boolean shouldRenderAtSqrDistance(double distanceSq) {return distanceSq < 102400;}
	public Iterable<ItemStack> getArmorSlots() {return Collections.emptyList();}
	public ItemStack getItemBySlot(EquipmentSlot slot) {return ItemStack.EMPTY;}
	public HumanoidArm getMainArm() {return null;}
	public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}
}
