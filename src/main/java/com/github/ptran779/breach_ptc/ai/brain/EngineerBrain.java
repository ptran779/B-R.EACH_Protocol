package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.special_behavior.WorkOnStructureBehavior;
import com.github.ptran779.breach_ptc.entity.agent.Engineer;

import java.util.ArrayList;

public class EngineerBrain extends AbsAgentBrain {
	protected Engineer engineer;
	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 16;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	int workOnStructB;
	protected void behaviorInit(){
		super.behaviorInit();
		workOnStructB = addBehavior(new WorkOnStructureBehavior(engineer, 80, 40, friendlyShortRS));
	}

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Engineer, EngineerBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Engineer, EngineerBrain>> CUSTOM_F = new ArrayList<>();
	public void evalSensorF(float[] state, long mask){
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = SENSOR_F.get(i).eval(engineer, this);;
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	};
	public void evalCustF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = CUSTOM_F.get(i).eval(engineer, this);
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	}

	/// make all class do this itself
	static {
		registerBasedCommonReceptor(SENSOR_F);
		registerBasedCustomEval(CUSTOM_F);
		INPUT_SPACE = SENSOR_F.size();
		CUSTOM_SPACE = CUSTOM_F.size();
	}

	public EngineerBrain(Engineer agent) {
		super(agent);
		this.engineer = agent;

		// safety design: put this at very last of the branch
		sensorInit();
		behaviorInit();
		thresholdInit();
	}

	public void activateGoalWrapper() {
		super.activateGoalWrapper();
		engineer.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(workOnStructB), false));
	}
	protected double getShortRangeScan() {return 16;}
	protected double getLongRangeScan() {return 24;}
	protected double getShootingRange() {return 20;}

	protected void packInput(float[] modArr) {
		for (int i = 0; i < SENSOR_F.size(); i++) {
			modArr[i] = SENSOR_F.get(i).eval(this.engineer, this);
		}
	}
}