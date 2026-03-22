package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.ai.api.CoolDownBehavior;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.entity.extra.Grenade;
import com.github.ptran779.breach_ptc.item.GrenadeItem;
import com.github.ptran779.breach_ptc.item.VPTerminalItem;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.server.EntityInit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import static com.github.ptran779.breach_ptc.config.ServerConfig.VP_MIN_TARGET_HEALTH;

public class GrenadeBehavior extends CoolDownBehavior {
	public final double[] THROW_ANGLES = {15, 30, 45, 60, 75};
	AbsAgentEntity agent;

	int tickProgress = 0;
	public GrenadeBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, int actionCoolDown) {
		super(agent, baseCooldown, varCooldown, actionCoolDown);
		this.agent = agent;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 ||
			(
				(agent.getControlFlg1() & EntityUtils.BF_TARGET_HOSTILE) == 0 &&
				(agent.getControlFlg1() & EntityUtils.BF_TARGET_AGENT) == 0)
		) return false;
		if (!(agent.getSpecialSlot().getItem() instanceof GrenadeItem grenadeItem)) return false;
		LivingEntity target = agent.getTarget();
		return target !=null && target.isAlive() && grenadeItem.shouldUseCheck(agent, target);
	}

	public void start() {
		tickProgress = agent.tickCount;
		agent.setAniMoveStatic(AnimationID.A_THROW_GRENADE);
		PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
			new EntityRenderPacket(agent.getId(), 1));
		agent.setAggressive(true);
	}

	public void stop() {
		agent.setAniMoveStatic(AnimationID.A_LIVING);
		agent.setAggressive(false);
	}

	public boolean canKeepRun(LivingEntity target) {
		return target != null && target.isAlive() && agent.getSpecialSlot().getItem() instanceof GrenadeItem;
	}

	public boolean run() {
		LivingEntity target = agent.getTarget();
		if (!canKeepRun(target)) return true;

		agent.getLookControl().setLookAt(target);
		int dummy = agent.tickCount - tickProgress;

		if (dummy == 3) {
			agent.equipSpecial(false);
		} else if(dummy == 20){
			agent.level().playSound(null, agent, SoundEvents.SHULKER_BOX_OPEN, SoundSource.NEUTRAL, 1.0F, 1.2F);
		} else if (dummy == 25) {
			agent.getSpecialSlot().getOrCreateTag().putLong("DeployTick", agent.level().getGameTime());
		} else if(dummy == 45){
			agent.level().playSound(null, agent, SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F);
		} else if (dummy == 50) {
			resetActionCoolDown();
			computeAndThrow(target);
			agent.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		} else if (dummy == 60) {
			agent.setAniMoveTransition(AnimationID.A_THROW_GRENADE, AnimationID.A_IDLE, 3f, 0f, 0.5f);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
		} else return dummy >= 70;
		return false;
	}

	private boolean arcClearCheck(Vec3 from, Vec3 flat, double angle, double speed, LivingEntity agent) {
		double pitch = Math.toRadians(angle);
		double vx = Math.cos(pitch) * speed;
		double vy = Math.sin(pitch) * speed;

		Vec3 pos = from;
		Vec3 vel = new Vec3(flat.x * vx, vy, flat.z * vx);

		for (int t = 0; t < 60; t += 3) {
			Vec3 next = pos.add(vel);
			if (agent.level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, agent))
				.getType() != HitResult.Type.MISS) {
				return false;
			}
			vel = new Vec3(vel.x, vel.y + Grenade.GRAVITY, vel.z);
			pos = next;
		}
		return true;
	}

	private void computeAndThrow(LivingEntity target) {
		// compute velocity depend on target length away
		Vec3 from = agent.getEyePosition();
		Vec3 delta = target.getEyePosition().subtract(from);
		double dxz = Math.hypot(delta.x, delta.z);
		if (dxz < 1e-6) return;

		Vec3 flat = new Vec3(delta.x / dxz, 0, delta.z / dxz);
		double speedNeeded = -1, angleNeeded = 0;

		for (double angle : THROW_ANGLES) {
			// 1) solve for speed at actual angle
			double pitch = Math.toRadians(angle);
			double cosP = Math.cos(pitch);
			double tanP = Math.tan(pitch);
			double denom = 2 * (dxz * tanP - delta.y) * cosP * cosP;
			if (denom <= 0) continue;
			double v2 = -Grenade.GRAVITY * dxz * dxz / denom;
			if (v2 <= 0) continue;
			double speed = Math.sqrt(v2);
			if (speed > GrenadeItem.MaxThrowSpeed) continue;

			// 2) arc check with computed speed
			if (!arcClearCheck(from, flat, angle, speed, agent)) continue;

			// both passed
			speedNeeded = speed;
			angleNeeded = angle;
			break;
		}

		if (speedNeeded > 0) {
			Vec3 throwDir = new Vec3(flat.x * Math.cos(Math.toRadians(angleNeeded)), Math.sin(Math.toRadians(angleNeeded)),
				flat.z * Math.cos(Math.toRadians(angleNeeded))).normalize();
			Grenade g = new Grenade(EntityInit.GRENADE.get(), agent.level());
			g.setPos(agent.getX(), agent.getEyeY() - 0.1, agent.getZ());
			g.setGrenadeType(((GrenadeItem) agent.getSpecialSlot().getItem()).getGrenadeType());
			g.setDeltaMovement(throwDir.scale(speedNeeded));
			agent.level().addFreshEntity(g);
			agent.getSpecialSlot().shrink(1);
			agent.getSpecialSlot().getOrCreateTag().remove("DeployTick");
		}
	}

	public String toString() {return "Grenade B";}
}