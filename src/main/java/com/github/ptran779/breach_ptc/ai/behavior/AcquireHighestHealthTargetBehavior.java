package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.LivingEntity;

public class AcquireHighestHealthTargetBehavior extends AquireConditionalTargetBehavior {
	public AcquireHighestHealthTargetBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, float dropRange,
	                                          Sensor<LivingEntity> highHealthTargetable) {
		super(agent, baseCooldown, varCooldown, dropRange, highHealthTargetable);
	}

	public String toString(){return "High Health Tar Aqr B";}
}
