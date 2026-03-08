package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Behavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FollowBehavior extends Behavior {
  protected AbsAgentEntity agent;
  protected LivingEntity followTarget;
  protected int maxDistanceSq;
  protected int minDistanceSq;
  protected int targetDistanceSq;

  public FollowBehavior(AbsAgentEntity agent, int maxDistance, int minDistance, int targetDistance) {
    this.agent = agent;
    this.maxDistanceSq = maxDistance*maxDistance;
    this.minDistanceSq = minDistance*minDistance;
    this.targetDistanceSq = targetDistance*targetDistance;
  }

	@Override
  public boolean canUse() {
    if((agent.getControlFlg1() & AbsAgentEntity.BF_FOLLOW)==0 || agent.followPlayer == null) return false;
    this.followTarget = ((ServerLevel) agent.level()).getServer().getPlayerList().getPlayer(agent.followPlayer);
    if (followTarget == null) return false;
    double dist = agent.distanceToSqr(followTarget);
    return maxDistanceSq > dist && dist > minDistanceSq;
  }

	public void start() {agent.setAniMoveStatic(AnimationID.A_LIVING);}
	public void stop(){
		agent.stopNav();
	}
	@Override
  public boolean run() {
	  if((agent.getControlFlg1() & AbsAgentEntity.BF_FOLLOW)==0 || agent.followPlayer == null || followTarget == null || !followTarget.isAlive()) return true;
    double dist = agent.distanceToSqr(followTarget);
		if (maxDistanceSq < dist || dist < targetDistanceSq) return true;
    agent.moveto(followTarget, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
		return false;
  }


	public String toString(){return "Follow B";}
}
