package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import static com.github.ptran779.breach_ptc.Utils.findNearestGround;

public class TakeCoverBehavior extends ThrottleBehavior {
	protected AbsAgentEntity agent;
	LivingEntity target;
	Sensor<LivingEntity> retarHostileS;
	protected int minRangeSq;  // if more than range blockSq, assume range target
	protected double searchRange;
	Path safepath;
	BlockPos safepos;

	public TakeCoverBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown,
	                         Sensor<LivingEntity> retarHostileS, int minRange, double coverSearchRange) {
		super(baseCooldown, varCooldown, agent);
		this.agent = agent;
		this.retarHostileS = retarHostileS;
		this.minRangeSq = minRange * minRange;
		this.searchRange = coverSearchRange;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		target = retarHostileS.get(agent.tickCount);

		if (target == null || !isRangeEnemy()) return false;
		ServerLevel level = (ServerLevel) agent.level();
		// check current raycast to see if agent in danger or already safe
		if (Utils.rayCastHit(agent.getEyePosition(), target.getEyePosition(), level)) return false;
		safepath = findCoverRandomly(level);
		return safepath != null;
	}

	public boolean canUseGoal() {
		return false;
	}

	protected boolean isRangeEnemy(){
		DamageSource source = agent.getLastDamageSource();
		if (source != null && source.is(DamageTypeTags.IS_PROJECTILE)) return true;
		return agent.distanceToSqr(target) > minRangeSq;
	}

	protected Path findCoverRandomly(ServerLevel level) {
		BlockPos agentBlockPos = agent.blockPosition();
		// 25 Trials
		for (int i = 0; i < 25; i++) {
			// 1. Pick a random X/Z within radius
			double rx = (agent.getRandom().nextDouble() - 0.5) * 2 * searchRange;
			double rz = (agent.getRandom().nextDouble() - 0.5) * 2 * searchRange;

			// 2. Vertical Scan: Find valid ground (Solid floor + 2 Air blocks)
			// We scan +-5 blocks from the random spot
			BlockPos candidate = agentBlockPos.offset((int)rx, 0, (int)rz);
			safepos = findNearestGround(candidate, agent, 5);
			if (safepos == null) continue;
			Vec3 safeEyePos = Vec3.atBottomCenterOf(safepos).add(0, agent.getEyeHeight(), 0);
			// Raycast From enemy to position.
			if (!Utils.rayCastHit(safeEyePos, target.getEyePosition(), level)) continue;
			// Raycast From agent to position
			if (Utils.rayCastHit(safeEyePos, agent.getEyePosition(), level)) continue;

			// 4. Reachability Check
			Path path = agent.getNavigation().createPath(safepos,0);
			if (path == null) continue;
			return path; // Found it. Done.
		}
		return null;
	}

	public void start(){
		agent.setAniMoveStatic(AnimationID.A_LIVING);
		agent.getNavigation().moveTo(safepath, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
		safepath = null;
	}

	@Override
	public boolean run() {return !agent.moveto(safepos.getCenter(), agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());}

	public void stop(){
		safepos = null;
		agent.stopNav();
	}

	public String toString(){return "Take Cover B";}
}
