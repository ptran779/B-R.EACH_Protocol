package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.DemolitionBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Demolition extends AbsAgentEntity {
	DemolitionBrain demolitionBrain;
	public static AgentConfig config;
	public Demolition(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		demolitionBrain = new DemolitionBrain(this);
	}

	public String getAgentType() {return "Demolition";}

	public static void updateClassConfig(@Nonnull AgentConfig config) {Demolition.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {
		return demolitionBrain;
	}
	@Override public int getInputSpace() {
		return DemolitionBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return DemolitionBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return DemolitionBrain.CUSTOM_SPACE;
	}

}