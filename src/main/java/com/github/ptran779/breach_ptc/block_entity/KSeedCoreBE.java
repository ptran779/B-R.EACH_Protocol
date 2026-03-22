package com.github.ptran779.breach_ptc.block_entity;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.block.KSeedCoreBlock;
import com.github.ptran779.breach_ptc.config.SkinManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.server.BlockEntityInit;
import com.github.ptran779.breach_ptc.server.BlockInit;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.github.ptran779.breach_ptc.Utils.generateAgent;

public class KSeedCoreBE extends BlockEntity {
	public int tickcount = -40;
	private UUID riderUUID;  // superseded. If this exist, use this skin
	private boolean female = false;
	private String skin="";
	private int agentPoolIdx;
	private boolean spawnRandom = false;
	// skin
	private transient ResourceLocation cachedSkin;
	public ResourceLocation getCryoSkin() {
		if (cachedSkin == null) {
			if (riderUUID != null) {
				// Use MC native skin lookup from UUID
				GameProfile profile = new GameProfile(riderUUID, null);
				cachedSkin = Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile);
				// Detect slim (Alex) vs classic (Steve) model — overwrite female flag
				female = DefaultPlayerSkin.getSkinModelName(riderUUID).equals("slim");
			} else {
				cachedSkin = SkinManager.get(female, skin);
			}
		}
		return cachedSkin;
	}

	public KSeedCoreBE(BlockPos pPos, BlockState pBlockState) {
		super(BlockEntityInit.K_SEED_CORE_BE.get(), pPos, pBlockState);
	}

	protected void saveAdditional(CompoundTag pTag) {
		super.saveAdditional(pTag);
		pTag.putBoolean("female", female);
		pTag.putString("skin", skin);
		pTag.putBoolean("spawnRandom", spawnRandom);
		if (riderUUID != null) pTag.putUUID("riderUUID", riderUUID);
	}
	public void load(CompoundTag pTag) {
		super.load(pTag);
		female = pTag.getBoolean("female");
		skin = pTag.getString("skin");
		spawnRandom = pTag.getBoolean("spawnRandom");
		if (pTag.hasUUID("riderUUID")) riderUUID = pTag.getUUID("riderUUID");
	}
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		tag.putBoolean("female", female);
		tag.putString("skin", skin);
		tag.putBoolean("spawnRandom", spawnRandom);
		if (riderUUID != null) tag.putUUID("riderUUID", riderUUID);
		return tag;
	}
	public void handleUpdateTag(CompoundTag tag) {
		super.handleUpdateTag(tag);
		if (tag.contains("female")) this.female = tag.getBoolean("female");
		if (tag.contains("skin")) this.skin = tag.getString("skin");
		if (tag.contains("spawnRandom")) this.spawnRandom = tag.getBoolean("spawnRandom");
		if (tag.hasUUID("riderUUID")) riderUUID = tag.getUUID("riderUUID");
	}
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		handleUpdateTag(pkt.getTag());
	}

	public boolean getFemale() {return female;}
	public void setRiderUUID(UUID riderUUID) {this.riderUUID = riderUUID;}
	public boolean getSpawnRandom(){return spawnRandom;}
	public boolean getHasRider(){return riderUUID != null;}

	// Call this method from the server side when the pod decides who is inside!
	public void setAgentData(Utils.AgentData data) {
		this.female = data.female();
		this.skin = data.skin();
		this.agentPoolIdx = data.agentPoolIdx();
		this.cachedSkin = null;
		this.spawnRandom = true;

		this.setChanged();

		// If we are on the server, broadcast the new data to all clients looking at the pod
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
		}
	}

	protected void spawnRandom(){
		AbsAgentEntity agent = generateAgent(level, agentPoolIdx);
		agent.setCosmetic(female, skin);
		agent.getSuperBrain().activateGoalWrapper();

		BlockPos pos = this.getBlockPos();
		agent.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 1.5D);  // fixme critical when adding block direction

		level.addFreshEntity(agent);
	}

	public void tick() {
		tickcount++;
		if (!level.isClientSide()) {
			// lock in rider position and no moving
			if (riderUUID != null) {
				ServerPlayer player = ((ServerLevel) level).getServer()
					.getPlayerList().getPlayer(riderUUID);
				if (player != null) {
					player.teleportTo(
						worldPosition.getX() + 0.5,
						worldPosition.getY() + 0.1,
						worldPosition.getZ() + 0.5
					);
					player.setDeltaMovement(Vec3.ZERO);
				}
			}
			if (tickcount == 0) {
				// High-pressure vent and unlatch
				((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5,
					worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.05);
				((ServerLevel) level).sendParticles(ParticleTypes.SNOWFLAKE, worldPosition.getX() + 0.5,
					worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.05);

				level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.5f);
			} else if (tickcount == 20) {
				// Core rising
				level.playSound(null, worldPosition, SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.2f);
			} else if (tickcount == 80) {
				BlockState current = level.getBlockState(worldPosition);
				level.setBlock(worldPosition, current.setValue(KSeedCoreBlock.DEPLOY, true), 3);
			}
			else if (tickcount == 90) {
				// pressurized door open
				((ServerLevel) level).sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 0.25,
					worldPosition.getZ() + 1, 10, 0.3, 0.1, 0.3, 0.05);
				level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
			} else if (tickcount == 115){
				level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 0.5f);
			} else if (tickcount == 145 || tickcount == 165 || tickcount == 185){
				level.playSound(null, worldPosition, SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.BLOCKS, 1.0f, 0.5f);
			}
			else if (tickcount >= 320) {
				// fixme critical update later for directional?
				if (spawnRandom) spawnRandom();
				level.setBlock(worldPosition, BlockInit.K_SEED_CORE_USED_1.get().defaultBlockState(), 3);
				level.removeBlockEntity(worldPosition);
			}
			if(tickcount >= 20 && tickcount <= 80) {
				double minX = worldPosition.getX() - 0.5;
				double maxX = worldPosition.getX() + 0.5;
				double minY = worldPosition.getY()-2;
				double maxY = worldPosition.getY();
				double minZ = worldPosition.getZ() - 0.5;
				double maxZ = worldPosition.getZ() + 0.5;

				level.getEntities(null, new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ))
					.forEach(entity -> {
						entity.hurtMarked = true;
						if (tickcount <= 30){
							entity.setDeltaMovement(0, 0.2, 0);
						} else {
							entity.teleportTo(entity.getX(), maxY, entity.getZ());
						}
					});
			}
		}
	}
}
