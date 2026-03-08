package com.github.ptran779.breach_ptc.ai.api;

import com.github.ptran779.breach_ptc.attribute.AgentAttribute;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;

// mostly support timing executing, cooldown method, and all of its thing
public abstract class CombatBehavior extends Behavior {
  protected AbsAgentEntity agent;
  protected int attackCoolDown;
  protected double dropRS, targetRS;
  protected double speedScale;

  public CombatBehavior(AbsAgentEntity agent, double dropR, double speedScale) {
    this.agent = agent;
    this.dropRS = dropR*dropR;
    this.speedScale = speedScale;
  }

  protected void resetCooldown() {
    attackCoolDown = (int) Math.max(1, (20 / (agent.getAttribute(AgentAttribute.AGENT_ATTACK_SPEED).getValue() * speedScale)));
  }

  public void start(){
    agent.setAggressive(true);
    resetCooldown();
  }

  public void stop(){
    agent.setAggressive(false);
    agent.stopNav();
    agent.setAniMoveStatic(AnimationID.A_LIVING);
  }
}
