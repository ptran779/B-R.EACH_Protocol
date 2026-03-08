package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.ai.behavior.*;
import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.api.MLServer;
import com.github.ptran779.breach_ptc.ai.api.ScoreCompiler;
import com.github.ptran779.email.DataManager;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.github.ptran779.breach_ptc.server.ForgeServerEvent.BRAIN_SERVER;

public class SwordBrain extends AbsAgentBrain {
	public static final int COLD_BRAIN_TIME = 300;
	protected boolean computing = false;  // is server figuring out what to do
	protected float[] payload;  // main
	protected float[] payloadLastRun; // copy that used to scale
	protected float[] statePre, statePost, stateCust;   // pre and post and custom sensor data -- all raw
	protected Swordman swordAgent;

	protected List<DataManager.itemUnit> currentChain = new ArrayList<>();

	// critical update me with each new code PLS
	public static int OUTPUT_SPACE = 14;
	public static int INPUT_SPACE;
	public static int CUSTOM_SPACE;

	private int lastActiveTick = 0;
	private int nextScanTick = 0;
	private int currentThrottleRate = 20;

	int eatB, followB, saluteB, wanderB, meleeB, gunB, nearTarAcqB, highTarAcqB, retarAcqB, takeCoverB, retreatB,
		chargeVirtAmmo, forceAmmoReload, selfStudy;

	/// bunch of config stuff need to go to DiskIO
//	@Nullable
	public ScoreCompiler scoreFunc;
	@Nonnull
	// how much does input deviate from this value that force brain computation aka corrosion? just default all to 20%
	// for now, will config later
	public float[] inputDeviation;

	//config stuff
	public boolean autotrain = false;
	private boolean collectExp = false;
	public int impTime = 1;
	public int failTime = 0;
	public float exploreRate = 0;

	/// static func array -- share across for masking usage
	public static final ArrayList<Feature<Swordman, SwordBrain>> SENSOR_F = new ArrayList<>();
	public static final ArrayList<Feature<Swordman, SwordBrain>> CUSTOM_F = new ArrayList<>();

	/// custom eval stuff
	protected static void registerCustomEval(List<Feature<Swordman, SwordBrain>> list) {
		///  a bunch of 1 hot encoder
		list.add((agent, brain) -> {
			int b = brain.activeBehaviours;  // critical: special case since these acquisition are single tick
			return (b == brain.nearTarAcqB || b == brain.highTarAcqB || b == brain.retarAcqB) ? 1f : 0f;
		});  // acquiring target
		list.add((agent, brain) -> {
			int b = brain.lastRunBehaviours;
			return (b == brain.meleeB || b == brain.gunB) ? 1f : 0f;
		});  // combat
		list.add((agent, brain) -> {
			int b = brain.lastRunBehaviours;
			return (b == brain.takeCoverB || b == brain.retreatB) ? 1f : 0f;
		});  // survival
		list.add((agent, brain) -> {
			int b = brain.lastRunBehaviours;
			return (b == brain.chargeVirtAmmo || b == brain.eatB || b == brain.forceAmmoReload || b == brain.selfStudy) ? 1f : 0f;
		});  // preparation
	}

	static {
		registerCommonSensor(SENSOR_F);
		registerCustomEval(CUSTOM_F);
		INPUT_SPACE = SENSOR_F.size();
		CUSTOM_SPACE = CUSTOM_F.size();
	}

	public SwordBrain(Swordman agent) {
		super(agent);
		this.swordAgent = agent;
		payload = new float[INPUT_SPACE];
		payloadLastRun = new float[INPUT_SPACE];
		inputDeviation = new float[INPUT_SPACE];
		Arrays.fill(inputDeviation,
			0.25f);  // critical should be small enough to detect constant general change? only true static might prevent it
		// from constant recompute?
		statePre = new float[SENSOR_F.size()];
		statePost = new float[SENSOR_F.size()];
		stateCust = new float[CUSTOM_F.size()];

		sensorInit();
		behaviorInit();
		thresholdInit();
		//fixme change this?
		scoreFunc = new ScoreCompiler(
			"tanh(" +
				"(($a1 - $b1) * 3.0" + // heath
				" + ($a4 - $b4) * 1.0" + // food
				" + min(0, $a7 - $b7) * 1.5" + // ammo usage  -- ramp up sensor speed or change strat
				" + $c1 * (max(0, ($b19 - $a19) * 20) + max(0, ($b20 - $a20) * 2) - step(6 - $b1) * 4)" +  // kill and dmg
				// reward less being on low health
				" - $c0 * $b19 * 10" + // punish for target flickering unless necessary
				" + step(6 - $b1) * $c2 * 10)" +  // try to hide if you low
				" / 1000)");  // global scale modifier for tanh resolution
		try {
			scoreFunc.validate(INPUT_SPACE, CUSTOM_SPACE);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	protected void behaviorInit() {
		eatB = addBehavior(new EatBehavior(swordAgent, bestFoodIS));
		followB = addBehavior(new FollowBehavior(swordAgent, 32, 6, 4));
		saluteB = addBehavior(new SaluteBehavior(swordAgent, 200, 100, bossS));
		wanderB = addBehavior(new WanderBehavior(swordAgent, 200, 100));
		gunB = addBehavior(new GunBehavior(swordAgent, 2, 24, 64, gunIS, clearLosS, targetSqS));
		meleeB = addBehavior(new MeleeBehavior(swordAgent, 1, 3, 32, meleeDmgS, targetSqS));
		retarAcqB = addBehavior(new AcquireRetaliationTargetBehavior(swordAgent, 20, 10, 32, retarHostileS));
		nearTarAcqB = addBehavior(new AcquireNearestTargetBehavior(swordAgent, 20, 10, 64, nearestHostileS));
		highTarAcqB = addBehavior(new AcquireHighestHealthTargetBehavior(swordAgent, 20, 10, 64, highestHealthHostileS));
		takeCoverB = addBehavior(new TakeCoverBehavior(swordAgent, 200, 0, retarHostileS, 8, 12));
		retreatB = addBehavior(new RetreatBehavior(20, 0, swordAgent, hostileShortRS, 12, 4));
		chargeVirtAmmo = addBehavior(new RechargeVirtualAmmoBehavior(swordAgent, 80, 20, friendlyShortRS));
		forceAmmoReload = addBehavior(new ForceReloadBehavior(swordAgent, ammoInChamberS, maxAmmoInChamberS));
		selfStudy = addBehavior(new AutoLearnBehavior(swordAgent, 200, 0));  //critical fixme for fast test only
	}
	protected void thresholdInit(){
		int c = 0;
		/// last compute time
		inputDeviation[c++] = 1000;  // time is not important
		/// self
		inputDeviation[c++] = 0.2f;  // health
		inputDeviation[c++] = 0.1f;  // armor
		inputDeviation[c++] = 0.1f;  // armor toughness
		inputDeviation[c++] = 0.2f;  // food
		inputDeviation[c++] = 0.1f;  // melee dmg
		inputDeviation[c++] = 0.1f;  // gun dmg
		inputDeviation[c++] = 0.6f;  // ammoInChamber -- heavily flux,
		inputDeviation[c++] = 0.5f;  // total ammo -- more smooth, but still a bit flux heavy
		/// entity group
		// enemy
		inputDeviation[c++] = 0.4f;  // total number
		inputDeviation[c++] = 0.3f;  // total health
		inputDeviation[c++] = 0.2f;  // avg health
		inputDeviation[c++] = 0.7f;  // centroid dist
		inputDeviation[c++] = 0.7f;  // std centroid
		// friend
		inputDeviation[c++] = 0.4f;  // total number
		inputDeviation[c++] = 0.3f;  // total health
		inputDeviation[c++] = 0.2f;  // avg health
		inputDeviation[c++] = 0.7f;  // centroid dist
		inputDeviation[c++] = 0.7f;  // std centroid
		/// entity dat
		// current target
		inputDeviation[c++] = 1f;  // boolean exist 1 or 0
		inputDeviation[c++] = 0.2f;  // health
		inputDeviation[c++] = 0.7f;  // dist
		// retar hostile
		inputDeviation[c++] = 1f;  // boolean exist 1 or 0
		inputDeviation[c++] = 0.2f;  // health
		inputDeviation[c++] = 0.7f;  // dist
		// nearest hostile
		inputDeviation[c++] = 1f;  // boolean exist 1 or 0
		inputDeviation[c++] = 0.2f;  // health
		inputDeviation[c++] = 0.7f;  // dist
		// highest health hostile
		inputDeviation[c++] = 1f;  // boolean exist 1 or 0
		inputDeviation[c++] = 0.2f;  // health
		inputDeviation[c++] = 0.7f;  // dist
		// space
		inputDeviation[c++] = 0.7f;  // centroid dist
		inputDeviation[c++] = 0.7f;  // std centroid
		inputDeviation[c++] = 0.7f;  // centroid dist
		inputDeviation[c++] = 0.7f;  // std centroid
	}

	/// for entity disk IO
	public void diskWrite(CompoundTag nbt) {
		if (this.scoreFunc != null) {
			nbt.putByteArray("alu_inst", this.scoreFunc.getInstructions());

			ListTag constList = new ListTag();
			for (float f : this.scoreFunc.getConstants()) {
				constList.add(FloatTag.valueOf(f));
			}
			nbt.put("alu_const", constList);
		}

		ListTag devList = new ListTag();
		for (float f : this.inputDeviation) {
			devList.add(FloatTag.valueOf(f));
		}
		nbt.put("b_input_dev", devList);
		nbt.putBoolean("b_a_train", autotrain);
		nbt.putBoolean("b_a_col_exp", collectExp);
		nbt.putInt("b_imp_t", impTime);
		nbt.putFloat("b_exp_r", exploreRate);

		pushDeepChain(); // flush what left of the memory out
	}
	public void diskRead(CompoundTag nbt) {
		if (nbt.contains("alu_inst")) {
			byte[] inst = nbt.getByteArray("alu_inst");
			ListTag constList = nbt.getList("alu_const", 5); // 5 = FloatTag
			float[] constants = new float[constList.size()];
			for (int i = 0; i < constList.size(); i++) {
				constants[i] = constList.getFloat(i);
			}
			this.scoreFunc = new ScoreCompiler(inst, constants);
		}

		ListTag devList = nbt.getList("b_input_dev", 5);
		// Ensure array exists and matches current INPUT_SPACE constant
		int limit = Math.min(devList.size(), INPUT_SPACE);
		for (int i = 0; i < limit; i++) {
			this.inputDeviation[i] = devList.getFloat(i);
		}

		autotrain = nbt.getBoolean("b_a_train");
		collectExp = nbt.getBoolean("b_a_col_exp");
		impTime = nbt.getInt("b_imp_t");
		exploreRate = nbt.getFloat("b_exp_r");
	}

	public boolean getCollectExp() {return collectExp;}
	public void trySetCollectExp(boolean mode) {
		if (mode) {
			ItemStack item = swordAgent.getChipBrainStack();
			if (item.getItem() instanceof BrainChipItem brainChipItem) {
				MlModelManager.MLUnit unit =
					MlModelManager.getMUnit(brainChipItem.getOrCreateUUID(item), swordAgent.level().getGameTime());
				if (unit.dataManager == null) {
					unit.dataManager = new DataManager();
				}
				;
				collectExp = true;
			}
		} else {
			collectExp = false;
		}
	}

	public void lockComputing() {computing = true;}
	public void doneComputing() {computing = false;}
	public void activateGoalWrapper() {
		swordAgent.goalSelector.addGoal(1, new GoalWrapper(behaviors.get(retreatB), false));
		swordAgent.goalSelector.addGoal(1, new GoalWrapper(behaviors.get(takeCoverB), false));

		swordAgent.goalSelector.addGoal(2, new GoalWrapper(behaviors.get(gunB), true));
		swordAgent.goalSelector.addGoal(2, new GoalWrapper(behaviors.get(meleeB), true));

		swordAgent.goalSelector.addGoal(3, new GoalWrapper(behaviors.get(followB), true));

		swordAgent.goalSelector.addGoal(4, new GoalWrapper(behaviors.get(retarAcqB), false));
		swordAgent.goalSelector.addGoal(5, new GoalWrapper(behaviors.get(nearTarAcqB), false));

		swordAgent.goalSelector.addGoal(6, new GoalWrapper(behaviors.get(chargeVirtAmmo), false));

		swordAgent.goalSelector.addGoal(8, new GoalWrapper(behaviors.get(eatB), false));
		swordAgent.goalSelector.addGoal(9, new GoalWrapper(behaviors.get(saluteB), true));
		swordAgent.goalSelector.addGoal(10, new GoalWrapper(behaviors.get(wanderB), true));
	}

	protected boolean deviationDetected(float oldVal, float newVal, float thresRatio) {
		float diff = Math.abs(newVal - oldVal);
		return diff > 0.01f && diff > (Math.abs(oldVal) * thresRatio);
	}
	protected void resetHiddenStateToZeros() {
		ItemStack item = swordAgent.getChipBrainStack();
		if (item.getItem() instanceof BrainChipItem brainChipItem) {  // fixme critical see if this can be cache and only scan when inventory touch
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, swordAgent.level().getGameTime());
			if (mUnit.model != null) mUnit.model.clearHidden();
		}
	}
	protected void packInput() {
		// SENSOR_F size is roughly 34 based on your registry
		for (int i = 0; i < SENSOR_F.size(); i++) {
			payload[i] = SENSOR_F.get(i).eval(this.swordAgent, this);
		}
	}  // update payload
	public void forceWakeUp() {
		this.currentThrottleRate = 20;
	}
	@Override public void preTick() {
		int currentTick = swordAgent.tickCount;

		// Ensure brain in Hot State if active
		if (this.activeBehaviours != -1) {
			lastActiveTick = currentTick;
			currentThrottleRate = 20;
		}

		// Put brain to cold state if has not been running for a while
		if (currentTick - lastActiveTick >= COLD_BRAIN_TIME && currentThrottleRate != COLD_BRAIN_TIME) {
			currentThrottleRate = COLD_BRAIN_TIME;
			pushDeepChain();
			resetHiddenStateToZeros();
		}

		// is it time to run
		if (computing || currentTick < nextScanTick) return;
		nextScanTick = currentTick + currentThrottleRate;
		packInput(); // raw, not scale

		// is there a reason to run
		if (!forceTrigger && !detectDeviation())  return;
		forceTrigger = false;

		System.arraycopy(payload, 0, payloadLastRun, 0, INPUT_SPACE);  // copy the arr
		scaleCommonInput(payloadLastRun);  // scale for ML
		if (exploreRate > 0f && swordAgent.getRandom().nextFloat() < exploreRate) runRandomBehavior();
		else runBrainInf();
	}

	private boolean detectDeviation(){
		for (int i = 0; i < INPUT_SPACE; i++) { // will trigger cache hot wire
			// compare the send data aka what trigger compute vs the current reading
			if (deviationDetected(statePre[i], payload[i], inputDeviation[i])) return true;
		}
		return false;
	};
	private void runRandomBehavior(){
		// random exploration -- for encouraged new behavior testing
		int[] randomIdx = new int[OUTPUT_SPACE];
		for (int b = 0; b < OUTPUT_SPACE; b++) randomIdx[b] = b;

		// Fast Fisher-Yates shuffle for the behavior indices
		for (int b = OUTPUT_SPACE - 1; b > 0; b--) {
			int swap = swordAgent.getRandom().nextInt(b + 1);
			int temp = randomIdx[b];
			randomIdx[b] = randomIdx[swap];
			randomIdx[swap] = temp;
		}
		tryBehaviorChain(randomIdx);
	}
	private void runBrainInf(){
		ItemStack item = swordAgent.getChipBrainStack();
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, swordAgent.level().getGameTime());
			if (mUnit.model == null || mUnit.model.getInsize() != INPUT_SPACE || mUnit.model.getOutsize() != OUTPUT_SPACE) {
				swordAgent.brainOFF();
			} else {
				if (BRAIN_SERVER.TASK_QUEUE_INF.offer(
					new MLServer.InfDatIn(swordAgent.getUUID(), payloadLastRun, mUnit.model))) {
					//critical fixme later handle case where thread full -- better
					lockComputing();  // mark in computational mode
					lastActiveTick = swordAgent.tickCount;
				}
			}
		}
	}

//	@Override
//	public void preTick() {
//		int currentTick = swordAgent.tickCount;
//
//		// 1. Throttle check
//		updateThrottleState(currentTick);
//
//		// 2. FIRST EARLY EXIT: Is it too early, or are we busy?
//		if (computing || currentTick < nextScanTick) return;
//
//		// 3. Prepare for evaluation
//		nextScanTick = currentTick + currentThrottleRate;
//		packInput();
//
//		// 4. Evaluate triggers (Flattened logic)
//		boolean needsCompute = forceTrigger || checkDeviation();
//		forceTrigger = false; // Always consume the trigger if we checked it
//
//		// 5. SECOND EARLY EXIT: Nothing changed, go back to sleep.
//		if (!needsCompute) return;
//
//		// ==========================================
//		// 6. IF WE REACH HERE, WE ARE COMPUTING
//		// No nested if-blocks required.
//		// ==========================================
//
//		System.arraycopy(payload, 0, statePre, 0, INPUT_SPACE);
//		System.arraycopy(payload, 0, payloadLastRun, 0, INPUT_SPACE);
//		scaleCommonInput(payloadLastRun);
//
//		if (exploreRate > 0f && swordAgent.getRandom().nextFloat() < exploreRate) {
//			executeExploration();
//		} else {
//			executeInference(currentTick);
//		}
//	}
//	private boolean checkDeviation() {
//		for (int i = 0; i < INPUT_SPACE; i++) {
//			if (deviationDetected(statePre[i], payload[i], inputDeviation[i])) {
//				return true; // Fast exit on first deviation
//			}
//		}
//		return false;
//	}
//	private void updateThrottleState(int currentTick) {
//		if (this.activeBehaviours != -1) {
//			lastActiveTick = currentTick;
//			currentThrottleRate = 20; // Hot State
//		}
//
//		if (currentTick - lastActiveTick >= COLD_BRAIN_TIME && currentThrottleRate != COLD_BRAIN_TIME) {
//			currentThrottleRate = COLD_BRAIN_TIME; // Switch to 30s slow scan
//			pushDeepChain();
//			resetHiddenStateToZeros();
//		}
//	}
//	private void executeExploration() {
//		int[] randomIdx = new int[OUTPUT_SPACE];
//		for (int b = 0; b < OUTPUT_SPACE; b++) randomIdx[b] = b;
//
//		// Fast Fisher-Yates shuffle
//		for (int b = OUTPUT_SPACE - 1; b > 0; b--) {
//			int swap = swordAgent.getRandom().nextInt(b + 1);
//			int temp = randomIdx[b];
//			randomIdx[b] = randomIdx[swap];
//			randomIdx[swap] = temp;
//		}
//		tryBehaviorChain(randomIdx);
//	}
//	private void executeInference(int currentTick) {
//		ItemStack item = swordAgent.getChipBrainStack();
//
//		// Early exit if it's not a brain chip
//		if (!(item.getItem() instanceof BrainChipItem brainChipItem)) {
//			return;
//		}
//
//		UUID chipTag = brainChipItem.getOrCreateUUID(item);
//		MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, swordAgent.level().getGameTime());
//
//		// Failsafe: Turn brain off if model is invalid
//		if (mUnit.model == null || mUnit.model.getInsize() != INPUT_SPACE || mUnit.model.getOutsize() != OUTPUT_SPACE) {
//			swordAgent.brainOFF();
//			return;
//		}
//
//		// Send to Server Thread
//		if (BRAIN_SERVER.TASK_QUEUE_INF.offer(new MLServer.InfDatIn(swordAgent.getUUID(), payloadLastRun, mUnit.model))) {
//			lockComputing();
//			lastActiveTick = currentTick;
//		} else {
//			// FIXME: Task Queue is full! Handle thread overflow later.
//		}
//	}

	// in pos tick series
	public void onBehaviorStart(int startB) {
		super.onBehaviorStart(startB);
		System.arraycopy(payload, 0, statePre, 0, INPUT_SPACE);  // copy the arr
//		System.out.println(Arrays.toString(statePre));
		agent.printObservation(agent.getDisplayName().getString() + " starting " + behaviors.get(startB), agent.level().getServer());
//		System.out.println("Starting " + behaviors.get(startB));
	}

	// add to tmp chain
	public void onBehaviorStop(int stopB) {
		agent.printObservation(agent.getDisplayName().getString() + " stopping " + behaviors.get(stopB), agent.level().getServer());
		if (!collectExp) return;  // only run with collector on
//		System.out.println("Stopping " + behaviors.get(stopB));
		ItemStack item = swordAgent.getChipBrainStack();  // this check can be remove once func state map correctly
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, swordAgent.level().getGameTime());
			if (mUnit.dataManager == null) {
				collectExp = false;  // turn this off
			} else {
				// state post
				long mask = scoreFunc.getMaskB();
				while (mask != 0L) {
					// 1. Get the exact index of the lowest '1' bit
					int i = Long.numberOfTrailingZeros(mask);
					// 2. Compute ONLY that specific sensor and slot it in
					statePost[i] = SENSOR_F.get(i).eval(swordAgent, this);
					// 3. Clear that '1' bit from the mask so we can jump to the next one
					mask &= (mask - 1L);
				}
				// custom func
				mask = scoreFunc.getMaskC();
				while (mask != 0L) {
					// 1. Get the exact index of the lowest '1' bit
					int i = Long.numberOfTrailingZeros(mask);
					// 2. Compute ONLY that specific sensor and slot it in
					stateCust[i] = CUSTOM_F.get(i).eval(swordAgent, this);
					// 3. Clear that '1' bit from the mask so we can jump to the next one
					mask &= (mask - 1L);
				}
				currentChain.add(new DataManager.itemUnit(payloadLastRun.clone(), stopB, scoreFunc.evaluate(statePre,
					statePost, stateCust)));
			}
		}
	}

	// for commiting cold chain storage
	public void pushDeepChain() {
		agent.printObservation(agent.getDisplayName().getString() + " going cold ", agent.level().getServer());
//		System.out.println("Cold Brain");
		if (currentChain.isEmpty()) return;
		ItemStack item = swordAgent.getChipBrainStack();  // this check can be remove once func state map correctly
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, swordAgent.level().getGameTime());
			if (mUnit.dataManager == null) {
				collectExp = false;  // turn this off
			} else {
//				System.out.println("\n\n Cold Storage of the chain");
				mUnit.dataManager.add(currentChain);  // fixme critical swap the memory manager to revolving memory slider
				currentChain = new ArrayList<>();
			}
		}
	}
}