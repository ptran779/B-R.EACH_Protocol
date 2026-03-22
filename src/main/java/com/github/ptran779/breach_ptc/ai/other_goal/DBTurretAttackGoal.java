package com.github.ptran779.breach_ptc.ai.other_goal;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.entity.structure.DBTurret;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.github.ptran779.breach_ptc.entity.structure.DBTurret.DEPLOYED;

public class DBTurretAttackGoal extends Goal {
  private final DBTurret turret;
  private final double maxRangeSq;
  private final int attackCoolDown;
  private int attackLastShot = 0;
  private int seeTime = 0;
	Sensor<Boolean> hasLOSToTarget;

	public DBTurretAttackGoal(DBTurret turret, double maxRange, int attackCoolDown, Sensor<Boolean> hasLOSToTarget) {
    this.setFlags(EnumSet.of(Flag.LOOK));

    this.turret = turret;
    this.maxRangeSq = maxRange*maxRange;
    this.attackCoolDown = attackCoolDown;
		this.hasLOSToTarget = hasLOSToTarget;
  }

  public boolean canUse() {
    return turret.getEntityData().get(DEPLOYED) && turret.getTarget() != null && turret.getTarget().isAlive() && WandECheck();
  }

  public boolean canContinueToUse() {
    return canUse() && seeTime >= -100;
  }

  private boolean WandECheck(){
    // ammo check here too
	  double targetDistSq = turret.distanceToSqr(turret.getTarget());
    return targetDistSq < maxRangeSq;
  }

  public void start() {
    turret.setAggressive(true);
    seeTime = 0;
    attackLastShot = turret.tickCount;
  }
  public void stop() {
    turret.setAggressive(false);
  }

  public boolean requiresUpdateEveryTick() {return true;}

  public void tick() {
    LivingEntity target = turret.getTarget();
    // if I clear target, it shit itself...
	  if (target == null || !target.isAlive() || !hasLOSToTarget.get(turret.tickCount)) {return;}
	  turret.getLookControl().setLookAt(target);
    // shoot the freak
    if (turret.tickCount - attackLastShot < attackCoolDown) return;
    turret.shoot();
	  attackLastShot = turret.tickCount;
  }
}
