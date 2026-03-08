package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class RetreatBehavior extends ThrottleBehavior {
	AbsAgentEntity agent;
	Sensor<List<LivingEntity>> nearbyDangerS;
	float awayDist;
	float minDistPerRunSq;
	static final float[] FAN_ANG = new float[]{0,10,-10,20,-20,30,-30};
	Vec3 towardPos;
	boolean brainMode = false;

	public RetreatBehavior(int baseCooldown, int varCooldown, AbsAgentEntity agent, Sensor<List<LivingEntity>> nearbyDangerS, float awayDist, float minDistPerRun) {
		super(baseCooldown, varCooldown, agent);
		this.agent = agent;
		this.nearbyDangerS = nearbyDangerS;
		this.awayDist = awayDist;
		this.minDistPerRunSq = minDistPerRun*minDistPerRun;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		towardPos = getNextPosition();
		brainMode = true;
		return towardPos != null;
	}

	public boolean canUseGoal() {
		if (!super.canUse() || !shouldRunAway()) return false;
		towardPos = getNextPosition();
		brainMode = false;
		return towardPos != null;
	}

	protected boolean shouldRunAway(){
		return (agent.getHealth() < 8) && !nearbyDangerS.get(agent.tickCount).isEmpty();
	}

	protected Vec3 getNextPosition() {
		List<LivingEntity> dangerNear = nearbyDangerS.get(agent.tickCount);
		if (dangerNear.isEmpty()) return null;
		Vec3 finalDir = getPushVector(agent, dangerNear).normalize();
		Vec3 agentPos = agent.position();
		ServerLevel level = (ServerLevel) agent.level();
		for (float angle : FAN_ANG) {
			Vec3 testDir = finalDir.yRot((float) Math.toRadians(angle));
			Vec3 target = agentPos.add(testDir.scale(awayDist));

			BlockPos ground = Utils.findNearestGround(new BlockPos((int)target.x, (int)target.y, (int)target.z), agent, 5);

			// Raycast check: Can we actually get there in a straight line?
			if (ground != null &&
				!Utils.rayCastHit(agent.position().add(0,1,0), ground.above().getCenter(), level) &&  // can see target
				ground.getCenter().distanceToSqr(agent.getEyePosition()) >= minDistPerRunSq) {  // pass minimum movable
				return ground.getCenter();
			}
		}
		return null;
	}

	protected Vec3 getPushVector(LivingEntity user, List<LivingEntity> dangerNear){
		Vec3 agentPos = user.position();
		Vec3 pushVector = Vec3.ZERO;

		// 1. Calculate Weighted Repulsion
		for (LivingEntity enemy : dangerNear) {
			double dSq = user.distanceToSqr(enemy);
			// Inverse square weighting: closer is much heavier
			double weight = 1.0 / (dSq + 0.1);
			Vec3 away = agentPos.subtract(enemy.position()).normalize();
			pushVector = pushVector.add(away.scale(weight));
		}
		return pushVector;
	}

	@Override
	public boolean run() {
		if (towardPos == null) return true;
		boolean movin = agent.moveto(towardPos, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
		if (brainMode) return !movin;
		else if (!movin) towardPos = getNextPosition();
		return !shouldRunAway();
	}

	public void stop(){
		agent.stopNav();
	}

	public String toString(){return "Retreat B";}
}
