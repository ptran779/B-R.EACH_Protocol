package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Soldier extends AbsAgentEntity {
  private static AgentConfig config;
  public Soldier(EntityType<? extends AbsAgentEntity> entityType, Level level) {
    super(entityType, level);
  }
	public String getAgentType(){return "Soldier";};

  public static void updateClassConfig(@Nonnull AgentConfig config) {Soldier.config = config;}
  public AgentConfig getAgentConfig() {return config;}
}