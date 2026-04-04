package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.ai.api.CoolDownBehavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

public class PrecisionSnipeBehavior extends CoolDownBehavior {
	AbsAgentEntity agent;
	Sensor<Integer> ammoInChamberS;
	Sensor<Float> gunDmg;
	Sensor<Double> targetDistS;
	Sensor<Boolean> targetLosS, friendlyLOS;
	double dropRS;
	boolean prime;
	boolean cleanup;

	int tickProgress = 0;
	public PrecisionSnipeBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, int actionCooldown,
	                              double dropR, Sensor<Integer> ammoInChamberS, Sensor<Float> gunDmg,
	                              Sensor<Double> targetDistS, Sensor<Boolean> targetLosS, Sensor<Boolean> friendlyLOS) {
		super(agent, baseCooldown, varCooldown, actionCooldown);
		this.agent = agent;
		this.dropRS = dropR * dropR;
		this.ammoInChamberS = ammoInChamberS;
		this.gunDmg = gunDmg;
		this.targetDistS = targetDistS;
		this.targetLosS = targetLosS;
		this.friendlyLOS = friendlyLOS;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		// bit flag check
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 || !agent.inAggressive()) return false;
		// inventory check
		if (!agent.inventory1.gunExist() || ammoInChamberS.get(agent.tickCount) <= 0) return false;
		// entity check
		LivingEntity target = agent.getTarget();
		if (target == null || !target.isAlive() || targetDistS.get(agent.tickCount) > dropRS) return false;
		if (!targetLosS.get(agent.tickCount)) return false;
		return gunDmg.get(agent.tickCount) < agent.getTarget().getHealth();
	}
	public void start() {
		agent.setAggressive(true);
		tickProgress = agent.tickCount;
		prime = false;
		cleanup = false;
	}
	public void stop() {
		agent.postShoot();  // just in case
		agent.setAniMoveStatic(AnimationID.A_LIVING);
		agent.setAggressive(false);
	}
	public boolean canKeepRun(LivingEntity target) {
		return cleanup || (target != null && target.isAlive() && agent.inventory1.gunExist() && targetDistS.get(
			agent.tickCount) < dropRS);
	}
	// fixme: check animation due to gun holding TOO LAZY :( I"ll have ti redesign animation pipeline and it gonna be a
	//  pain in the ass
	public boolean run() {
		LivingEntity target = agent.getTarget();
		if (!canKeepRun(target)) return true;
		int dummy = agent.tickCount - tickProgress;
		if (!(agent.pullWeapon(2, dummy))) return false;  // pull the gun first
		agent.getLookControl().setLookAt(target);

		if (!prime) {
			prime = true;
			tickProgress = agent.tickCount;
			agent.setAniMoveStatic(AnimationID.A_PRECISION_SNIPE);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
				new EntityRenderPacket(agent.getId(), 1));
			agent.preShoot(true);
			return false;
		}

		if (dummy >= 20 && dummy < 60) {
			// Only run every 2 ticks to save performance, or every tick for a solid line
			if (agent.tickCount % 5 == 0) {
				spawnLaserParticles(target);
			}
		}

		if (dummy == 60) {
			// emergency cleanup if there a friendly in LOS OR target hiding behind wall
			if (!targetLosS.get(agent.tickCount) || friendlyLOS.get(agent.tickCount)) return true;
			agent.shootingTick();
			cleanup = true;
			resetActionCoolDown();
		} else if (dummy == 65) {
			target.hurt(agent.level().damageSources().mobAttack(agent),
				gunDmg.get(agent.tickCount) * 2);  // I dont have a way to modify bullet damage :)
		} else if (dummy == 70) {
			agent.setAniMoveTransition(AnimationID.A_PRECISION_SNIPE, AnimationID.A_IDLE, 3.5f, 0f, 0.5f);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
				new EntityRenderPacket(agent.getId(), 1));
			agent.postShoot();
		} else return dummy > 90;
		return false;
	}

	private void spawnLaserParticles(LivingEntity target) {
		Vec3 start = agent.getEyePosition();
		Vec3 direction = target.getEyePosition().subtract(start);
		double distance = direction.length();
		direction = direction.normalize();

		// Spawn a particle every 0.5 blocks along the line
		for (double i = 0; i < distance; i += 1) {
			Vec3 point = start.add(direction.scale(i));
			if (agent.level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 0.5F), point.x, point.y,
					point.z, 1, 0, 0, 0, 0);
			}
		}
	}

	public String toString() {return "Precision Snipe B";}
}