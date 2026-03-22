package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.HeavyBrain;
import com.github.ptran779.breach_ptc.ai.brain.SoldierBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Soldier extends AbsAgentEntity {
	SoldierBrain soldierBrain;
	public static AgentConfig config;
	public Soldier(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		soldierBrain = new SoldierBrain(this);
	}
	public String getAgentType() {return "Soldier";}
	;

	public static void updateClassConfig(@Nonnull AgentConfig config) {Soldier.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {
		return soldierBrain;
	}
	@Override public int getInputSpace() {
		return SoldierBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return SoldierBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return SoldierBrain.CUSTOM_SPACE;
	}
}