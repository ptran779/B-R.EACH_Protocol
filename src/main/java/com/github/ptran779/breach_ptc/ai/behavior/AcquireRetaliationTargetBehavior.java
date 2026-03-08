package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.LivingEntity;

public class AcquireRetaliationTargetBehavior extends AquireConditionalTargetBehavior {
	public AcquireRetaliationTargetBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, float dropRange, Sensor<LivingEntity> retarHostileS) {
		super(agent, baseCooldown, varCooldown, dropRange, retarHostileS);
	}

	public String toString(){return "Retar Aquire B";}
}
