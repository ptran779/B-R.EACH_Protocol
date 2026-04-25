package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.special_behavior.ShieldChargeBehavior;
import com.github.ptran779.breach_ptc.entity.agent.Heavy;

import java.util.ArrayList;

public class HeavyBrain extends AbsAgentBrain {
	protected Heavy heavy;
	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 16;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	int shieldChargeB;
	protected void behaviorInit(){
		super.behaviorInit();
		shieldChargeB = addBehavior(new ShieldChargeBehavior(agent, 60, 20, 400, 32, hostileLongRS, friendlyLosFastS));
	}

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Heavy, HeavyBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Heavy, HeavyBrain>> CUSTOM_F = new ArrayList<>();
	public void evalSensorF(float[] state, long mask){
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = SENSOR_F.get(i).eval(heavy, this);;
			// 3. Clear that '1' bit from the mask so we can jump to the next one
			mask &= (mask - 1L);
		}
	};
	public void evalCustF(float[] state, long mask) {
		while (mask != 0L) {
			// 1. Get the exact index of the lowest '1' bit
			int i = Long.numberOfTrailingZeros(mask);
			// 2. Compute ONLY that specific sensor and slot it in
			state[i] = CUSTOM_F.get(i).eval(heavy, this);
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

	public HeavyBrain(Heavy agent) {
		super(agent);
		this.heavy = agent;
		// from constant recompute?

		sensorInit();
		behaviorInit();
		thresholdInit();
	}

	public void activateGoalWrapper() {
		heavy.goalSelector.addGoal(1, new GoalWrapper(behaviors.get(retreatB), false));

		heavy.goalSelector.addGoal(2, new GoalWrapper(behaviors.get(shieldChargeB), true));

		heavy.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(gunB), true));
		heavy.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(meleeB), true));

		heavy.goalSelector.addGoal(4, new GoalWrapper(behaviors.get(followB), true));

		heavy.goalSelector.addGoal(5, new GoalWrapper(behaviors.get(retarAcqB), false));
		heavy.goalSelector.addGoal(6, new GoalWrapper(behaviors.get(nearTarAcqB), false));

		heavy.goalSelector.addGoal(10, new GoalWrapper(behaviors.get(chargeVirtAmmoB), false));
		heavy.goalSelector.addGoal(11, new GoalWrapper(behaviors.get(eatB), false));
		heavy.goalSelector.addGoal(12, new GoalWrapper(behaviors.get(saluteB), true));
		heavy.goalSelector.addGoal(13, new GoalWrapper(behaviors.get(patrolB), false));

		heavy.goalSelector.addGoal(14, new GoalWrapper(behaviors.get(wanderB), true));
	}
	protected double getShortRangeScan() {return 12;}
	protected double getLongRangeScan() {return 28;}
	protected double getShootingRange() {return 24;}

	protected void packInput(float[] modArr) {
		for (int i = 0; i < SENSOR_F.size(); i++) {
			modArr[i] = SENSOR_F.get(i).eval(this.heavy, this);
		}
	}
}