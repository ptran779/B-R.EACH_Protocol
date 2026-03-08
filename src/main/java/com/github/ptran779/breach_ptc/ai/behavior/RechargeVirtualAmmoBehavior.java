package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.structure.PortDisp;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

import static com.github.ptran779.breach_ptc.config.ServerConfig.VIRT_AMMO_REFILL;

public class RechargeVirtualAmmoBehavior extends ThrottleBehavior {
	AbsAgentEntity agent;
	Sensor<List<LivingEntity>> friendlyS;
	PortDisp target;
	int dummy;
	boolean recharging;
	public RechargeVirtualAmmoBehavior(AbsAgentEntity agent, int baseCooldown, int varCooldown, Sensor<List<LivingEntity>> friendlyS) {
		super(baseCooldown, varCooldown, agent);
		this.agent = agent;
		this.friendlyS = friendlyS;
	}

	@Override
	public boolean canUse() {
		if (!super.canUse()) return false;
		if (agent.getVirtualAmmo() >= agent.getMaxVirtualAmmo()){return false;}
		List<LivingEntity> all = friendlyS.get(agent.tickCount);
		for (LivingEntity entity : all){
			if (entity instanceof PortDisp portDisp){
				if (portDisp.charge >= agent.getAmmoPerCharge()) {
					target = portDisp;
					return true;
				}
			}
		}
		return false;
	}

	public boolean canUseGoal() {
		if (!super.canUse()) return false;
		if (agent.getVirtualAmmo() > agent.getMaxVirtualAmmo()*0.8){return false;}
		List<LivingEntity> all = friendlyS.get(agent.tickCount);
		for (LivingEntity entity : all){
			if (entity instanceof PortDisp portDisp){
				if (portDisp.charge >= agent.getAmmoPerCharge()) {
					target = portDisp;
					return true;
				}
			}
		}
		return false;
	}

	public void start(){
		agent.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		recharging = false;
	}
	public void stop(){
		target = null;
		agent.setAniMoveStatic(AnimationID.A_LIVING);
	}

	public boolean run() {
		if (target == null || !target.isAlive()) return true;
		agent.getLookControl().setLookAt(target);
		if (agent.distanceToSqr(target) > 3) {
			return !(agent.moveto(target, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue()));
		} else if (!recharging) {
			agent.stopNav();
			dummy = agent.tickCount;
			recharging = true;
			agent.setAniMoveStatic(AnimationID.A_STATION_RELOAD);
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent),new EntityRenderPacket(agent.getId(), 1));
			return false;
		} else {
			int tProg = agent.tickCount - dummy;
			if (tProg == 50 || tProg == 60 || tProg == 70) {
				agent.level().playSound(null, agent, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.BLOCKS, 1.2f, 0.5f);
			} else if (tProg >= 90) {
				((ServerLevel) agent.level()).sendParticles(ParticleTypes.END_ROD, agent.getX(), agent.getY()+1, agent.getZ(), 10, 0, 1, 0, 0.02);
				int reloadAmount = Math.min(Math.min(target.charge / agent.getAmmoPerCharge(), agent.getMaxVirtualAmmo() - agent.getVirtualAmmo()), VIRT_AMMO_REFILL.get());
				agent.setVirtualAmmo(agent.getVirtualAmmo() + reloadAmount);
				target.charge -= reloadAmount * agent.getAmmoPerCharge();
				return true;
			}
			return false;
		}
	}

	public String toString(){return "Recharge Virt Ammo B";}
}
