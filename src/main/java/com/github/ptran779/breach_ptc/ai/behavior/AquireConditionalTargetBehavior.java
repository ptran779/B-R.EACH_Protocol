package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.LivingEntity;

public class AquireConditionalTargetBehavior extends ThrottleBehavior {
	Sensor<LivingEntity> targetSensor;
	protected LivingEntity target;
	protected AbsAgentEntity agent;
	protected float dropRangeSq;
	public AquireConditionalTargetBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, float dropRange,
	                                       Sensor<LivingEntity> nearestTargetable) {
		super(baseCooldown, varCooldown, agent);
		this.agent = agent;
		dropRangeSq = dropRange*dropRange;
		this.targetSensor = nearestTargetable;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;

		target = targetSensor.get(agent.tickCount);
		return target != null;
	}

	public boolean canUseGoal(){
		if (!super.canUse()) return false;
		if (agent.getTarget() != null && agent.getTarget().isAlive() && agent.getTarget().distanceToSqr(agent) < dropRangeSq) return false;
		agent.setTarget(null);  // clean the dead weight too far

		target = targetSensor.get(agent.tickCount);
		return target != null;
	}

	public void start(){
		agent.setTarget(target);
		target = null;
	}

	@Override
	public boolean run() {
		return true;
	}

	public String toString(){return "Conditional Tar Aqr B";}
}

