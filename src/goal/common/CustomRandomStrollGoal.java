package com.github.ptran779.breach_ptc.goal.common;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.entity.agent.AbstractAgentEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

public class CustomRandomStrollGoal extends RandomStrollGoal {
  private final AbstractAgentEntity agent;

  public CustomRandomStrollGoal(AbstractAgentEntity agent, double pSpeedModifier, int cooldownTicks) {
    super(agent, pSpeedModifier, cooldownTicks);
    this.agent = agent;
  }

  @Override
  public boolean canUse() {
    return agent.getFollowMode() == Utils.FollowMode.WANDER && super.canUse();
  }

  @Override
  public boolean canContinueToUse() {
    return agent.getFollowMode() == Utils.FollowMode.WANDER && super.canContinueToUse();
  }
}
