package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Demolition extends AbsAgentEntity {
    private static AgentConfig config;
    public Demolition(EntityType<? extends AbsAgentEntity> entityType, Level level) {
        super(entityType, level);
    }

		public String getAgentType(){return "Demolition";};

//    protected void registerGoals() {
//        super.registerGoals();
//        this.goalSelector.addGoal(2, new DeployVPGoal(this, 100, 300));
//        this.goalSelector.addGoal(2, new ThrowGrenadeGoal(this, 60, 300));
//        this.goalSelector.addGoal(3, new CustomRetaliationTargetGoal(this));
//        this.goalSelector.addGoal(4, new CustomRangeTargetGoal<>(this, LivingEntity.class, 30, 32, 48, true, entity -> this.shouldTargetEntity(this, (LivingEntity) entity)));
//        this.goalSelector.addGoal(3, new AgentAttackGoal(this, 4, 8, 32));
//    }

    public static void updateClassConfig(@Nonnull AgentConfig config) {Demolition.config = config;}
    public AgentConfig getAgentConfig() {return config;}

    public int getMaxVirtualAmmo(){return config.maxVirtualAmmo;}
    public int getAmmoPerCharge(){return config.chargePerAmmo;}

    //dummy test
    public int getSensorSize(){return 10;}
    public int getBehaviorSize(){return 10;}
}