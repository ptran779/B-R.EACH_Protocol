package com.github.ptran779.breach_ptc;

import com.github.ptran779.breach_ptc.config.AgentConfig;
import com.github.ptran779.breach_ptc.entity.agent.*;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeamNTarget;
import com.github.ptran779.breach_ptc.entity.extra.KSeedCore;
import com.github.ptran779.breach_ptc.entity.extra.VoidDrifterModule;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.ptran779.breach_ptc.server.EntityInit.*;

public class Utils {
	private static final List<String> MALE_FN =
		List.of("James", "Michael", "John", "Robert", "David", "William", "Richard", "Joseph", "Thomas", "Christopher",
			"Charles", "Daniel", "Matthew", "Anthony", "Mark", "Steven", "Donald", "Andrew", "Joshua", "Paul", "Kenneth",
			"Kevin", "Brian", "Timothy", "Ronald", "Jason", "George", "Edward", "Jeffrey", "Ryan", "Jacob", "Nicholas",
			"Gary", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Benjamin", "Scott", "Brandon", "Samuel", "Gregory",
			"Alexander", "Patrick", "Frank", "Jack", "Raymond", "Dennis", "Tyler", "Aaron", "Jerry", "Jose", "Nathan", "Adam",
			"Henry", "Zachary", "Douglas", "Peter", "Noah", "Kyle", "Ethan", "Christian", "Jeremy", "Keith", "Austin", "Sean",
			"Roger", "Terry", "Walter", "Dylan", "Gerald", "Carl", "Jordan", "Bryan", "Gabriel", "Jesse", "Harold",
			"Lawrence", "Logan", "Arthur", "Bruce", "Billy", "Elijah", "Joe", "Alan", "Juan", "Liam", "Willie", "Mason",
			"Albert", "Randy", "Wayne", "Vincent", "Lucas", "Caleb", "Luke", "Bobby", "Isaac", "Bradley");
	private static final List<String> FEMALE_FN =
		List.of("Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Karen", "Sarah",
			"Lisa", "Nancy", "Sandra", "Ashley", "Emily", "Kimberly", "Betty", "Margaret", "Donna", "Michelle", "Carol",
			"Amanda", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia", "Amy", "Kathleen", "Angela",
			"Dorothy", "Shirley", "Emma", "Brenda", "Nicole", "Pamela", "Samantha", "Anna", "Katherine", "Christine", "Debra",
			"Rachel", "Olivia", "Carolyn", "Maria", "Janet", "Heather", "Diane", "Catherine", "Julie", "Victoria", "Helen",
			"Joyce", "Lauren", "Kelly", "Christina", "Joan", "Judith", "Ruth", "Hannah", "Evelyn", "Andrea", "Virginia",
			"Megan", "Cheryl", "Jacqueline", "Madison", "Sophia", "Abigail", "Teresa", "Isabella", "Sara", "Janice", "Martha",
			"Gloria", "Kathryn", "Ann", "Charlotte", "Judy", "Amber", "Julia", "Grace", "Denise", "Danielle", "Natalie",
			"Alice", "Marilyn", "Diana", "Beverly", "Jean", "Brittany", "Theresa", "Frances", "Kayla", "Alexis", "Tiffany",
			"Lori", "Kathy");
	private static final List<String> LN =
		List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
			"Hernandez", "Lopez", "Gonzales", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee",
			"Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
			"Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams", "Nelson", "Baker",
			"Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz",
			"Parker", "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy", "Cook", "Rogers",
			"Gutierrez", "Ortiz", "Morgan", "Cooper", "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox",
			"Ward", "Richardson", "Watson", "Brooks", "Chavez", "Wood", "James", "Bennet", "Gray", "Mendoza", "Ruiz",
			"Hughes", "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers", "Long", "Ross", "Foster", "Jimenez");

	public record spawnPool(EntityType<? extends AbsAgentEntity> type, Supplier<AgentConfig> config) {}

	public record AgentData(boolean female, String skin, int agentPoolIdx) {}

	public static final List<spawnPool> SPAWN_POOL =
		List.of(new spawnPool(SOLDIER.get(), () -> Soldier.config), new spawnPool(SNIPER.get(), () -> Sniper.config),
			new spawnPool(HEAVY.get(), () -> Heavy.config), new spawnPool(DEMOLITION.get(), () -> Demolition.config),
			new spawnPool(MEDIC.get(), () -> Medic.config), new spawnPool(ENGINEER.get(), () -> Engineer.config),
			new spawnPool(SWORDMAN.get(), () -> Swordman.config));

	public static BlockPos findSolidGroundBelow(BlockPos start, Level level) {
		BlockPos.MutableBlockPos pos = start.mutable();
		while (pos.getY() > level.getMinBuildHeight()) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				return pos.immutable();
			}
			pos.move(Direction.DOWN);
		}
		return null; // No ground found (shouldn't happen)
	}

	public static String randomName(boolean isFemale) {
		String first = isFemale ? FEMALE_FN.get(ThreadLocalRandom.current().nextInt(FEMALE_FN.size())) : MALE_FN.get(
			ThreadLocalRandom.current().nextInt(MALE_FN.size()));
		String last = LN.get(ThreadLocalRandom.current().nextInt(LN.size()));
		return first + " " + last;
	}

	public static void summonReinforcement(double x, double y, double z, ServerLevel level) {
		float speed = 0.5F;
		int height = 200;  // fixme

		// Spawn above the target location at specified height
		double spawnX = x;
		double spawnY = y + height;
		double spawnZ = z;

		VoidDrifterModule voidDrifter = new VoidDrifterModule(VOID_DRIFTER_MODULE_ENT.get(), level);
		voidDrifter.setPos(spawnX, spawnY, spawnZ);

		KSeedCore kSeedCore = new KSeedCore(K_SEED_CORE_ENT.get(), level);
		kSeedCore.setPos(spawnX, spawnY, spawnZ);
		kSeedCore.startRiding(voidDrifter);
		kSeedCore.setSpawnAgent(true);

		// Random horizontal direction
		float yaw = level.random.nextFloat() * 360F;
		float yawRad = yaw * (float) (Math.PI / 180F);
		voidDrifter.setDeltaMovement(Mth.sin(-yawRad) * speed, 0D, Mth.cos(yawRad) * speed);

		level.addFreshEntity(voidDrifter);
		level.addFreshEntity(kSeedCore);
	}

	public static Player findNearestPlayer(Entity user, double radius, Predicate<Player> func) {
		List<? extends Player> players = user.level().players();

		Player nearest = null;
		double closestDistSq = radius * radius;

		for (Player player : players) {
			if (player.isAlive() && !player.isSpectator() && !player.isCreative() && player != user) {

				double distSq = user.distanceToSqr(player);
				if (distSq < closestDistSq && func.test(player)) {
					closestDistSq = distSq;
					nearest = player;
				}
			}
		}

		return nearest;
	}

	public static List<LivingEntity> getAllLivingInRange(Entity user, double radius) {
		AABB area = user.getBoundingBox().inflate(radius);
		double radiusSq = radius * radius;
		List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, area,
			entity -> entity != user && entity.isAlive() && user.distanceToSqr(entity) <= radiusSq);
		return entities;
	}

	public static boolean hasFriendlyInLineOfFire(Mob user, LivingEntity target) {
		if (!(user instanceof IEntityTeamNTarget userTeam)) return false;
		if (target == null) return false;
		Vec3 start = user.getEyePosition();
		Vec3 end = target.getEyePosition();

		AABB pathAABB = new AABB(start, end).inflate(0.5); // widen slightly for tall hitbox
		List<LivingEntity> teammates = user.level().getEntitiesOfClass(LivingEntity.class, pathAABB,
			other -> other != user && userTeam.isAlly(other) && other.getBoundingBox().clip(start, end).isPresent());
		return !teammates.isEmpty();
	}

	//use for both client and server to help with setting default skin
	public static String makeSafeSkinName(String rawFileName) {
		if (rawFileName == null || rawFileName.isEmpty()) return "skin_default";

		// 1. Strip extension if present
		int dotIndex = rawFileName.lastIndexOf('.');
		String base = (dotIndex > 0 ? rawFileName.substring(0, dotIndex) : rawFileName);

		// 2. Lowercase
		base = base.toLowerCase(Locale.ROOT);

		// 3. Normalize Unicode → ASCII
		base = Normalizer.normalize(base, Normalizer.Form.NFKD).replaceAll("\\p{M}", ""); // removes diacritics

		// 4. Replace illegal ResourceLocation characters with _
		base = base.replaceAll("[^a-z0-9._-]", "_");

		// 5. Collapse multiple underscores and trim edges
		base = base.replaceAll("_+", "_").replaceAll("^_|_$", "");

		// 6. Fallback if empty after cleaning
		if (base.isEmpty()) base = "skin";

		// 7. Append short hash to avoid collisions
		String hash = Integer.toHexString(rawFileName.hashCode());
		return base + "_" + hash;
	}

	// Helper: Given an xyz, find the nearest walkable ground level Y
	public static BlockPos findNearestGround(BlockPos pos, LivingEntity entity, int verticalRange) {
		// Check the exact spot first
		if (isValidStandingSpot(pos, entity)) return pos;
		// Scan Up and Down
		for (int i = 1; i <= verticalRange; i++) {
			// Check Above
			if (isValidStandingSpot(pos.above(i), entity)) return pos.above(i);
			// Check Below
			if (isValidStandingSpot(pos.below(i), entity)) return pos.below(i);
		}
		return null;
	}

	// Helper: Checks for Solid Floor + 2 Air Blocks
	public static boolean isValidStandingSpot(BlockPos pos, LivingEntity entity) {
		if (!entity.level().getBlockState(pos.below()).isSolidRender(entity.level(), pos.below())) return false;
		if (!entity.level().getBlockState(pos).getCollisionShape(entity.level(), pos).isEmpty()) return false;
		return entity.level().getBlockState(pos.above()).getCollisionShape(entity.level(), pos).isEmpty();
	}

	// helper: check if target in the front arch
	public static boolean isInFrontArc(LivingEntity source, Vec3 targetPos, float degrees) {
		Vec3 forward = source.getLookAngle();
		Vec3 toTarget = targetPos.subtract(source.position()).normalize();
		double dot = forward.dot(toTarget);
		return dot >= Math.cos(Math.toRadians(degrees / 2));
	}

	/// cast ray cast between 2 point and return true if anything blocking
	public static boolean rayCastHit(Vec3 pos1, Vec3 pos2, ServerLevel level) {
		return level.clip(new ClipContext(pos1, pos2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
			.getType() == HitResult.Type.BLOCK;
	}

	/**
	 * Calculates the 3D Euclidean distance from the user to the center of a group of entities.
	 */
	public static float computeCentroid(List<LivingEntity> entList, LivingEntity user) {
		if (entList.isEmpty()) return 0.0f;

		double sumX = 0, sumY = 0, sumZ = 0;
		for (LivingEntity entity : entList) {
			sumX += entity.getX();
			sumY += entity.getY();
			sumZ += entity.getZ();
		}

		int size = entList.size();
		double relX = (sumX / size) - user.getX();
		double relY = (sumY / size) - user.getY();
		double relZ = (sumZ / size) - user.getZ();

		return (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
	}

	/**
	 * Calculates the Standard Deviation (spread) of entities around their own 3D centroid.
	 */
	public static float computeStdOfCentroid(List<LivingEntity> entList) {
		if (entList.isEmpty() || entList.size() < 2) return 0.0f;

		double sumX = 0, sumY = 0, sumZ = 0;
		for (LivingEntity e : entList) {
			sumX += e.getX();
			sumY += e.getY();
			sumZ += e.getZ();
		}

		int size = entList.size();
		double meanX = sumX / size;
		double meanY = sumY / size;
		double meanZ = sumZ / size;

		double variance = 0;
		for (LivingEntity e : entList) {
			double dx = e.getX() - meanX;
			double dy = e.getY() - meanY;
			double dz = e.getZ() - meanZ;
			variance += (dx * dx + dy * dy + dz * dz);
		}

		return (float) Math.sqrt(variance / size);
	}

	/**
	 * Calculates horizontal (X/Z) distance from user to the center of a point cloud (Terrain/Lidar).
	 */
	public static float computeRadialCentroid(List<Vec3> allPoints, LivingEntity user) {
		if (allPoints.isEmpty()) return 0.0f;
		double sumX = 0, sumZ = 0;
		for (Vec3 vec : allPoints) {
			sumX += vec.x;
			sumZ += vec.z;
		}

		int size = allPoints.size();
		double relX = (sumX / size) - user.getX();
		double relZ = (sumZ / size) - user.getZ();

		return (float) Math.sqrt(relX * relX + relZ * relZ);
	}

	/**
	 * Calculates horizontal spread (Standard Deviation) of a point cloud.
	 */
	public static float computeRadialStdCentroid(List<Vec3> allPoints) {
		if (allPoints.isEmpty() || allPoints.size() < 2) return 0.0f;

		double sumX = 0, sumZ = 0;
		for (Vec3 vec : allPoints) {
			sumX += vec.x;
			sumZ += vec.z;
		}

		int size = allPoints.size();
		double meanX = sumX / size;
		double meanZ = sumZ / size;

		double variance = 0;
		for (Vec3 vec : allPoints) {
			double dx = vec.x - meanX;
			double dz = vec.z - meanZ;
			variance += (dx * dx + dz * dz);
		}

		return (float) Math.sqrt(variance / size);
	}

	/**
	 * Calculates relative height from user's feet to the average height of a point cloud.
	 */
	public static float computeHeightCentroid(List<Vec3> allPoints, LivingEntity user) {
		if (allPoints.isEmpty()) return 0.0f;
		double sumY = 0;
		for (Vec3 vec : allPoints) {
			sumY += vec.y;
		}
		return (float) ((sumY / allPoints.size()) - user.getY());
	}

	/**
	 * Calculates the vertical Standard Deviation (elevation variance) of a point cloud.
	 */
	public static float computeHeightStd(List<Vec3> allPoints) {
		if (allPoints.isEmpty() || allPoints.size() < 2) return 0.0f;

		double sumY = 0;
		for (Vec3 vec : allPoints) {
			sumY += vec.y;
		}
		double meanY = sumY / allPoints.size();

		double variance = 0;
		for (Vec3 vec : allPoints) {
			double dy = vec.y - meanY;
			variance += (dy * dy);
		}

		return (float) Math.sqrt(variance / allPoints.size());
	}

	public static void summonerTest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		// Spawn above player at specified height
		summonReinforcement(player.getX(), player.getY(), player.getZ(), context.getSource().getLevel());
	}

	public static AgentData generateRandomAgent(Level level) {
		boolean female = level.random.nextBoolean();
		int idx = level.random.nextInt(SPAWN_POOL.size());
		String defaultSkin = female ? SPAWN_POOL.get(idx).config().get().defaultFemaleSkin : SPAWN_POOL.get(idx).config()
			.get().defaultMaleSkin;
		;
		return new AgentData(female, defaultSkin, idx);
	}

	public static AbsAgentEntity generateAgent(Level level, int idx) {
		return (SPAWN_POOL.get(idx).type).create(level);
	}
}
