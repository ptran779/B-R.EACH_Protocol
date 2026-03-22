package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;

import java.util.ArrayList;

public class SwordBrain extends AbsAgentBrain {
	protected Swordman swordAgent;
	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 15;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Swordman, SwordBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Swordman, SwordBrain>> CUSTOM_F = new ArrayList<>();
	public void evalSensorF(float[] state, long mask){
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = SENSOR_F.get(i).eval(swordAgent, this);;
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	};
	public void evalCustF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = CUSTOM_F.get(i).eval(swordAgent, this);
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

	public SwordBrain(Swordman agent) {
		super(agent);
		this.swordAgent = agent;
		// from constant recompute?

		sensorInit();
		behaviorInit();
		thresholdInit();
	}

	public void activateGoalWrapper() {
		swordAgent.goalSelector.addGoal(1, new GoalWrapper(behaviors.get(retreatB), false));

		swordAgent.goalSelector.addGoal(2, new GoalWrapper(behaviors.get(gunB), true));
		swordAgent.goalSelector.addGoal(2, new GoalWrapper(behaviors.get(meleeB), true));

		swordAgent.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(followB), true));

		swordAgent.goalSelector.addGoal(4, new GoalWrapper(behaviors.get(retarAcqB), false));
		swordAgent.goalSelector.addGoal(5, new GoalWrapper(behaviors.get(nearTarAcqB), false));

		swordAgent.goalSelector.addGoal(6, new GoalWrapper(behaviors.get(chargeVirtAmmoB), false));
		swordAgent.goalSelector.addGoal(7, new GoalWrapper(behaviors.get(eatB), false));
		swordAgent.goalSelector.addGoal(8, new GoalWrapper(behaviors.get(saluteB), true));
		swordAgent.goalSelector.addGoal(9, new GoalWrapper(behaviors.get(patrolB), false));

		swordAgent.goalSelector.addGoal(10, new GoalWrapper(behaviors.get(wanderB), true));
	}
	protected double getShortRangeScan() {return 16;}
	protected double getLongRangeScan() {return 24;}
	protected double getShootingRange() {return 20;}

	protected void packInput(float[] modArr) {
		for (int i = 0; i < SENSOR_F.size(); i++) {
			modArr[i] = SENSOR_F.get(i).eval(this.swordAgent, this);
		}
	}  // update payload
}