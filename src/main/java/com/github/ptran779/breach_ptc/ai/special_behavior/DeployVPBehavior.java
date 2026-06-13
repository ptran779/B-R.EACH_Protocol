package com.github.ptran779.breach_ptc.ai.special_behavior;

import com.github.ptran779.breach_ptc.ai.api.CoolDownBehavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.entity.extra.VectorPursuer;
import com.github.ptran779.breach_ptc.item.VPTerminalItem;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.server.EntityInit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

import static com.github.ptran779.breach_ptc.config.ServerConfig.VP_MIN_TARGET_HEALTH;

public class DeployVPBehavior extends CoolDownBehavior {
	AbsAgentEntity agent;
	LivingEntity target;
	int tickProgress = 0;
	public DeployVPBehavior(AbsAgentEntity entity, int baseCooldown, int varCooldown, int actionCoolDown) {
		super(entity, baseCooldown, varCooldown, actionCoolDown);
		this.agent = entity;
	}

	public boolean canUse() {
		if (!super.canUse()) return false;
		if ((agent.getControlFlg1() & EntityUtils.BF_ALLOW_SPECIAL) == 0 || !agent.inAggressive() ||
			!(agent.getSpecialSlot().getItem() instanceof VPTerminalItem)) {
			return false;
		}
		target = agent.getTarget();
		return target != null && target.isAlive() && target.getHealth() >= VP_MIN_TARGET_HEALTH.get();
	}

	public boolean canKeepRun() {
		return (target != null && target.isAlive() && agent.getSpecialSlot().getItem() instanceof VPTerminalItem);
	}
	public void start() {
		agent.setAniMoveStatic(AnimationID.A_VP_DEPLOY);
		PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),
			new EntityRenderPacket(agent.getId(), 1));
		tickProgress = agent.tickCount;
	}
	public void stop() {
		target = null;
		agent.setAniMoveStatic(AnimationID.A_LIVING);
	}
	public boolean run() {
		if (!canKeepRun()) return true;
		int dummy = agent.tickCount - tickProgress;
		if (dummy == 5) {
			agent.equipSpecial(true);
			agent.level().playSound(null, agent.getX(), agent.getY(), agent.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
				SoundSource.PLAYERS, 1.0F, 1.25F);
		} else if (dummy == 8 || dummy == 10 || dummy == 12 || dummy == 15 || dummy == 17 || dummy == 20 || dummy == 22 || dummy == 25) {
			agent.level().playSound(null, agent.getX(), agent.getY(), agent.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
				SoundSource.PLAYERS, 1.0F, 1.25F);
		} else if (dummy == 30) {
			resetActionCoolDown();
			VectorPursuer drone = new VectorPursuer(EntityInit.VECTOR_PURSUER.get(), agent.level());
			drone.bossUUID = agent.getBossUUID();
			agent.level().playSound(null, agent.getX(), agent.getY(), agent.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
				SoundSource.PLAYERS, 1.0F, 1.25F);

			drone.setPos(agent.getX(), agent.getY() + 1, agent.getZ());
			drone.deployer = agent;
			drone.setTarget(target);
			agent.level().addFreshEntity(drone);
			agent.getSpecialSlot().shrink(1);
		} else return dummy >= 40;
		return false;
	}
}
