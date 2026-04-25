package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.special_behavior.PrecisionSnipeBehavior;
import com.github.ptran779.breach_ptc.entity.agent.Sniper;

import java.util.ArrayList;

public class SniperBrain extends AbsAgentBrain {
	protected Sniper sniperAgent;
	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 16;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	int precisionSnipeB;
	protected void behaviorInit() {
		super.behaviorInit();
		precisionSnipeB = addBehavior(
			new PrecisionSnipeBehavior(sniperAgent, 40, 10, 400, getLongRangeScan(), ammoInChamberS, gunDmgS, targetDistSqS,
				targetLosS, friendlyLosFastS));
	}

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Sniper, SniperBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Sniper, SniperBrain>> CUSTOM_F = new ArrayList<>();

	/// make all class do this itself
	static {
		registerBasedCommonReceptor(SENSOR_F);
		registerBasedCustomEval(CUSTOM_F);
		INPUT_SPACE = SENSOR_F.size();
		CUSTOM_SPACE = CUSTOM_F.size();
	}

	public SniperBrain(Sniper agent) {
		super(agent);
		this.sniperAgent = agent;
		// from constant recompute?

		sensorInit();
		behaviorInit();
		thresholdInit();
	}
	public void evalSensorF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = SENSOR_F.get(i).eval(sniperAgent, this);
			;
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	}
	;
	public void evalCustF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = CUSTOM_F.get(i).eval(sniperAgent, this);
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	}
	protected double getShortRangeScan() {return 10;}
	protected double getLongRangeScan() {return 48;}
	protected double getShootingRange() {return 40;}

	public void activateGoalWrapper() {
		super.activateGoalWrapper();
		sniperAgent.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(precisionSnipeB), false));
	}
	protected void packInput(float[] modArr) {
		for (int i = 0; i < SENSOR_F.size(); i++) {
			modArr[i] = SENSOR_F.get(i).eval(this.sniperAgent, this);
		}
	}  // update payload
}