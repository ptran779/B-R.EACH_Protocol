package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.EngineerBrain;
import com.github.ptran779.breach_ptc.ai.brain.HeavyBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Heavy extends AbsAgentEntity {
	HeavyBrain heavyBrain;
	public static AgentConfig config;
	public Heavy(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		heavyBrain = new HeavyBrain(this);
	}
	public String getAgentType() {return "Heavy";}
	;

	public static void updateClassConfig(@Nonnull AgentConfig config) {Heavy.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {
		return heavyBrain;
	}
	@Override public int getInputSpace() {
		return HeavyBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return HeavyBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return HeavyBrain.CUSTOM_SPACE;
	}
}