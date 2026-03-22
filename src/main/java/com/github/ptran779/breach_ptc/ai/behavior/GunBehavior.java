package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.CombatBehavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GunBehavior extends CombatBehavior {
  protected Sensor<Boolean> friendlyLOS, targetLOS;
	protected Sensor<Double> targetDistS;
	protected Sensor<Integer> ammoInChamberS, totalAmmoCountS;
	protected double shootRS;
	protected boolean firing = false;
	protected LivingEntity target;
	int dummy = 0;  // for animation purpose

  public GunBehavior(AbsAgentEntity agent, double speedScale, double shootR, int dropR, Sensor<Integer> ammoInChamberS,
                     Sensor<Integer> totalAmmoCountS, Sensor<Boolean> friendlyLOS, Sensor<Double> distS, Sensor<Boolean> targetLOS) {
    super(agent, dropR, speedScale);
		this.ammoInChamberS = ammoInChamberS;
		this.totalAmmoCountS = totalAmmoCountS;
    this.friendlyLOS = friendlyLOS;
    this.shootRS = shootR*shootR;
		this.targetDistS = distS;
  }

  public void start(){
    super.start();
	  dummy = agent.tickCount;
	  agent.preAmmoUsageCount();
  }

  public boolean canUse() {
	  target = agent.getTarget();
	  if (target == null || !target.isAlive() || !agent.inAggressive()) {return false;}
	  targetRS = targetDistS.get(agent.tickCount);
	  if (targetRS < 0) return false;
		if (Utils.rayCastHit(agent.getEyePosition(), target.getEyePosition(), (ServerLevel) agent.level())) return false;  // sensor fixme
	  return targetRS <= dropRS &&
		  agent.inventory1.gunExist() && // need this fast check due to cant throttle. Other sensor support slow throttle with resolution
		  (agent.getVirtualAmmo() > 0 || ammoInChamberS.get(agent.tickCount) > 0 || totalAmmoCountS.get(agent.tickCount) > 0);
//		  gunSensor.get(agent.tickCount);  // can upgrade to expensive check later
  }

  public boolean run() {
	  if (!canUse()) return true;
	  if (!(agent.pullWeapon(2, agent.tickCount - dummy))) return false;

		// fast look control in very close range due to knockback messup
	  if (targetDistS.get(agent.tickCount) < 2) {this.agent.getLookControl().setLookAt(target, 60, 60);}
		else this.agent.getLookControl().setLookAt(target);

	  if (attackCoolDown > 0) {
		  attackCoolDown--;
		  return false;
	  }

	  if (shootRS < targetRS) {
		  if (firing) {
			  firing = false;
			  agent.postShoot();
		  }
		  // nav
		  if (!agent.moveto(target, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue())){return true;};
	  } else {
		  agent.stopNav();
			if (friendlyLOS.get(agent.tickCount)) {
				resetCooldown();
				return false;
			}  // friendly check
		  if (!firing) {
			  if (agent.preShoot(true)){
				  firing = true;
			  } else {
				  resetCooldown();
			  }  // make it wait before try to pull/prep gun again
		  } else {
			  if (!agent.shootingTick()) {
				  firing = false;
				  dummy = agent.tickCount;
				  agent.postShoot();
				  resetCooldown();  // why not, just safety
			  }
		  }
	  }
	  return false;
  }

  public void stop(){
    super.stop();
    agent.postShoot();
	  firing = false;
  }

	public String toString(){return "Gun B";}
}
