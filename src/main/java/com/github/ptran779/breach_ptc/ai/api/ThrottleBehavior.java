package com.github.ptran779.breach_ptc.ai.api;

import net.minecraft.world.entity.Entity;

// it throttle can use check, not the actual logic, else wtf do we have this :)
public abstract class ThrottleBehavior extends Behavior {
	protected final int baseCooldown;
	protected final int varCooldown;
	protected final Entity entity;

	private int nextAvailableTick = 0; // The "Target" time
	/**
	 * @param baseCooldown The fixed minimum ticks before this behavior can run again.
	 * @param varCooldown The noise amplitude (0 to varCooldown). Added to base for variability.
	 * @param entity The entity executing this behavior.
	 */
	public ThrottleBehavior(int baseCooldown, int varCooldown, Entity entity) {
		this.entity = entity;
		this.baseCooldown = baseCooldown;
		this.varCooldown = varCooldown;
		// Start with a random offset so 50 agents don't all "wake up" at once
		this.nextAvailableTick = entity.tickCount + generateOffset();
	}

	@Override
	public boolean canUse() {
		boolean out = entity.tickCount >= nextAvailableTick;
		if (out) {this.nextAvailableTick = entity.tickCount + generateOffset();}
		return out;
	}

	protected int generateOffset() {
		if (varCooldown <= 0) return baseCooldown;

		// Random centered around baseCooldown
		// Range: (base - var/2) to (base + var/2)
		int noise = entity.level().random.nextInt(varCooldown + 1) - (varCooldown / 2);
		return Math.max(0, baseCooldown + noise); // Safety check: never negative
	}
}
