package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.CoolDownBehavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.item.ModularShieldItem;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

import static com.github.ptran779.breach_ptc.client.animation.AnimationID.*;

public class ShieldChargeBehavior extends CoolDownBehavior {
	AbsAgentEntity agent;
	Sensor<List<LivingEntity>> hostileLongRS; //
	Sensor<Boolean> friendlyLOS; //
	double chargeRangeSq;
	LivingEntity target;
	int tickProgress;
	int actionStage;
	List<LivingEntity> nearMark;

	public ShieldChargeBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, int actionCoolDown,
	                            double chargeRange, Sensor<List<LivingEntity>> hostileLongRS, Sensor<Boolean> friendlyLOS) {
		super(agent, baseCooldown, varCooldown, actionCoolDown);
		this.agent = agent;
		this.chargeRangeSq = chargeRange * chargeRange;
		this.hostileLongRS = hostileLongRS;
		this.friendlyLOS = friendlyLOS;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		// bit flag check
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 || !agent.inAggressive()) return false;
		// inventory check
		if (!(agent.getSpecialSlot().getItem() instanceof ModularShieldItem)) return false;
		List<LivingEntity> hostileNearby = hostileLongRS.get(agent.tickCount);

		for (LivingEntity hostile : hostileNearby) {
			if (!(hostile instanceof Mob mob) || !hostile.isAlive() || agent.distanceToSqr(hostile) >= chargeRangeSq) continue;
			LivingEntity mobTarget = mob.getTarget();
			if (mobTarget != null && agent.isAlly(mobTarget) && !Utils.rayCastHit(agent.getEyePosition(), hostile.getEyePosition(),
				(ServerLevel) agent.level())) {
				target = hostile;
				return true;
			}
		}
		return false;
	}
	public void start() {
		agent.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		agent.setAggressive(true);
		tickProgress = agent.tickCount;
		actionStage = 0;
		nearMark = null;
		agent.getSpecialSlot().getOrCreateTag().remove("DeployTick");

		agent.setAniMoveStatic(AnimationID.A_SHIELD_DEPLOY);
		PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
			new EntityRenderPacket(agent.getId(), 1));
	}
	public void stop() {
		agent.postShoot();
		target = null;
		agent.setAniMoveStatic(A_LIVING);
		agent.setAggressive(false);
		agent.isShieldActive = false;  // critical shield off
	}
	public boolean canKeepRun() {
		return actionStage >= 2 || (target != null && target.isAlive() && agent.getSpecialSlot()
			.getItem() instanceof ModularShieldItem);
	}

	public boolean run() {
		if (!canKeepRun()) return true;
		if (target != null) agent.getLookControl().setLookAt(target);
		int dummy = agent.tickCount - tickProgress;
		switch (actionStage) {
			case 0: {
				if (dummy == 15) {
					agent.equipSpecial(true);
					agent.getSpecialSlot().getOrCreateTag().putLong("DeployTick", agent.level().getGameTime());
				} else if (dummy >= 30) {
					actionStage++;
					agent.setAniMoveStatic(A_SHIELD_CHARGE);
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
						new EntityRenderPacket(agent.getId(), 1));
					return false;
				}
				break;
			}
			case 1: {
				if (agent.distanceToSqr(target) > 4) {
					if (!agent.moveto(target, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue())) return true;
				} else {
					tickProgress = agent.tickCount;
					actionStage++;
					frontNearbyHostiles(6, 180).forEach(e -> ((Mob) e).setTarget(agent));  // taunt
					agent.isShieldActive = true;  // critical shield on
					resetActionCoolDown();  // perfect time to trigger this since it will draw target now
					//also, play some effect & sound
					agent.level().playSound(null, agent, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 1f);
					((ServerLevel) agent.level()).sendParticles(ParticleTypes.CRIT, agent.getX(), agent.getY() + 1, agent.getZ(),
						15, 0.3, 0.5, 0.3, 0.2);
					agent.setAniMoveStatic(A_SHIELD_BONK);
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
						new EntityRenderPacket(agent.getId(), 1));
					return false;
				}
				break;
			}
			case 2: {
				if (dummy == 20) {
					agent.level().playSound(null, agent, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1F, 1F);
					nearMark = frontNearbyHostiles(6, 180);

					// bash them
					nearMark.forEach(e -> {
						e.hurt(agent.damageSources().mobAttack(agent),
							(float) (4 + agent.getAttributeValue(Attributes.ATTACK_DAMAGE)));
						e.knockback(2F, agent.getX() - e.getX(), agent.getZ() - e.getZ()); // Knockback 2
					});
					// if gun here, lets kick their ass
					if (!agent.inventory1.gunExist()) {
						actionStage = 3;
						tickProgress = agent.tickCount;
						return false;
					}
				} else if (dummy == 30) {
					agent.equipGun();
					agent.preShoot(true);
				}
				// perform triple hit execution chain
				if (dummy > 30) {
					// quick redirection
					if (!target.isAlive()) {
						for (LivingEntity e : nearMark) {
							if (e != null && e.isAlive()) {
								target = e;
								break;
							}
						}
					}
					if (target == null || !target.isAlive()) {
						actionStage = 4;
						tickProgress = agent.tickCount;
						return false;
					} else if ((dummy == 45 || dummy == 55 || dummy == 65) && !friendlyLOS.get(agent.tickCount)) agent.shootGun(); // add safety check
					else if (dummy >= 75) {  // done
						actionStage = 4;
						tickProgress = agent.tickCount;
						return false;
					}
				}
				break;
			}
			case 3: {  // clean up
				if (dummy == 1) {
					agent.setAniMoveTransition(A_SHIELD_BONK, A_IDLE, 1.25f, 0, 1);
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
						new EntityRenderPacket(agent.getId(), 1));
				} else if (dummy >= 20) return true;  // done
				break;
			}
			case 4: {  // also clean up
				if (dummy == 1 ) {
					agent.setAniMoveTransition(A_SHIELD_BONK, A_IDLE, 3.75f, 0, 1);
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
						new EntityRenderPacket(agent.getId(), 1));
				} else if (dummy >= 20) return true;  // done
				break;
			}
			default:
				return true;
		}
		return false;
	}

	private List<LivingEntity> frontNearbyHostiles(float radius, float arcDegrees) {
		return agent.level().getEntitiesOfClass(LivingEntity.class, agent.getBoundingBox().inflate(radius),
			e -> e.isAlive() && !agent.isAlly(e) && e instanceof Mob mob && mob.getTarget() != null && agent.isAlly(
				mob.getTarget()) && Utils.isInFrontArc(agent, e.position(), arcDegrees));
	}
}
