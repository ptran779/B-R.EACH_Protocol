package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class SaluteBehavior extends ThrottleBehavior {
  protected int saluteTimer = 0;
  protected boolean flag = false;  // if boss already been seen within x radius
  protected Sensor<Player> bossS;
  protected Player boss;
  protected AbsAgentEntity agent;

  public SaluteBehavior(AbsAgentEntity agent, int cooldown, int varCooldown, Sensor<Player> bossS) {
    super(cooldown, varCooldown, agent);
    this.agent = agent;
    this.bossS = bossS;
  }

  @Override
  public boolean canUse() {
//    if (flag) return agent.tickCount - saluteTimer < 60;
    if (!super.canUse()) return false;
    if (agent.getBossUUID() == null) return false;

    boss = bossS.get(agent.tickCount);
    if (flag) {
      if (boss == null) {
				flag = false;
			}
      return false;
    }
    return boss != null && boss.isAlive();
  }

	@Override
  public boolean run() {
    if (boss == null || !boss.isAlive()) {return true;}
    agent.getLookControl().setLookAt(boss);
    return agent.tickCount - saluteTimer >= 60;
  }

  public void start(){
    flag = true;
    saluteTimer = agent.tickCount;
    agent.stopNav();
    agent.setAniMoveStatic(AnimationID.A_SALUTE);
    PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> agent), new EntityRenderPacket(agent.getId(), 1));
  }

  public void stop() {
    super.stop();
    agent.setAniMoveStatic(AnimationID.A_LIVING);
  }

	public String toString(){return "Salute B";}
}
