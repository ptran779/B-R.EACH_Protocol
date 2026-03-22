package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.special_behavior.HealFriendlyBehavior;
import com.github.ptran779.breach_ptc.entity.agent.Medic;

import java.util.ArrayList;

public class MedicBrain extends AbsAgentBrain {
	protected Medic medicAgent;
	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 16;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	int healNearbyB;
	protected void behaviorInit(){
		super.behaviorInit();
		healNearbyB = addBehavior(new HealFriendlyBehavior(medicAgent, 80, 40, friendlyShortRS));
	}

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Medic, MedicBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Medic, MedicBrain>> CUSTOM_F = new ArrayList<>();
	public void evalSensorF(float[] state, long mask){
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = SENSOR_F.get(i).eval(medicAgent, this);;
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	};
	public void evalCustF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = CUSTOM_F.get(i).eval(medicAgent, this);
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

	public MedicBrain(Medic agent) {
		super(agent);
		this.medicAgent = agent;
		// from constant recompute?

		sensorInit();
		behaviorInit();
		thresholdInit();
	}

	protected double getShortRangeScan() {return 16;}
	protected double getLongRangeScan() {return 24;}
	protected double getShootingRange() {return 20;}

	public void activateGoalWrapper() {
		super.activateGoalWrapper();
		medicAgent.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(healNearbyB), false));
	}
	protected void packInput(float[] modArr) {
		for (int i = 0; i < SENSOR_F.size(); i++) {
			modArr[i] = SENSOR_F.get(i).eval(this.medicAgent, this);
		}
	}  // update payload
}