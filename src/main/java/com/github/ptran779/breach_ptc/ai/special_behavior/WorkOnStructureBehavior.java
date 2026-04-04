package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.config.ServerConfig;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.Engineer;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.entity.structure.AbsAgentStruct;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.item.EngiHammerItem;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class WorkOnStructureBehavior extends ThrottleBehavior {
	AbsAgentEntity agent;
  AbsAgentStruct aStruct;
  protected int tickProgress = -1;
	Sensor<List<LivingEntity>> friendlyS;
	public WorkOnStructureBehavior(Engineer agent, int baseCooldown, int varCooldown, Sensor<List<LivingEntity>> friendlyS) {
		super(agent, baseCooldown, varCooldown);
		this.agent = agent;
		this.friendlyS = friendlyS;
	}
	public boolean canUse() {
		if (!super.canUse()) return false;
    if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 ||
	    !(agent.getSpecialSlot().getItem() instanceof EngiHammerItem)) {
			return false;
		}
		List<LivingEntity> nearby = friendlyS.get(agent.tickCount);
		for (LivingEntity nearbyEnt : nearby) {
			if (nearbyEnt instanceof AbsAgentStruct struct &&
				struct.isAlive() &&
				(struct.charge + ServerConfig.ENGI_WORK_RECHARGE.get() <= struct.getMaxCharge())) {
				aStruct = struct;
				return true;
			}
		}
		return false;
  }

	public void start() {
		tickProgress = -1;
	}
	public void stop() {
		aStruct = null;
		agent.setAniMoveStatic(AnimationID.A_LIVING);
	}

	private boolean canKeepRun(){
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 ||
			!(agent.getSpecialSlot().getItem() instanceof EngiHammerItem)) {
			return false;
		}
		return aStruct != null && aStruct.isAlive();
	}

	@Override
	public boolean run() {
		if (!canKeepRun()) return true;
		int dummy = agent.tickCount - tickProgress;

		agent.getLookControl().setLookAt(aStruct);
		if (agent.distanceToSqr(aStruct) > 4) {
			if(!agent.moveto(aStruct, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue())) return true;
		} else if (tickProgress == -1) {
			agent.setAniMoveStatic(AnimationID.A_BONK);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
				new EntityRenderPacket(agent.getId(), 1));
			tickProgress = agent.tickCount;
		} else if (dummy == 10 || dummy == 20 || dummy == 30 || dummy == 40) {
			agent.level().playSound(null, aStruct, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.BLOCKS, 1f, 1.0f);
		} else if (dummy == 60) {
			agent.equipSpecial(false);
		} else if (dummy == 85 || dummy == 100 || dummy == 115) {
			agent.level().playSound(null, aStruct, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8f, 1.0f);
			((ServerLevel) agent.level()).sendParticles(ParticleTypes.SCRAPE, aStruct.getX(), aStruct.getY()+1.8, aStruct.getZ(), 10, 0, 1, 0, 0.02);
		} else if (dummy == 120) {
			aStruct.charge = Math.min(aStruct.charge +ServerConfig.ENGI_WORK_RECHARGE.get(), aStruct.getMaxCharge());
			if (aStruct.getHealth() < aStruct.getMaxHealth()) {aStruct.heal(2);}
			agent.setAniMoveTransition(AnimationID.A_BONK, AnimationID.A_IDLE, 6f, 0f, 0.5f);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
		} else if (dummy > 140) {
			agent.setAniMoveStatic(AnimationID.A_LIVING);
			return true;
		}
		return false;
	}
}
