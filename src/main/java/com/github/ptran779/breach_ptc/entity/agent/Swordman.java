package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Swordman extends AbsAgentEntity {
	public SwordBrain swordBrain;
	private static AgentConfig config;
	private final Set<UUID> observingPlayer = new HashSet<>();// tmp usage
	public Swordman(EntityType<? extends AbsAgentEntity> entityType, Level level) {
    super(entityType, level);
		swordBrain = new SwordBrain(this);
  }
	public String getAgentType(){return "Swordman";};

	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if(!brainMode) {swordBrain.activateGoalWrapper();}
	  swordBrain.diskRead(nbt);
	}

	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		swordBrain.diskWrite(nbt);
	}

	public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
    swordBrain.activateGoalWrapper();
		return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
  }

  public int getMaxVirtualAmmo(){return config.maxVirtualAmmo;}
  public int getAmmoPerCharge(){return config.chargePerAmmo;}
  public static void updateClassConfig(@Nonnull AgentConfig config) {Swordman.config = config;}
  public AgentConfig getAgentConfig() {return config;}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && brainMode) {
			swordBrain.tick();
		}
	}
	public boolean hurt(DamageSource source, float amount){
		swordBrain.forceWakeUp();
		return super.hurt(source, amount);
	}

	public int getSensorSize(){return SwordBrain.INPUT_SPACE;}
	public String getCSVSensorsHeader(){return SwordBrain.getCSVHeader();}
	public int getBehaviorSize(){return SwordBrain.OUTPUT_SPACE;}
	public int getScrCustVarSize(){return SwordBrain.CUSTOM_SPACE;}
	// check for brain chip Existence, confirm IO equal, deregister goal, turn on brain mode
	public void reloadBrain(){
		if (brainChipValid()){
			brainMode = true;
			removeFreeWill();  // purge all goal
		} else brainOFF();
	}
	// brain and chip valid for computing
	public boolean brainChipValid(){
		ItemStack item = getChipBrainStack();
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, level().getGameTime());
			if (mUnit.model == null) {
				return false;
			} else if (mUnit.model.getInsize() != getSensorSize() || mUnit.model.getOutsize() != getBehaviorSize()) {
				return false;
			} else {
				printObservation("agent " + getDisplayName() + " turn on brain mode", level().getServer());
				return true;
			}
		}
		return false;
	}
	public void brainOFF(){
		brainMode = false;
		if (goalSelector.getAvailableGoals().isEmpty()) swordBrain.activateGoalWrapper();
	}
	public boolean toggleObserver(UUID id) {
		if (observingPlayer.contains(id)) {
			observingPlayer.remove(id);
			return false; // Turned OFF
		} else {
			observingPlayer.add(id);
			return true; // Turned ON
		}
	}
	public void printObservation(String msg, MinecraftServer server){
		if (observingPlayer.isEmpty()) return;
		Component msgCom = Component.literal(msg);
		for (UUID id : observingPlayer) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {observingPlayer.remove(id);continue;}
			player.sendSystemMessage(msgCom);
		}
	}
}