package com.github.ptran779.breach_ptc.entity.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static com.github.ptran779.breach_ptc.entity.api.EntityUtils.BF_TARGET_AGENT;
import static com.github.ptran779.breach_ptc.entity.api.EntityUtils.BF_TARGET_HOSTILE;

// for agent and turret
public interface IEntityTeamNTarget {
	// team system
  default boolean isBoss(Player player) {
    if (getBossUUID() == null) {return false;}
    return (getBossUUID().equals(player.getUUID()));
  }
  default boolean isFriendlyPlayer(Player player, Level level) {
    // 1. No Boss? Not friendly.
    if (getBossUUID() == null) return false;
    // 2. Is the player the Boss? Friendly.
    if (getBossUUID().equals(player.getUUID())) return true;
    // 3. Does the TARGET player have a team? If no, cannot be teammates.
    if (player.getTeam() == null) return false;
    // 4. Is the BOSS online? If not, we can't check their team.
    Player boss = level.getPlayerByUUID(getBossUUID());
    if (boss == null) return false;
    // 5. Does the BOSS have a team?
    if (boss.getTeam() == null) return false;
    // 6. Compare teams
    return player.getTeam().getName().equals(boss.getTeam().getName());
  }
  default boolean isFriendlyMod(IEntityTeamNTarget teamer, Level level) {
    // 1. Missing UUIDs?
    if (getBossUUID() == null || teamer.getBossUUID() == null) return false;
    // 2. Same Boss? Friendly.
    if (getBossUUID().equals(teamer.getBossUUID())) return true;
    // 3. Get both Boss entities
    Player myBoss = level.getPlayerByUUID(getBossUUID());
    Player otherBoss = level.getPlayerByUUID(teamer.getBossUUID());

    // 4. Are both bosses online?
    if (myBoss == null || otherBoss == null) return false;
    // 5. Do both bosses have teams?
    if (myBoss.getTeam() == null || otherBoss.getTeam() == null) return false;
    // 6. Compare teams
    return myBoss.getTeam().getName().equals(otherBoss.getTeam().getName());
  }
	default boolean isAlly(LivingEntity entity){
		if (entity instanceof Player player) return isFriendlyPlayer(player, entity.level());
		if (entity instanceof IEntityTeamNTarget teamer) return isFriendlyMod(teamer, entity.level());
		return false;
	}
  UUID getBossUUID();

	// who to target
	int getControlFlg1();
	// quick dirty check to tell combat dependent behavior to stop itself if we no longer attacking mid combat
	default boolean inAggressive(){
		int flg = getControlFlg1();
		return (flg & BF_TARGET_HOSTILE) != 0 || (flg & BF_TARGET_AGENT) != 0;
	}
	default boolean shouldTargetEntity(IEntityTeamNTarget user, LivingEntity entity) {
		int flg = getControlFlg1();
		if ((flg & BF_TARGET_HOSTILE) != 0){
			if (entity instanceof Enemy) return true;
		}
		if ((flg & BF_TARGET_AGENT) != 0){
			if (entity instanceof Player player) return !user.isFriendlyPlayer(player, entity.level());
			if (entity instanceof IEntityTeamNTarget teamer) return  !user.isFriendlyMod(teamer, entity.level());
		}
		return false;
	}
	default boolean isPotentialHostile(IEntityTeamNTarget user, LivingEntity entity) {
		if (entity instanceof Enemy) return true;
		if (entity instanceof Player player) return !user.isFriendlyPlayer(player, entity.level());
		if (entity instanceof IEntityTeamNTarget teamer) return !user.isFriendlyMod(teamer, entity.level());
		return false;
	}
}
