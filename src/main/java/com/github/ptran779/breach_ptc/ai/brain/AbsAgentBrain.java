package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.*;
import com.github.ptran779.breach_ptc.ai.behavior.*;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.github.ptran779.breach_ptc.server.ForgeServerEvent.BRAIN_SERVER;

/// this it not the ML brain, this is support for enhancing brain and sensor info.
/// ML weight model is stored in centralized storage else where
public abstract class AbsAgentBrain extends Brain {
	@NotNull AbsAgentEntity agent;
	public static final int COLD_BRAIN_TIME = 300;
	protected boolean computing = false;  // is server figuring out what to do
	protected float[] payload;  // main
	protected float[] payloadLastRun; // copy that used to scale
	protected float[] statePre, statePost, stateCust;   // pre and post and custom sensor data -- all raw

	/// Sensor array
	protected Sensor<Player> bossS;
	protected Sensor<Boolean> friendlyLosFastS, friendlyLosFullS, targetLosS;
	protected Sensor<Double> targetDistSqS;
	protected Sensor<List<LivingEntity>> liveEntShortRS, hostileShortRS, friendlyShortRS, liveEntLongRS, hostileLongRS;
	protected Sensor<LivingEntity> retarHostileS, nearestHostileS, highestHealthHostileS;
	protected Sensor<Float> hostileShortRTotalHS, friendlyShortRTotalHS;
	protected Sensor<ItemStack> bestFoodIS;

	protected Sensor<Float> meleeDmgS, gunDmgS;  // a bit overkill?
	protected Sensor<Integer> totalAmmoCountS, ammoInChamberS, maxAmmoInChamberS;
	protected Sensor<List<Vec3>> lidar12RS;

	/// behavior 15 bases
	protected int eatB, followB, saluteB, wanderB, meleeB, gunB, nearTarAcqB, highTarAcqB, retarAcqB, takeCoverB, patrolB,
		retreatB, chargeVirtAmmoB, forceAmmoReload, selfStudy;

	protected void behaviorInit() {
		eatB = addBehavior(new EatBehavior(agent, bestFoodIS));
		followB = addBehavior(new FollowBehavior(agent, 32, 6, 4));
		saluteB = addBehavior(new SaluteBehavior(agent, 100, 100, bossS));
		wanderB = addBehavior(new WanderBehavior(agent, 200, 100));
		patrolB = addBehavior(new PatrolBehavior(agent, 20, 0));
		gunB = addBehavior(new GunBehavior(agent, 2, getShootingRange(), 64, ammoInChamberS, totalAmmoCountS,
			friendlyLosFastS,
			targetDistSqS,targetLosS));
		meleeB = addBehavior(new MeleeBehavior(agent, 1, 3, 32, meleeDmgS, targetDistSqS, targetLosS));
		retarAcqB = addBehavior(new AcquireRetaliationTargetBehavior(agent, 20, 10, 32, retarHostileS));
		nearTarAcqB = addBehavior(new AcquireNearestTargetBehavior(agent, 20, 10, 64, nearestHostileS));
		highTarAcqB = addBehavior(new AcquireHighestHealthTargetBehavior(agent, 20, 10, 64, highestHealthHostileS));
		takeCoverB = addBehavior(new TakeCoverBehavior(agent, 200, 0, retarHostileS, 8, 12));
		retreatB = addBehavior(new RetreatBehavior(20, 0, agent, hostileShortRS, 12, 4));
		chargeVirtAmmoB = addBehavior(new RechargeVirtualAmmoBehavior(agent, 80, 20, friendlyShortRS));
		forceAmmoReload = addBehavior(new ForceReloadBehavior(agent, ammoInChamberS, maxAmmoInChamberS));
		selfStudy = addBehavior(new AutoLearnBehavior(agent, 600, 0));  //critical fixme for fast test only
	}

	/// bunch of config stuff need to go to DiskIO
	public ScoreCompiler scoreFunc;
	// how much does input deviate from this value that force brain computation aka corrosion? just default all to 20%
	// for now, will config later
	public float[] inputDeviation;

	/// Agent Brain Config
	public boolean autotrain = false;
	private boolean collectExp = false;
	public int impTime = 1;
	public int failTime = 0;
	public float exploreRate = 0;

	/// Experience Storage
	protected List<DataManager.itemUnit> currentChain = new ArrayList<>();

	public AbsAgentBrain(@NotNull AbsAgentEntity agent) {
		super(agent);
		this.agent = agent;
		payload = new float[agent.getInputSpace()];
		payloadLastRun = new float[agent.getInputSpace()];
		inputDeviation = new float[agent.getInputSpace()];

		statePre = new float[agent.getInputSpace()];
		statePost = new float[agent.getInputSpace()];
		stateCust = new float[agent.getCustSpace()];

		scoreFunc = new ScoreCompiler("tanh(" + "(($a1 - $b1) * 3.0" + // heath
			" + ($a4 - $b4) * 1.0" + // food
			" + $c4 * 1.5" + // ammo usage  -- ramp up sensor speed or change strat
			" + $c1 * (max(0, ($b19 - $a19) * 20) + max(0, ($b20 - $a20) * 2) - step(6 - $b1) * 4)" +  // kill and dmg
			// reward less being on low health
			" - $c0 * $b19 * 10" + // punish for target flickering unless necessary
			" + step(6 - $b1) * $c2 * 10)" +  // try to hide if you low
			" / 1000)");  // global scale modifier for tanh resolution
		try {
			scoreFunc.validate(agent.getInputSpace(), agent.getCustSpace());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void sensorInit() {
		bossS = new Sensor<>(
			() -> Utils.findNearestPlayer(agent, getShortRangeScan(), player -> agent.getBossUUID().equals(player.getUUID())),
			40);
		;
		bestFoodIS = new Sensor<>(agent.inventory1::getBestFood, 20);
		meleeDmgS = new Sensor<>(() -> {
			ItemStack stack = agent.inventory1.getItem(agent.MELEE_SLOT);
			if (stack.isEmpty()) return 1.0f;
			var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
			return (float) (1.0 + modifiers.get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(AttributeModifier::getAmount)
				.sum());
		}, 10);
		gunDmgS = new Sensor<>(() -> agent.getGunDmg(), 10);
		totalAmmoCountS = new Sensor<>(() -> agent.inventory1.totalUsableAmmo(), 80);
		ammoInChamberS = new Sensor<>(() -> agent.inventory1.checkAmmoInChamber(), 20);
		maxAmmoInChamberS = new Sensor<>(() -> agent.inventory1.maxAmmoInChamber(), 20);

		friendlyLosFullS = new Sensor<>(() -> Utils.hasFriendlyInLineOfFire(agent, agent.getTarget(), -1), 40);
		friendlyLosFastS = new Sensor<>(() -> Utils.hasFriendlyInLineOfFire(agent, agent.getTarget(), 5)
			|| friendlyLosFullS.get(agent.tickCount), 10);
		targetDistSqS = new Sensor<>(() -> {
			LivingEntity target = agent.getTarget();
			if (target != null && target.isAlive()) return agent.distanceToSqr(target);
			return -1.0d;  // just super far away AKA non existence
		});
		targetLosS = new Sensor<>(() -> {
			LivingEntity target = agent.getTarget();
			if (target == null || !target.isAlive()) return false;
			return !Utils.rayCastHit(agent.getEyePosition(), target.getEyePosition(), (ServerLevel) agent.level());
		}, 10);
		liveEntLongRS = new Sensor<>(() -> Utils.getAllLivingInRange(agent, getLongRangeScan()), 300);
		hostileLongRS = new Sensor<>(() -> {
			List<LivingEntity> out = new ArrayList<>();
			List<LivingEntity> all = liveEntLongRS.get(agent.tickCount);
			for (LivingEntity entity : all) {
				if (agent.isPotentialHostile(agent, entity)) out.add(entity);
			}
			return out;
		}, 300);
		highestHealthHostileS = new Sensor<>(() -> {
			LivingEntity target = null;
			float highestH = -1;
			List<LivingEntity> entities = hostileLongRS.get(agent.tickCount);
			if (entities == null || entities.isEmpty()) return null;
			for (LivingEntity liveEnt : entities) {
				if (liveEnt == null || !liveEnt.isAlive() || !agent.shouldTargetEntity(agent,
					liveEnt) || liveEnt.getHealth() < highestH || Utils.rayCastHit(agent.getEyePosition(),
					liveEnt.getEyePosition(), (ServerLevel) agent.level())) continue;
				highestH = liveEnt.getHealth();
				target = liveEnt;
			}
			return agent.getTarget() == target ? null : target;
		}, 150);  // match with the other sensor speed
		lidar12RS = new Sensor<>(() -> {
			List<Vec3> out = new ArrayList<>(36);
			performLayerScan(out, -30, 12, 12);
			performLayerScan(out, 0, 12, 12);
			performLayerScan(out, 30, 12, 12);
			return out;
		}, 40);
		retarHostileS = new Sensor<>(() -> {
			LivingEntity target = agent.getLastHurtByMob();
			if (target == null || !target.isAlive() || target == agent.getTarget() || agent.isAlly(
				target) || agent.tickCount - agent.getLastHurtByMobTimestamp() > 600 || Utils.rayCastHit(agent.getEyePosition(),
				target.getEyePosition(), (ServerLevel) agent.level())) return null;  // only bother with 30s enemy retar
			return target;
		}, 20);
		liveEntShortRS = new Sensor<>(() -> Utils.getAllLivingInRange(agent, getShortRangeScan()), 60);
		hostileShortRS = new Sensor<>(() -> {
			List<LivingEntity> out = new ArrayList<>();
			List<LivingEntity> all = liveEntShortRS.get(agent.tickCount);
			for (LivingEntity entity : all) {
				if (agent.isPotentialHostile(agent, entity)) out.add(entity);
			}
			return out;
		}, 60);
		friendlyShortRS = new Sensor<>(() -> {
			List<LivingEntity> out = new ArrayList<>();
			List<LivingEntity> all = liveEntShortRS.get(agent.tickCount);
			for (LivingEntity entity : all) {
				if (agent.isAlly(entity)) out.add(entity);
			}
			return out;
		}, 60);
		nearestHostileS = new Sensor<>(() -> {
			// we had an internal check for the true target, so no worry about miss classification
			LivingEntity target = findNearestHostileInList(hostileShortRS.get(agent.tickCount));
			if (target == null) {
				target = findNearestHostileInList(hostileLongRS.get(agent.tickCount));
			}
			return agent.getTarget() == target ? null : target;
		}, 60);  // match with hostile update speed

		hostileShortRTotalHS = new Sensor<>(() -> {
			float out = 0;
			for (LivingEntity e : hostileShortRS.get(agent.tickCount)) {
				if (e != null && e.isAlive()) out += e.getHealth();
			}
			return out;
		}, 60);
		friendlyShortRTotalHS = new Sensor<>(() -> {
			float out = 0;
			for (LivingEntity e : friendlyShortRS.get(agent.tickCount)) {
				if (e != null && e.isAlive()) out += e.getHealth();
			}
			return out;
		}, 60);
	}
	protected void thresholdInit() {
		Arrays.fill(inputDeviation,
			0.25f); // critical should be small enough to detect constant general change? only true static might prevent it
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

	///  Disk IO
	// For CSV export support
	public static String getCSVHeader() {
		return "GameID," + "TimeSinceUpdate," + "SelfHealth,SelfArmor,SelfToughness,SelfFood,SelfMeleeDmg,SelfGunDmg,SelfAmmoChamber,SelfTotalAmmo," + "HostileGrp_Size,HostileGrp_TotalHP,HostileGrp_AvgHP,HostileGrp_CentroidDist,HostileGrp_StdDev," + "FriendlyGrp_Size,FriendlyGrp_TotalHP,FriendlyGrp_AvgHP,FriendlyGrp_CentroidDist,FriendlyGrp_StdDev," + "Target_Exists,Target_HP,Target_Dist," + "Retal_Exists,Retal_HP,Retal_Dist," + "Nearest_Exists,Nearest_HP,Nearest_Dist," + "HighHP_Exists,HighHP_HP,HighHP_Dist," + "TerrainRadialCentroid,TerrainRadialStd,TerrainHeightCentroid,TerrainHeightStd," + "ActionTaken,Score";
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
		int limit = Math.min(devList.size(), agent.getInputSpace());
		for (int i = 0; i < limit; i++) {
			this.inputDeviation[i] = devList.getFloat(i);
		}

		autotrain = nbt.getBoolean("b_a_train");
		collectExp = nbt.getBoolean("b_a_col_exp");
		impTime = nbt.getInt("b_imp_t");
		exploreRate = nbt.getFloat("b_exp_r");
	}
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

	// should be decent spacing
	public void activateGoalWrapper(){
		// run away
		agent.goalSelector.addGoal(1, new GoalWrapper(behaviors.get(retreatB), false));

		// fighting
		agent.goalSelector.addGoal(5, new GoalWrapper(behaviors.get(gunB), true));
		agent.goalSelector.addGoal(6, new GoalWrapper(behaviors.get(meleeB), true));

		// follow
		agent.goalSelector.addGoal(7, new GoalWrapper(behaviors.get(followB), true));

		// acquiring target
		agent.goalSelector.addGoal(10, new GoalWrapper(behaviors.get(retarAcqB), false));
		agent.goalSelector.addGoal(11, new GoalWrapper(behaviors.get(nearTarAcqB), false));

		agent.goalSelector.addGoal(20, new GoalWrapper(behaviors.get(chargeVirtAmmoB), false));
		agent.goalSelector.addGoal(21, new GoalWrapper(behaviors.get(eatB), false));
		agent.goalSelector.addGoal(22, new GoalWrapper(behaviors.get(saluteB), true));
		agent.goalSelector.addGoal(23, new GoalWrapper(behaviors.get(patrolB), true));
		agent.goalSelector.addGoal(24, new GoalWrapper(behaviors.get(wanderB), true));
	};

	/// Utils fixme adjust based on agent config range?
	protected abstract double getShortRangeScan();
	protected abstract double getLongRangeScan();
	protected abstract double getShootingRange();

	protected void performLayerScan(List<Vec3> points, float pitchOffset, double range, int rayCount) {
		Vec3 origin = agent.getEyePosition();
		float step = 360.0f / rayCount;

		for (int i = 0; i < rayCount; i++) {
			// 1. Calculate relative Yaw/Pitch
			float absYaw = agent.yBodyRot + (i * step);
			float absPitch = agent.getXRot() + pitchOffset;

			// 2. Convert to radians for Math
			float rYaw = absYaw * ((float) Math.PI / 180F);
			float rPitch = absPitch * ((float) Math.PI / 180F);

			// 3. Minecraft Cartesian Conversion (The Standard Way)
			// MC uses: X = -sin(yaw) * cos(pitch), Z = cos(yaw) * cos(pitch), Y = -sin(pitch)
			double dx = -Math.sin(rYaw) * Math.cos(rPitch);
			double dz = Math.cos(rYaw) * Math.cos(rPitch);
			double dy = -Math.sin(rPitch);

			Vec3 end = origin.add(dx * range, dy * range, dz * range);

			// 4. Raycast
			BlockHitResult result =
				agent.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, agent));

			points.add(result.getType() != HitResult.Type.MISS ? result.getLocation() : end);
		}
	}
	protected LivingEntity findNearestHostileInList(List<LivingEntity> entities) {
		if (entities == null || entities.isEmpty()) return null;

		LivingEntity bestFound = null;
		double minDistanceSqr = Double.MAX_VALUE;

		for (LivingEntity target : entities) {
			// Skip nulls, dead entities, and the agent itself
			if (target == null || !target.isAlive() || !agent.shouldTargetEntity(agent, target) || Utils.rayCastHit(
				agent.getEyePosition(), target.getEyePosition(), (ServerLevel) agent.level())) continue;

			double distSqr = agent.distanceToSqr(target);

			// Only run the heavier targeting check if this entity is actually closer
			if (distSqr < minDistanceSqr) {
				if (agent.shouldTargetEntity(agent, target)) {
					minDistanceSqr = distSqr;
					bestFound = target;
				}
			}
		}

		return bestFound;
	}

	/// Feature Engineering & data packing -- universal usage with static -- used on static class for mass template design
	@FunctionalInterface
	public interface Feature<E extends AbsAgentEntity, B extends AbsAgentBrain> {
		float eval(E agent, B brain);
	}
	private static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerEntityGroup(List<Feature<E, B>> list,
	                                                                                            Function<AbsAgentBrain, List<LivingEntity>> listSupplier,
	                                                                                            Function<AbsAgentBrain, Float> totalHealthSupplier) {
		list.add((agent, brain) -> (float) listSupplier.apply(brain).size()); // Size
		list.add((agent, brain) -> { // Total Health
			return (float) totalHealthSupplier.apply(brain);
		});
		list.add((agent, brain) -> { // Avg Health
			List<LivingEntity> ents = listSupplier.apply(brain);
			if (ents.isEmpty()) return 0f;
			float out = totalHealthSupplier.apply(brain);
			return (out / ents.size());
		});
		list.add((agent, brain) -> Utils.computeCentroid(listSupplier.apply(brain), agent));
		list.add((agent, brain) -> Utils.computeStdOfCentroid(listSupplier.apply(brain)));
	}
	private static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerEntityDat(List<Feature<E, B>> list,
	                                                                                          Function<AbsAgentBrain, LivingEntity> entitySupplier) {
		list.add((agent, brain) -> {
			LivingEntity e = entitySupplier.apply(brain);
			return e != null && e.isAlive() ? 1f : 0f;
		});
		list.add((agent, brain) -> {
			LivingEntity e = entitySupplier.apply(brain);
			return e != null && e.isAlive() ? e.getHealth() : 0f;
		});
		list.add((agent, brain) -> {
			LivingEntity e = entitySupplier.apply(brain);
			return e != null && e.isAlive() ? agent.distanceTo(e) : 0f;
		});
	}
	private static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerTerrainGroup(List<Feature<E, B>> list,
	                                                                                             Function<AbsAgentBrain, List<Vec3>> lidarSupplier) {
		list.add((agent, brain) -> Utils.computeRadialCentroid(lidarSupplier.apply(brain), agent));
		list.add((agent, brain) -> Utils.computeRadialStdCentroid(lidarSupplier.apply(brain)));
		list.add((agent, brain) -> Utils.computeHeightCentroid(lidarSupplier.apply(brain), agent));
		list.add((agent, brain) -> Utils.computeHeightStd(lidarSupplier.apply(brain)));
	}
	/// registration
	protected static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerBasedCommonReceptor(
		List<Feature<E, B>> list) {
		// last compute time 0
		list.add((agent, brain) -> agent.tickCount - brain.lastUpdateTick);
		// self 1-8
		list.add((agent, brain) -> agent.getHealth());
		list.add((agent, brain) -> (float) agent.getAttributeValue(Attributes.ARMOR));
		list.add((agent, brain) -> (float) agent.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		list.add((agent, brain) -> agent.getFood());
		list.add((agent, brain) -> brain.meleeDmgS.get(agent.tickCount));
		list.add((agent, brain) -> brain.gunDmgS.get(agent.tickCount));
		list.add((agent, brain) -> brain.ammoInChamberS.get(agent.tickCount));
		list.add((agent, brain) -> (float) brain.totalAmmoCountS.get(agent.tickCount));
		// entity group 9-18
		registerEntityGroup(list, brain -> brain.hostileShortRS.get(brain.agent.tickCount),
			brain -> brain.hostileShortRTotalHS.get(brain.agent.tickCount));
		registerEntityGroup(list, brain -> brain.friendlyShortRS.get(brain.agent.tickCount),
			brain -> brain.friendlyShortRTotalHS.get(brain.agent.tickCount));
		// interested entity 19-30
		registerEntityDat(list, brain -> brain.agent.getTarget());
		registerEntityDat(list, brain -> brain.retarHostileS.get(brain.agent.tickCount));
		registerEntityDat(list, brain -> brain.nearestHostileS.get(brain.agent.tickCount));
		registerEntityDat(list, brain -> brain.highestHealthHostileS.get(brain.agent.tickCount));
		//terrain 31-34
		registerTerrainGroup(list, brain -> brain.lidar12RS.get(brain.agent.tickCount));
	}
	/// custom eval stuff
	protected static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerBasedCustomEval(
		List<Feature<E, B>> list) {
		// a bunch of 1 hot encoder
		list.add((agent, brain) -> {
			int b = brain.getActiveBehaviours();  // critical: special case since these acquisition are single tick
			return (b == brain.nearTarAcqB || b == brain.highTarAcqB || b == brain.retarAcqB) ? 1f : 0f;
		});  // acquiring target

		list.add((agent, brain) -> {
			int b = brain.getlastRunBehaviours();
			return (b == brain.meleeB || b == brain.gunB) ? 1f : 0f;
		});  // combat

		list.add((agent, brain) -> {
			int b = brain.getlastRunBehaviours();
			return (b == brain.takeCoverB || b == brain.retreatB) ? 1f : 0f;
		});  // survival

		list.add((agent, brain) -> {
			int b = brain.getlastRunBehaviours();
			return (b == brain.chargeVirtAmmoB || b == brain.eatB || b == brain.forceAmmoReload || b == brain.selfStudy) ? 1f : 0f;
		});  // preparation

		list.add((agent, brain) -> agent.postAmmoUsageCount());  // count the bullet usage
	}

	// used to scale the based input. Other position remain unaffected
	public static int scaleEntityGroup(float[] arr, int c) {
		arr[c] = (float) (Math.log(1 + arr[c++]) * 0.4);  // size high bound around 10
		arr[c] = (float) (Math.log(1 + arr[c++]) * 0.2);  // total health is arbitrary tbh...
		arr[c] = arr[c++] * 0.1f;  // avg health can be linear...
		arr[c] = arr[c++] * 0.1f;  // Spatial space
		arr[c] = arr[c++] * 0.1f;  // Spatial space
		return c;
	}
	public static int scaleEntityDat(float[] arr, int c) {
		arr[c + 1] = arr[c + 1] * 0.1f;  // health
		arr[c + 2] = arr[c + 2] * 0.1f;  // dist
		return c + 3;
	}
	public static int scaleTerrainDat(float[] arr, int c) {
		// just lidar rn. ~ 12 -20 block
		arr[c] = arr[c++] * 0.1f;
		arr[c] = arr[c++] * 0.1f;
		arr[c] = arr[c++] * 0.1f;
		arr[c] = arr[c++] * 0.1f;
		return c;
	}
	public static void scaleCommonInput(float[] inArr) {
		int c = 0;
		// time
		inArr[c] = (float) (Math.log(1 + inArr[c++]) * 0.18);  // 15 sec upper bound
		// self
		inArr[c] = inArr[c++] * 0.1f;   // 20 health -- unless
		inArr[c] = inArr[c++] * 0.1f;   // 20 armor max?
		inArr[c] = inArr[c++] * 0.1f;   // 12 if netherite, can be higher
		inArr[c] = inArr[c++] * 0.025f; // 40 food
		inArr[c] = inArr[c++] * 0.1f;   // ~7-10 vanilla
		inArr[c] = inArr[c++] * 0.1f;   // ~7->100 depend on gun
		inArr[c] = inArr[c++] * 0.1f;   // most gun have 30-40 max per magazine
		inArr[c] = (float) (Math.log(1 + inArr[c++]) * 0.22f);   //  total ammo count usually in 100 estimate
		// entity group
		c = scaleEntityGroup(inArr, c);  // hostile
		c = scaleEntityGroup(inArr, c);  // friendly
		// interested entity
		c = scaleEntityDat(inArr, c);  // current target
		c = scaleEntityDat(inArr, c);  // retaliation
		c = scaleEntityDat(inArr, c);  // nearest
		c = scaleEntityDat(inArr, c);  // highest health
		//terrain
		scaleTerrainDat(inArr, c);
	}

	/// Experience Storage
	public boolean getCollectExp() {return collectExp;}
	public void trySetCollectExp(boolean mode) {
		if (mode) {
			ItemStack item = agent.getChipBrainStack();
			if (item.getItem() instanceof BrainChipItem brainChipItem) {
				MlModelManager.MLUnit unit =
					MlModelManager.getMUnit(brainChipItem.getOrCreateUUID(item), agent.level().getGameTime());
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
	public void pushDeepChain() {
		agent.printObservation(agent.getDisplayName().getString() + " going cold ", agent.level().getServer());
		if (currentChain.isEmpty()) return;
		ItemStack item = agent.getChipBrainStack();  // this check can be remove once func state map correctly
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, agent.level().getGameTime());
			if (mUnit.dataManager == null) {
				collectExp = false;  // turn this off
			} else {
				mUnit.dataManager.add(
					currentChain);  // fixme critical swap the memory manager to revolving memory slider -- OR MAYBE DONT :), sure it store more but hey so what
				currentChain = new ArrayList<>();
			}
		}
	}

	/// logic handling
	protected int lastActiveTick = 0;
	protected int nextScanTick = 0;
	protected int currentThrottleRate = 20;
	public void lockComputing() {computing = true;}
	public void doneComputing() {computing = false;}
	public void forceAwake() {
		this.currentThrottleRate = 20;
		this.nextScanTick = Math.min(agent.tickCount + currentThrottleRate, nextScanTick); // fast tract if still asleep
	}
	public void onBehaviorStart(int startB) {
		super.onBehaviorStart(startB);
		System.arraycopy(payload, 0, statePre, 0, agent.getInputSpace());  // copy the arr
		agent.printObservation(agent.getDisplayName().getString() + " starting " + behaviors.get(startB),
			agent.level().getServer());
	}
	public void onBehaviorStop(int stopB) {
		agent.printObservation(agent.getDisplayName().getString() + " stopping " + behaviors.get(stopB),
			agent.level().getServer());
		if (!collectExp) return;  // only run with collector on
		ItemStack item = agent.getChipBrainStack();  // this check can be remove once func state map correctly
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, agent.level().getGameTime());
			if (mUnit.dataManager == null) {
				collectExp = false;  // turn this off
			} else {
				// state post
				long mask = scoreFunc.getMaskB();
				evalSensorF(statePost, mask);
				// custom func
				mask = scoreFunc.getMaskC();
				evalCustF(stateCust, mask);
				currentChain.add(
					new DataManager.itemUnit(payloadLastRun.clone(), stopB, scoreFunc.evaluate(statePre, statePost, stateCust)));
			}
		}
	}

	private void resetHiddenStateToZeros() {
		ItemStack item = agent.getChipBrainStack();
		if (item.getItem() instanceof BrainChipItem brainChipItem) {  // fixme critical see if this can be cache and only scan when inventory touch
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, agent.level().getGameTime());
			if (mUnit.model != null) mUnit.model.clearHidden();
		}
	}
	private boolean deviationDetected(float oldVal, float newVal, float thresRatio) {
		float diff = Math.abs(newVal - oldVal);
		return diff > 0.01f && diff > (Math.abs(oldVal) * thresRatio);
	}
	private boolean detectDeviation() {
		for (int i = 0; i < agent.getInputSpace(); i++) { // will trigger cache hot wire
			// compare the send data aka what trigger compute vs the current reading
			if (deviationDetected(statePre[i], payload[i], inputDeviation[i])) {
				agent.printObservation(String.format("[%s] Deviation Input at: %d | Pre: %.4f | Post: %.4f | Limit: %.4f",
						agent.getDisplayName().getString(), i, statePre[i], payload[i], inputDeviation[i]),
					agent.level().getServer());
				return true;
			}
		}
		return false;
	}
	private void runRandomBehavior() {
		// random exploration -- for encouraged new behavior testing
		int[] randomIdx = new int[agent.getOutputSpace()];
		for (int b = 0; b < agent.getOutputSpace(); b++) randomIdx[b] = b;

		// Fast Fisher-Yates shuffle for the behavior indices
		for (int b = agent.getOutputSpace() - 1; b > 0; b--) {
			int swap = agent.getRandom().nextInt(b + 1);
			int temp = randomIdx[b];
			randomIdx[b] = randomIdx[swap];
			randomIdx[swap] = temp;
		}
		tryBehaviorChain(randomIdx);
	}
	private void runBrainInf() {
		ItemStack item = agent.getChipBrainStack();
		if (item.getItem() instanceof BrainChipItem brainChipItem) {
			// brain item tag
			UUID chipTag = brainChipItem.getOrCreateUUID(item);
			// verified Live Brain
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, agent.level().getGameTime());
			if (mUnit.model == null || mUnit.model.getInsize() != agent.getInputSpace() || mUnit.model.getOutsize() != agent.getOutputSpace()) {
				agent.brainOFF();
			} else {
				if (BRAIN_SERVER.TASK_QUEUE_INF.offer(new MLServer.InfDatIn(agent.getUUID(), payloadLastRun, mUnit.model))) {
					//critical fixme later handle case where thread full -- better
					lockComputing();  // mark in computational mode
//					lastActiveTick = agent.tickCount;
				}
			}
		}
	}

	public void preTick() {
		int currentTick = agent.tickCount;

		// Ensure brain in Hot State if active  fixme when a behavior stop, did it put this back to -1?
		if (this.activeBehaviours != -1) {
			lastActiveTick = currentTick;
			forceAwake();
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
		packInput(payload); // raw, not scale

		// is there a reason to run
		if (!forceTrigger && !detectDeviation()) return;
		forceTrigger = false;

		System.arraycopy(payload, 0, payloadLastRun, 0, agent.getInputSpace());  // copy the arr
		scaleCommonInput(payloadLastRun);  // scale for ML
		if (exploreRate > 0f && agent.getRandom().nextFloat() < exploreRate) runRandomBehavior();
		else runBrainInf();
	}

	// PLS overwrite these
	public abstract void evalSensorF(float[] state, long mask);
	public abstract void evalCustF(float[] state, long mask);

	protected abstract void packInput(float[] payload);
}
