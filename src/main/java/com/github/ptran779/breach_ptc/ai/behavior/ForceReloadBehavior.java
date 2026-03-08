package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Behavior;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

public class ForceReloadBehavior extends Behavior {
  protected AbsAgentEntity agent;
  protected Sensor<Integer> ammoInChamberS, maxAmmoS;
	protected int dummy = 0;
	protected boolean reloading = false;
  public ForceReloadBehavior(AbsAgentEntity agent, Sensor<Integer> ammoInChamberS, Sensor<Integer> maxAmmoS) {
    this.agent = agent;
    this.ammoInChamberS = ammoInChamberS;
		this.maxAmmoS = maxAmmoS;
  }

	@Override
	public boolean canUse() {
		return agent.inventory1.gunExist() &&
			ammoInChamberS.get(agent.tickCount) < maxAmmoS.get(agent.tickCount) &&
			(agent.getVirtualAmmo() > 0 && agent.inventory1.findGunAmmo(agent.getGunStack())>-1);
	}

	public void start(){
		reloading = false;
		dummy = agent.tickCount;
	}

	@Override
  public boolean run() {
		if (!agent.inventory1.gunExist()) return true;
		if (!(agent.pullWeapon(2, agent.tickCount - dummy))) return false;
		if (!reloading){
			reloading = true;
			dummy = agent.tickCount;
			PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
			agent.setAniMoveStatic(AnimationID.A_RELOAD);
			return false;
		}
		int tDelta = agent.tickCount - dummy;
		// Timer counts down from 70 to 0.
		if (tDelta == 1) {  // Old mag drop
			agent.level().playSound(null, agent, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 2f, 0.5f);
		} else if (tDelta == 20) { // Shove in new mag
			agent.level().playSound(null, agent, SoundEvents.LADDER_HIT, SoundSource.BLOCKS, 2f, 1.8f);
		} else if (tDelta == 35) { // Lock in new mag
			agent.level().playSound(null, agent, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 2f, 2f);
		} else if (tDelta == 50) { // Prime the gun
			agent.level().playSound(null, agent, SoundEvents.CROSSBOW_LOADING_END, SoundSource.BLOCKS, 2f, 1f);
		} else if (tDelta >= 70) { // Ammo in!
			agent.executeAmmoReloadMath();
			return true;
		}
		return true; // Still reloading, keep the behavior alive
  }

  public void stop(){
    agent.setAniMoveStatic(AnimationID.A_LIVING);
  }

	public String toString(){return "Force Reload B";}
}
