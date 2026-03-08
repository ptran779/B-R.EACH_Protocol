package com.github.ptran779.breach_ptc.ai.api;

public abstract class Behavior {
//  public boolean active=false;
  public Behavior() {}

  public void start(){}
  public abstract boolean canUse();
  public abstract boolean run();  // set this to true when behavior is complete
  public void stop(){}
	public boolean canUseGoal(){return canUse();}

	public String toString(){return "Abstract B";}
}
