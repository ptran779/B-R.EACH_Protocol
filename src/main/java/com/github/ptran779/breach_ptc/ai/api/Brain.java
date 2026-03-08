package com.github.ptran779.breach_ptc.ai.api;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public abstract class Brain {
  // for main sensor, put in order and hard code the order somewhere. It is way faster.
  protected LivingEntity brainEntity;
	protected int lastUpdateTick=0;
	protected boolean forceTrigger = false;

  protected final ArrayList<Behavior> behaviors;
  protected int activeBehaviours=-1;
  protected int lastRunBehaviours=-1;

	public Brain(LivingEntity brainEntity) {
		behaviors = new ArrayList<>();
		this.brainEntity = brainEntity;
	}

	public void flushUpdate(){lastUpdateTick = brainEntity.tickCount;}
	public boolean isRunning(){
    return activeBehaviours>-1;
  }
	protected void startBehavior(int i){
    activeBehaviours = i;
  }
	protected boolean tryBehavior(int i){
		if (behaviors.get(i).canUse()){
			startBehavior(i);
			return true;
		}
		return false;
	}

	// try all behavior index order until hit executable
	public void tryBehaviorChain(int[] idx){
		int chainMax = idx.length;
		for (int i=0; i<chainMax; i++){
			if (tryBehavior(i)) {
				break;
			};
		}
	}
  // behavior stuff
  public int addBehavior(Behavior behavior) {
    behaviors.add(behavior);
    return behaviors.size() - 1;
  }

	public void onBehaviorStop(int stopB){}
	public void onBehaviorStart(int startB){flushUpdate();}

  public abstract void preTick();  // decision picking -- highly asyc
	public void postTick() {
		// 1. Handle stopping the OLD behavior if the Brain switched tasks
		if (lastRunBehaviours != activeBehaviours && lastRunBehaviours != -1) {
			onBehaviorStop(lastRunBehaviours);
			behaviors.get(lastRunBehaviours).stop();
		}
		// 2. Handle starting the NEW behavior
		if (lastRunBehaviours != activeBehaviours && isRunning()) {
			behaviors.get(activeBehaviours).start();
			onBehaviorStart(activeBehaviours);
		}
		// 3. ALWAYS run the active behavior (even if it just started this exact tick)
		if (isRunning() && behaviors.get(activeBehaviours).run()) {
			/// push data here
			onBehaviorStop(activeBehaviours);
			behaviors.get(activeBehaviours).stop();
			activeBehaviours = -1; // Drop to idle immediately
			// require immediate eval regardless of sensor
			this.forceTrigger = true;
		}
		// 4. Update history for the next game tick
		lastRunBehaviours = activeBehaviours;
	}

  public void tick(){
    preTick();
    postTick();
  }
}