package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.LivingEntity;

public class AcquireNearestTargetBehavior extends AquireConditionalTargetBehavior {
	public AcquireNearestTargetBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, float dropRange,
	                                    Sensor<LivingEntity> nearestTargetable) {
		super(agent, baseCooldown, varCooldown, dropRange, nearestTargetable);
	}

	public String toString(){return "Nearest Tar Aqr B";}
}
