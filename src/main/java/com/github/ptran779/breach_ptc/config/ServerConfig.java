package com.github.ptran779.breach_ptc.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ServerConfig {
	public static ForgeConfigSpec.BooleanValue PLAYER_DROP_POD;
	public static ForgeConfigSpec.DoubleValue BD_TURRET_DPS;
	public static ForgeConfigSpec.IntValue BD_TURRET_CHARGE_MAX;
	public static ForgeConfigSpec.IntValue PORT_DIS_CHARGE_MAX;

	public static ForgeConfigSpec.IntValue ENGI_WORK_RECHARGE, VIRT_AMMO_REFILL;
	public static ForgeConfigSpec.IntValue BANDAGE_HEALTH_REFILL;

	public static ForgeConfigSpec.IntValue VP_MIN_TARGET_HEALTH;
	// grenade
	public static ForgeConfigSpec.IntValue GRENADE_FRAG_CLUSTER, GRENADE_INCENDIARY_CLUSTER, GRENADE_EMP_CLUSTER,
	GRENADE_CORROSIVE_CLUSTER, GRENADE_CRYO_CLUSTER, GRENADE_FLASHBANG_CLUSTER;

	public static void register() {
		registerCommonConfig();
	}

	private static void registerCommonConfig() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("Player Settings").push("player");
		PLAYER_DROP_POD = builder.comment("Should first time player join in enter in droppod?").define("PlayerDropPod", true);
		builder.pop();

		builder.comment("Structure Settings").push("structure");
		BD_TURRET_DPS =
			builder.comment("Double Barrel Bullet Damage per shot").defineInRange("BDTurretDps", 6f, 0f, Float.MAX_VALUE);
		BD_TURRET_CHARGE_MAX =
			builder.comment("Double Barrel Bullet Max Charge").defineInRange("BDTurretChargeMax", 100, 0, Integer.MAX_VALUE);
		PORT_DIS_CHARGE_MAX =
			builder.comment("Portable Dispenser Max Charge").defineInRange("PortDisMaxCharge", 200, 0, Integer.MAX_VALUE);
		builder.pop();

		builder.comment("Agents settings").push("agent");
		ENGI_WORK_RECHARGE = builder.comment("How much charge per work should engineer refill")
			.defineInRange("EngiWorkVal", 20, 0, Integer.MAX_VALUE);

		VIRT_AMMO_REFILL = builder.comment("How much virtual ammo can be refill per action")
			.defineInRange("VirtAmmoRefill", 80, 0, Integer.MAX_VALUE);

		BANDAGE_HEALTH_REFILL = builder.comment("How much health can bandage restore")
			.defineInRange("BandageRestoreAmount", 6, 0, Integer.MAX_VALUE);

		builder.comment("grenade usage for goal system").push("grenade_condition");
		GRENADE_FRAG_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the frag?")
			.defineInRange("EnemyClusterFrag", 4, 0, Integer.MAX_VALUE);
		GRENADE_INCENDIARY_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the incendiary?")
			.defineInRange("EnemyClusterIncendiary", 4, 0, Integer.MAX_VALUE);
		GRENADE_EMP_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the incendiary?")
			.defineInRange("EnemyClusterIncendiary", 3, 0, Integer.MAX_VALUE);
		GRENADE_CRYO_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the incendiary?")
			.defineInRange("EnemyClusterIncendiary", 3, 0, Integer.MAX_VALUE);
		GRENADE_CORROSIVE_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the incendiary?")
			.defineInRange("EnemyClusterIncendiary", 3, 0, Integer.MAX_VALUE);
		GRENADE_FLASHBANG_CLUSTER = builder.comment(
				"How many entity needed within the target enemy cluster to throw the incendiary?")
			.defineInRange("EnemyClusterIncendiary", 5, 0, Integer.MAX_VALUE);

		VP_MIN_TARGET_HEALTH = builder.comment(
				"How much health require minimum for vector pursuer to trigger (so only chase after high value target)")
			.defineInRange("VPMinimumHeathTarget", 30, 0, Integer.MAX_VALUE);

		builder.pop();
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build());
	}
}
