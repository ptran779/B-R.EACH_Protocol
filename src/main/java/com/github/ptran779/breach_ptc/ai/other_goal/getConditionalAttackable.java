package com.github.ptran779.breach_ptc.ai.other_goal;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.entity.structure.DBTurret;
import net.minecraft.world.entity.LivingEntity;

public class getConditionalAttackable extends ThrottleBehavior {
	Sensor<LivingEntity> targetSensor;
	Sensor<Boolean> hasLOSToCurTarget;
	protected LivingEntity target;
	protected DBTurret turret;
	protected float dropRangeSq;
	int lastSeenTick = 0;
	public getConditionalAttackable(DBTurret user, int baseCooldown, int varCooldown, float dropRange,
	                                Sensor<LivingEntity> targetableSensor, Sensor<Boolean> hasLOSToCurTarget) {
		super(user, baseCooldown, varCooldown);
		this.turret = user;
		dropRangeSq = dropRange*dropRange;
		this.targetSensor = targetableSensor;
		this.hasLOSToCurTarget = hasLOSToCurTarget;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;

		target = targetSensor.get(turret.tickCount);
		return target != null;
	}

	public boolean canUseGoal() {
		if (!super.canUse()) return false;

		LivingEntity currentTarget = turret.getTarget();
		if (currentTarget != null && currentTarget.isAlive() && currentTarget.distanceToSqr(turret) < dropRangeSq) {
			// LOS check
			if (hasLOSToCurTarget.get(turret.tickCount)) {
				lastSeenTick = turret.tickCount;
				return false;
			}
			if (turret.tickCount - lastSeenTick < 40) {return false;}
		}

		turret.setTarget(null);
		target = targetSensor.get(turret.tickCount);
		return target != null;
	}

	public void start(){
		turret.setTarget(target);
		target = null;
		lastSeenTick = turret.tickCount;
	}

	@Override
	public boolean run() {
		return true;
	}

	public String toString(){return "Nearest Tar Aqr B";}
}
