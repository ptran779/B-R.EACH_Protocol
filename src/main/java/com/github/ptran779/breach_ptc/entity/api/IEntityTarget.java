package com.github.ptran779.breach_ptc.entity.api;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

// for agent and turret
public interface IEntityTarget {
	int getControlFlg1();
  default boolean shouldTargetEntity(IEntityTeam user, LivingEntity entity) {
		int flg = getControlFlg1();
	  if ((flg & AbsAgentEntity.BF_TARGET_HOSTILE) != 0){
      if (entity instanceof Enemy) return true;
	  }
	  if ((flg & AbsAgentEntity.BF_TARGET_AGENT) != 0){
		  if (entity instanceof Player player) return !user.isFriendlyPlayer(player, entity.level());
		  if (entity instanceof IEntityTeam teamer) return  !user.isFriendlyMod(teamer, entity.level());
	  }
	  return false;
  }
	default boolean isPotentialHostile(IEntityTeam user, LivingEntity entity) {
		if (entity instanceof Enemy) return true;
		if (entity instanceof Player player) return !user.isFriendlyPlayer(player, entity.level());
		if (entity instanceof IEntityTeam teamer) return !user.isFriendlyMod(teamer, entity.level());
		return false;
	}
}
