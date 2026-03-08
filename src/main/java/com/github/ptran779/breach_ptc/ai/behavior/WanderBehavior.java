package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class WanderBehavior extends ThrottleBehavior {
  protected final AbsAgentEntity agent;
  Vec3 towards;

  public WanderBehavior(AbsAgentEntity agent, int cooldown, int varcooldown) {
    super(cooldown, varcooldown, agent);
    this.agent = agent;
  }

  public boolean canUse() {
	  return super.canUse() && (agent.getControlFlg1() & AbsAgentEntity.BF_WANDER) != 0;
  }

	public void start(){
		towards = DefaultRandomPos.getPos(agent, 10, 7);
  }

  @Override
  public boolean run() {
    if (towards == null || (agent.getControlFlg1() & AbsAgentEntity.BF_WANDER) == 0) {return true;}
    return !agent.moveto(towards, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
  }

	public void stop(){
		towards = null;
		agent.stopNav();
	}

	public String toString(){return "Wander B";}
}
