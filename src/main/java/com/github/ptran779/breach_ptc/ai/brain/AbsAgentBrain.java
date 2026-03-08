package com.github.ptran779.breach_ptc.ai.brain;

import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.ai.api.Brain;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
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
import java.util.List;
import java.util.function.Function;

/// this it not the ML brain, this is support for enhancing brain and sensor info.
/// ML weight model is stored in centralized storage else where
public abstract class AbsAgentBrain extends Brain {
	@NotNull AbsAgentEntity agent;

	protected Sensor<Player> bossS;
	protected Sensor<Boolean> clearLosS;
	protected Sensor<Double> targetSqS;
	protected Sensor<List<LivingEntity>> liveEntShortRS, hostileShortRS, friendlyShortRS, liveEntLongRS, hostileLongRS;
	protected Sensor<LivingEntity> retarHostileS, nearestHostileS, highestHealthHostileS;
	protected Sensor<Float> hostileShortRTotalHS, friendlyShortRTotalHS;
	protected Sensor<ItemStack> bestFoodIS;
	protected Sensor<Boolean> gunIS;
	// fixme not necessary? just do direct check : gun item, ammo in chamber, virt ammo, inv ammo -- throttle
	protected Sensor<Float> meleeDmgS;  // a bit overkill?
	protected Sensor<Integer> totalAmmoCountS, ammoInChamberS, maxAmmoInChamberS;
	protected Sensor<List<Vec3>> lidar12RS;

	public AbsAgentBrain(@NotNull AbsAgentEntity agent) {
		super(agent);
		this.agent = agent;
	}

	protected void sensorInit() {
		bossS = new Sensor<>(() -> Utils.findNearestPlayer(agent, getShortRangeScan(), player -> agent.getBossUUID().equals(player.getUUID())),40);;
		bestFoodIS = new Sensor<>(agent.inventory1::getBestFood, 40);
		meleeDmgS = new Sensor<>(() -> {
			ItemStack stack = agent.inventory1.getItem(agent.MELEE_SLOT);
			if (stack.isEmpty()) return 1.0f;
			var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
			return (float) (1.0 + modifiers.get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(AttributeModifier::getAmount)
				.sum());
		}, 20);
		totalAmmoCountS = new Sensor<>(() -> agent.inventory1.totalUsableAmmo(), 80);
		ammoInChamberS = new Sensor<>(() -> agent.inventory1.checkAmmoInChamber(), 10);
		maxAmmoInChamberS = new Sensor<>(() -> agent.inventory1.maxAmmoInChamber(), 10);

		gunIS = new Sensor<>(agent.inventory1::gunExistWithAmmo, 40);
		clearLosS = new Sensor<>(() -> Utils.hasFriendlyInLineOfFire(agent, agent.getTarget()), 60);
		targetSqS = new Sensor<>(() -> {
			LivingEntity target = agent.getTarget();
			if (target != null && target.isAlive()) return agent.distanceToSqr(target);
			return -1.0d;  // just super far away AKA non existence
		});
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
					liveEnt) || liveEnt.getHealth() < highestH) continue;
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
				target) || agent.tickCount - agent.getLastHurtByMobTimestamp() > 600)
				return null;  // only bother with 10s enemy retar
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
				target = findNearestHostileInList(liveEntLongRS.get(agent.tickCount));
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
	public void activateGoalWrapper() {}

	/// Utils
	protected double getShortRangeScan() {return 12;}
	protected double getLongRangeScan() {return 32;}

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

		for (LivingEntity liveEnt : entities) {
			// Skip nulls, dead entities, and the agent itself
			if (liveEnt == null || !liveEnt.isAlive() || liveEnt == agent) continue;

			double distSqr = agent.distanceToSqr(liveEnt);

			// Only run the heavier targeting check if this entity is actually closer
			if (distSqr < minDistanceSqr) {
				if (agent.shouldTargetEntity(agent, liveEnt)) {
					minDistanceSqr = distSqr;
					bestFound = liveEnt;
				}
			}
		}

		return bestFound;
	}

	@FunctionalInterface
	public interface Feature<E extends AbsAgentEntity, B extends AbsAgentBrain> {
		float eval(E agent, B brain);
	}

	protected static <E extends AbsAgentEntity, B extends AbsAgentBrain>
	void registerEntityGroup(List<Feature<E, B>> list, Function<AbsAgentBrain, List<LivingEntity>> listSupplier,
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
	protected static <E extends AbsAgentEntity, B extends AbsAgentBrain>
	void registerEntityDat(List<Feature<E, B>> list, Function<AbsAgentBrain,
		LivingEntity> entitySupplier) {
		list.add((agent, brain) -> entitySupplier.apply(brain) != null && entitySupplier.apply(brain).isAlive() ? 1f : 0f);
		list.add((agent, brain) -> {
			LivingEntity e = entitySupplier.apply(brain);
			return e != null && e.isAlive() ? e.getHealth() : 0f;
		});
		list.add((agent, brain) -> {
			LivingEntity e = entitySupplier.apply(brain);
			return e != null && e.isAlive() ? agent.distanceTo(e) : 0f;
		});
	}
	protected static <E extends AbsAgentEntity, B extends AbsAgentBrain>
	void registerTerrainGroup(List<Feature<E, B>> list, Function<AbsAgentBrain
		, List<Vec3>> lidarSupplier) {
		list.add((agent, brain) -> Utils.computeRadialCentroid(lidarSupplier.apply(brain), agent));
		list.add((agent, brain) -> Utils.computeRadialStdCentroid(lidarSupplier.apply(brain)));
		list.add((agent, brain) -> Utils.computeHeightCentroid(lidarSupplier.apply(brain), agent));
		list.add((agent, brain) -> Utils.computeHeightStd(lidarSupplier.apply(brain)));
	}
	public static <E extends AbsAgentEntity, B extends AbsAgentBrain> void registerCommonSensor(List<Feature<E, B>> list) {
		// last compute time 0
		list.add((agent, brain) -> agent.tickCount - brain.lastUpdateTick);
		// self 1-8
		list.add((agent, brain) -> agent.getHealth());
		list.add((agent, brain) -> (float) agent.getAttributeValue(Attributes.ARMOR));
		list.add((agent, brain) -> (float) agent.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		list.add((agent, brain) -> agent.getFood());
		list.add((agent, brain) -> brain.meleeDmgS.get(agent.tickCount));
		list.add((agent, brain) -> agent.getGunDmg());
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


	// For CSV export support
	public static String getCSVHeader() {
		return "GameID," +
			"TimeSinceUpdate," +
			"SelfHealth,SelfArmor,SelfToughness,SelfFood,SelfMeleeDmg,SelfGunDmg,SelfAmmoChamber,SelfTotalAmmo," +
			"HostileGrp_Size,HostileGrp_TotalHP,HostileGrp_AvgHP,HostileGrp_CentroidDist,HostileGrp_StdDev," +
			"FriendlyGrp_Size,FriendlyGrp_TotalHP,FriendlyGrp_AvgHP,FriendlyGrp_CentroidDist,FriendlyGrp_StdDev," +
			"Target_Exists,Target_HP,Target_Dist," +
			"Retal_Exists,Retal_HP,Retal_Dist," +
			"Nearest_Exists,Nearest_HP,Nearest_Dist," +
			"HighHP_Exists,HighHP_HP,HighHP_Dist," +
			"TerrainRadialCentroid,TerrainRadialStd,TerrainHeightCentroid,TerrainHeightStd," +
			"ActionTaken,Score";
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
}
