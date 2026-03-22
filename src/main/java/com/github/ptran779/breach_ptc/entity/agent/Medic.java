package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.MedicBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Medic extends AbsAgentEntity {
	public MedicBrain medicBrain;
	public static AgentConfig config;
	public Medic(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		medicBrain = new MedicBrain(this);
	}
	public String getAgentType() {return "Medic";}
	;

	public static void updateClassConfig(@Nonnull AgentConfig config) {Medic.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {return medicBrain;}
	@Override public int getInputSpace() {
		return MedicBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return MedicBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return MedicBrain.CUSTOM_SPACE;
	}
}