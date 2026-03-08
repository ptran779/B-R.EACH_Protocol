package com.github.ptran779.breach_ptc.client.animation;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.github.ptran779.breach_ptc.client.animation.AgentLivingAnimation.*;
import static com.github.ptran779.breach_ptc.client.animation.AgentSpecialAnimation.BONK;
import static com.github.ptran779.breach_ptc.client.animation.AnimationID.*;

@OnlyIn(Dist.CLIENT)
public class AnimationLibrary {
  public static final AnimationDefinition[] AGENT_ANI = new AnimationDefinition[32];
  static {
    register();
  }
  public static void register() {
    AGENT_ANI[A_IDLE]= IDLE;
    AGENT_ANI[A_WALK]= WALK;
	  AGENT_ANI[A_ROTATE] = ROTATE;
    AGENT_ANI[A_RUN]= RUN;
    AGENT_ANI[A_RELOAD]= RELOAD;
    AGENT_ANI[A_EATING]= EATING;
    AGENT_ANI[A_SALUTE]= SALUTE;
    AGENT_ANI[A_TRIPLE_STRIKE]= TRIPLE_STRIKE;
	  AGENT_ANI[A_SWORD_DRAW]= SWORD_DRAW;
	  AGENT_ANI[A_GUN_DRAW]= GUN_DRAW;
	  AGENT_ANI[A_STATION_RELOAD] = STATION_RELOAD;
    AGENT_ANI[A_BONK]= BONK;
  }

  public static AnimationDefinition get(int id) {
      return AGENT_ANI[id];
  }
}