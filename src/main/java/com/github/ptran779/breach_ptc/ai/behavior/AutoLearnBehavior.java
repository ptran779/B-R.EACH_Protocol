package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.MLServer;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static com.github.ptran779.breach_ptc.server.ForgeServerEvent.BRAIN_SERVER;

// time -- for a real pain in the ass :(
public class AutoLearnBehavior extends ThrottleBehavior {
	AbsAgentEntity agent;
	boolean nightPass=false;
	UUID unitUUID;
	MlModelManager.MLUnit mUnit;
	public AutoLearnBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown) {
		super(baseCooldown, varCooldown, agent);
		this.agent = agent;
	}

	public boolean canUse(){
		if(!super.canUse()) return false;
		Level level= agent.level();
		if (level.getDayTime() < 14000 || level.getDayTime() > 22000){
			nightPass = true;
			return false;
		}
		if (!nightPass) return false;
		return prepareTraining();
	}

	public boolean prepareTraining(){
		if (!(agent instanceof Swordman swordman)) return false;
		if (!swordman.swordBrain.autotrain || swordman.swordBrain.scoreFunc == null) return false;
		ItemStack brainChip = agent.getChipBrainStack();
		// confirm brain chip exist
		if (brainChip.getItem() instanceof BrainChipItem brainChipItem){
			// confirm model & data exist
			unitUUID = brainChipItem.getOrCreateUUID(brainChip);
			mUnit = MlModelManager.getMUnit(unitUUID, agent.level().getGameTime());
			// verified correct ML model && data state
			if (mUnit.model == null || mUnit.model.getInsize() != agent.getSensorSize() || mUnit.model.getOutsize() != agent.getBehaviorSize()) return false;
			return mUnit.dataManager != null && !mUnit.dataManager.getRawDat().isEmpty(); // there is nothing to train off
		}
		return false;
	}

	public void stop(){
		nightPass = false;
	}

	public void start(){
		// prepare data for training
		mUnit.dataManager.prepareData(mUnit.model.valFrac, mUnit.model.testFrac, mUnit.model.maxChain);
		BRAIN_SERVER.TASK_QUEUE_TRAIN.add(new MLServer.TrainDatIn(agent.getUUID(), MLServer.TARGET_RECEIVER.AGENT, unitUUID, mUnit.model, mUnit.dataManager));
		((Swordman)agent).swordBrain.lockComputing();
//		System.out.println("sword brain send it's training regard...");
	}

	@Override
	public boolean run() {
		return false;
	}

	public String toString(){return "Auto Learn B";}
}
