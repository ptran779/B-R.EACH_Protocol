package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.DemolitionBrain;
import com.github.ptran779.breach_ptc.ai.brain.EngineerBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Engineer extends AbsAgentEntity {
	EngineerBrain engineerBrain;
	public static AgentConfig config;
	public Engineer(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		engineerBrain = new EngineerBrain(this);
	}
	public String getAgentType() {return "Engineer";}

	public static void updateClassConfig(@Nonnull AgentConfig config) {Engineer.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {
		return engineerBrain;
	}
	@Override public int getInputSpace() {
		return EngineerBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return EngineerBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return EngineerBrain.CUSTOM_SPACE;
	}
}