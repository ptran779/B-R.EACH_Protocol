package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.entity.structure.AbsAgentStruct;
import com.github.ptran779.breach_ptc.item.IHealItem;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class HealFriendlyBehavior extends ThrottleBehavior {
	AbsAgentEntity agent;
	Sensor<List<LivingEntity>> friendlyS;
	LivingEntity healTarget = null;
	protected int tickProgress = -1;
	/**
	 * @param entity       The entity executing this behavior.
	 * @param baseCooldown The fixed minimum ticks before this behavior can run again.
	 * @param varCooldown  The noise amplitude (0 to varCooldown). Added to base for variability.
	 */
	public HealFriendlyBehavior(AbsAgentEntity entity, int baseCooldown, int varCooldown, Sensor<List<LivingEntity>> friendlyS) {
		super(entity, baseCooldown, varCooldown);
		this.agent = entity;
		this.friendlyS = friendlyS;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 ||
			!(agent.getSpecialSlot().getItem() instanceof IHealItem healitem)) {return false;}
		List<LivingEntity> nearby = friendlyS.get(agent.tickCount);
		for (LivingEntity nearbyEnt : nearby) {
			if (nearbyEnt instanceof AbsAgentStruct) continue;  // pls dont heal structure
			if (healitem.canHeal(nearbyEnt)) {
				healTarget = nearbyEnt;
				return true;
			}
		}
		return false;
	}
	public boolean canKeepRun(){
		return (healTarget != null && healTarget.isAlive());
	}
	public void start() {
		tickProgress = -1;
	}
	public void stop() {
		healTarget = null;
		agent.setAniMoveStatic(AnimationID.A_LIVING);
	}
	public boolean run() {
		if (!canKeepRun()) return true;
		int dummy = agent.tickCount - tickProgress;
		agent.getLookControl().setLookAt(healTarget);


		if (agent.distanceToSqr(healTarget) > 4) {
			return !agent.moveto(healTarget, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
		} else {
			if (!(agent.getSpecialSlot().getItem() instanceof IHealItem healItem)) return true;
			if (tickProgress == -1) {
				agent.setAniMoveStatic(healItem.getAniMove());
				PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
				tickProgress = agent.tickCount;
				agent.equipSpecial(false);
			} else {
				return healItem.computeEffect(healTarget, dummy, agent.getSpecialSlot());  // complete healing
			}
		}
		return false;
	}
}
