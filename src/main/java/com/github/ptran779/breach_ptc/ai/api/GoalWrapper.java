package com.github.ptran779.breach_ptc.ai.api;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// special wrapper for behavior to convert to goal
public class GoalWrapper extends Goal {
	Behavior main;
	boolean interruptible;
	boolean exist = false;

	public GoalWrapper(Behavior main, boolean interruptible){
		this.main = main;
		this.interruptible = interruptible;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET)); // I just use most since the goal is pretty stupid
	}

	public boolean canUse() {return main.canUseGoal();}
	public boolean isInterruptable() {
		return interruptible;
	}
	public void start(){main.start();}
	public void stop(){main.stop();exist=false;}
	public boolean requiresUpdateEveryTick() {return true;}
	public void tick(){exist = main.run();};
	public boolean canContinueToUse() {return !exist;}
}
