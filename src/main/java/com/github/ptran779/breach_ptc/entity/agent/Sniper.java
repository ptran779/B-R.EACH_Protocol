package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.SniperBrain;
import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Sniper extends AbsAgentEntity {
	public SniperBrain sniperBrain;
	public static AgentConfig config;
	public Sniper(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		sniperBrain = new SniperBrain(this);
	}
	public String getAgentType() {return "Sniper";}
	;

	public static void updateClassConfig(@Nonnull AgentConfig config) {Sniper.config = config;}
	public AgentConfig getAgentConfig() {return config;}
	@Override public AbsAgentBrain getSuperBrain() {return sniperBrain;}
	@Override public int getInputSpace() {
		return SniperBrain.INPUT_SPACE;
	}
	@Override public int getOutputSpace() {
		return SniperBrain.OUTPUT_SPACE;
	}
	@Override public int getCustSpace() {
		return SniperBrain.CUSTOM_SPACE;
	}
}