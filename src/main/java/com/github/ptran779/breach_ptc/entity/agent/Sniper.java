package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Sniper extends AbsAgentEntity {
    private static AgentConfig config;
    public Sniper(EntityType<? extends AbsAgentEntity> entityType, Level level) {
        super(entityType, level);
    }
		public String getAgentType(){return "Sniper";};

    public static void updateClassConfig(@Nonnull AgentConfig config) {Sniper.config = config;}
    public AgentConfig getAgentConfig() {return config;}
}