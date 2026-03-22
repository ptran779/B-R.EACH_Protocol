package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.CombatBehavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class MeleeBehavior extends CombatBehavior {
  protected Sensor<Float> meleeIS;
	protected Sensor<Double> targetDistS;
	protected LivingEntity target;
  protected double meleeRS;
  protected int sCount = -1;
	protected int lastStuckTick=-PATH_COOLDOWN_FAILURE;  // if melee fails to reach target due to pathing, block it from running for a short
	// time
	public static final int PATH_COOLDOWN_FAILURE = 60;

  private int dummy;
	private boolean aniflag = false;  // animation flag, use to reset

  protected double getAttackReachSqr(LivingEntity target) {return Math.pow((agent.getBbWidth() + target.getBbWidth())/2+2, 2);}

  public MeleeBehavior(AbsAgentEntity agent, double speedScale, double meleeR, double dropR, Sensor<Float> meleeIS,
                       Sensor<Double> distS) {
    super(agent, dropR, speedScale);
    this.meleeIS = meleeIS;
    this.meleeRS = meleeR*meleeR;
		this.targetDistS = distS;
  }

  protected void fixRotation(){  // rotate the body identical to head to avoid award calculation
    float snapYaw = agent.getYHeadRot();
    agent.setYRot(snapYaw);
    agent.setYBodyRot(snapYaw);
  }

	public boolean canUse(){
		target = agent.getTarget();
		if (agent.tickCount - lastStuckTick < PATH_COOLDOWN_FAILURE){return false;}
		if (target == null || !target.isAlive() || !agent.inAggressive()) {return false;}
		targetRS = targetDistS.get(agent.tickCount);
		if (targetRS < 0) return false;
		return targetRS <= dropRS && (meleeIS.get(agent.tickCount) > 1F);
	}

	public void start() {
    super.start();
		lastStuckTick = 0;
		dummy = agent.tickCount;
		aniflag = false;
		sCount = -1;
  }
	public void stop(){
		agent.setAggressive(false);
		agent.stopNav();
		agent.setAniMoveStatic(AnimationID.A_LIVING);
//		resetToIdle(agent.tickCount-dummy);
	}

	public boolean run() {
    if (!canUse()) return true;
		int tProg = agent.tickCount - dummy;
		if (!(agent.pullWeapon(1, tProg))) return false;

		// stop if under cooldown
		this.agent.getLookControl().setLookAt(target);
		if (attackCoolDown > 0) {
			attackCoolDown--;
			return false;
		} else if (aniflag) {
			agent.setAniMoveStatic(AnimationID.A_LIVING);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
			aniflag = false;
		}

		// approach target if far
		if (sCount == -1) {
			if (targetRS > meleeRS){
				if (!agent.moveto(target, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue())){
					lastStuckTick = agent.tickCount;
					return true;
				} else {
					return false;
				}
			}
			else {
				agent.stopNav();
				dummy = agent.tickCount;
				agent.setAniMoveStatic(AnimationID.A_TRIPLE_STRIKE);
				PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
				sCount = 0;
			}
			return false;
		}
		// in multi hit series
		tProg = agent.tickCount - dummy;
		fixRotation();
		switch (sCount) {
			case 0:
				if (tProg == 8) forwardDash();
				else if (tProg == 10 && tryHit()){sCount++;}
				else if (tProg > 20) resetToIdle(tProg); // Missed window
				break;
			case 1:
				if (tProg == 28) forwardDash();
				else if (tProg == 30 && tryHit()) {sCount++;}
				else if (tProg > 40) resetToIdle(tProg);
				break;
			case 2:
				if (tProg == 48) forwardDash();
				else if ((tProg == 50) && tryHit()) {sCount++;}  // not really strike, but for back hop
				else if (tProg > 60) resetToIdle(tProg);
				break;
			case 3:
				if (tProg == 60) backwardDash();
				else if (tProg >= 85) resetToIdle(tProg);
				break;
		}
		return false;
  }

	private void resetToIdle(int tProg) {
		resetCooldown();
		aniflag = true;
		sCount = -1;
		agent.setAniMoveTransition(AnimationID.A_TRIPLE_STRIKE, AnimationID.A_IDLE, tProg / 20f, 0, (attackCoolDown-5) / 20f); // tran back to pose
		PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
	}

	private void forwardDash() {
		Vec3 look = agent.getLookAngle();
    agent.setDeltaMovement(look.x * 0.45, 0, look.z * 0.45);
	}

	private void backwardDash(){
		Vec3 look = agent.getLookAngle();
		agent.setDeltaMovement(-look.x * 0.7, 0.3, -look.z * 0.7);
	}

	private boolean tryHit() {
		boolean hitAny = false;
		// sound effect
		if (sCount == 2) {agent.level().playSound(null, agent.getX(), agent.getY(), agent.getZ(),
			net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
		} else {agent.level().playSound(null, agent.getX(), agent.getY(), agent.getZ(),
				net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		// dmg manipulation for crit hit
		var attackAttr = agent.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attackAttr == null) return false; // Safety check
		double originalBaseDmg = attackAttr.getBaseValue();

		// --- MAIN TARGET CHECK ---
		if (targetRS < getAttackReachSqr(target)) {
			if (sCount == 2) {attackAttr.setBaseValue(originalBaseDmg * 1.5D);}
			if (agent.doHurtTarget(target)) {hitAny = true;}

			attackAttr.setBaseValue(originalBaseDmg);
		}

		// --- SECONDARY TARGETS (AoE) ---
		// Only process AABB casting if it's the 1st or 2nd hit
		if (sCount < 2) {
			double reachApprox = Math.sqrt(getAttackReachSqr(target));
			// Inflate the bounding box to grab everything nearby
			net.minecraft.world.phys.AABB hitBox = agent.getBoundingBox().inflate(reachApprox, 1.0D, reachApprox);

			// Get the normalized look vector to calculate the 90-degree arc
			Vec3 lookVec = agent.getLookAngle().normalize();
			var nearbyEntities = agent.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != agent && e != target && e.isAlive());
			for (LivingEntity e : nearbyEntities) {
				// Strict distance check just in case the bounding box grabbed a corner case
				if (agent.distanceToSqr(e) > getAttackReachSqr(e)) continue;
				if (agent.isAlly(e)) continue;

				Vec3 dirToEntity = e.position().subtract(agent.position()).normalize();
				if (lookVec.dot(dirToEntity) > 0.707) {
					double sweepMult = (sCount == 0) ? 0.25D : 0.50D;
					attackAttr.setBaseValue(originalBaseDmg * sweepMult);
					if (agent.doHurtTarget(e)) {hitAny = true;}
					attackAttr.setBaseValue(originalBaseDmg);
				}
			}
		}

		if (hitAny) {
			agent.getMainHandItem().hurtAndBreak(1, agent, (e) -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
			return true;
		}

		return false;
	}

	public String toString(){return "Melee B";}
}