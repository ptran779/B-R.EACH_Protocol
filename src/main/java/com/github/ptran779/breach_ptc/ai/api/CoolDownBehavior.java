package com.github.ptran779.breach_ptc.ai.api;

import net.minecraft.world.entity.Entity;

// extend of throttle: instead of can use cap, it also have 2nd cap on action complete cooldown. Pretty useful for skill based cooldown
public abstract class CoolDownBehavior extends ThrottleBehavior{
	private int lastActive = 0;
	private final int actionCoolDown;
	public CoolDownBehavior(Entity entity, int baseCooldown, int varCooldown, int actionCoolDown) {
		super(entity, baseCooldown, varCooldown);
		this.actionCoolDown = actionCoolDown;
	}
	public boolean canUse() {
		return super.canUse() && entity.tickCount - lastActive >= actionCoolDown;
	}

	// must call this if an action success to reset the cooldown
	public void resetActionCoolDown(){
		lastActive = entity.tickCount;
	}
}
