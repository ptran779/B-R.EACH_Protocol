package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Engineer extends AbsAgentEntity {
    private static AgentConfig config;
    public Engineer(EntityType<? extends AbsAgentEntity> entityType, Level level) {
        super(entityType, level);
    }
		public String getAgentType(){return "Engineer";};
//    protected void registerGoals() {
//        super.registerGoals();
//        this.goalSelector.addGoal(3, new CustomRetaliationTargetGoal(this));
//        this.goalSelector.addGoal(4, new CustomRangeTargetGoal<>(this, LivingEntity.class, 60, 24, 28, true, entity -> this.shouldTargetEntity(this, (LivingEntity) entity)));
//        this.goalSelector.addGoal(3, new AgentAttackGoal(this, 4, 12, 24));
//        this.goalSelector.addGoal(2, new WorkOnStructureGoal(this, 20));
//    }

    public static void updateClassConfig(@Nonnull AgentConfig config) {Engineer.config = config;}
    public AgentConfig getAgentConfig() {return config;}
}