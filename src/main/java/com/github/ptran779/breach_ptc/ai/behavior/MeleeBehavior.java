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
		if (target == null || !target.isAlive()) {return false;}
		targetRS = targetDistS.get(agent.tickCount);
		if (targetRS < 0) return false;
		return targetRS <= dropRS && (meleeIS.get(agent.tickCount) > 1F);
	}

	public void start() {
    super.start();
		dummy = agent.tickCount;
		aniflag = false;
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
				return !agent.moveto(target, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue()); // cant reach target  fixme check this bullshit
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
				else if (tProg == 50) tryHit();
				else if (tProg > 60) resetToIdle(tProg);
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
    agent.setDeltaMovement(look.x * 0.4, agent.getDeltaMovement().y, look.z * 0.4);
	}

	private boolean tryHit() {
		if (targetRS < getAttackReachSqr(target)) {
			agent.doHurtTarget(target);
			agent.getMainHandItem().hurtAndBreak(1, agent, (e) -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
			return true;
		}
		return false;
	}

	public String toString(){return "Melee B";}
}