package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class Swordman extends AbsAgentEntity {
	public SwordBrain swordBrain;
	public static AgentConfig config;

	public Swordman(EntityType<? extends AbsAgentEntity> entityType, Level level) {
    super(entityType, level);
		swordBrain = new SwordBrain(this);
  }
	public String getAgentType(){return "Swordman";};

	// critical fixme tmp attribute boost until I figure out what to do with this problem child for special skill
	public static AttributeSupplier.Builder createAttributes() {
		return AbsAgentEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)       // bonus HP on top of base 20 = 30 total
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.ARMOR_TOUGHNESS, 3.0)
			.add(Attributes.ATTACK_DAMAGE, 4.0)     // bonus on top of base 1 = 5 total
			.add(Attributes.MOVEMENT_SPEED, 0.55)    // bonus on top of base 0.5 = 0.6
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

  public int getMaxVirtualAmmo(){return config.maxVirtualAmmo;}
  public int getAmmoPerCharge(){return config.chargePerAmmo;}
  public static void updateClassConfig(@Nonnull AgentConfig config) {Swordman.config = config;}
  public AgentConfig getAgentConfig() {return config;}

	public String getCSVSensorsHeader(){return SwordBrain.getCSVHeader();}
	public int getInputSpace(){return SwordBrain.INPUT_SPACE;}
	public int getOutputSpace(){return SwordBrain.OUTPUT_SPACE;}
	public int getCustSpace(){return SwordBrain.CUSTOM_SPACE;}
	public AbsAgentBrain getSuperBrain() {return swordBrain;}
}