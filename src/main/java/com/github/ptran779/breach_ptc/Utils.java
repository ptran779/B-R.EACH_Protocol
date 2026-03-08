package com.github.ptran779.breach_ptc;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.extra.FallingHellPod;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

import static com.github.ptran779.breach_ptc.server.EntityInit.*;

public class Utils {
  private static final List<String> MALE_FN = List.of("James", "Michael", "John", "Robert", "David", "William", "Richard", "Joseph", "Thomas", "Christopher", "Charles", "Daniel", "Matthew", "Anthony", "Mark", "Steven", "Donald", "Andrew", "Joshua", "Paul", "Kenneth", "Kevin", "Brian", "Timothy", "Ronald", "Jason", "George", "Edward", "Jeffrey", "Ryan", "Jacob", "Nicholas", "Gary", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Benjamin", "Scott", "Brandon", "Samuel", "Gregory", "Alexander", "Patrick", "Frank", "Jack", "Raymond", "Dennis", "Tyler", "Aaron", "Jerry", "Jose", "Nathan", "Adam", "Henry", "Zachary", "Douglas", "Peter", "Noah", "Kyle", "Ethan", "Christian", "Jeremy", "Keith", "Austin", "Sean", "Roger", "Terry", "Walter", "Dylan", "Gerald", "Carl", "Jordan", "Bryan", "Gabriel", "Jesse", "Harold", "Lawrence", "Logan", "Arthur", "Bruce", "Billy", "Elijah", "Joe", "Alan", "Juan", "Liam", "Willie", "Mason", "Albert", "Randy", "Wayne", "Vincent", "Lucas", "Caleb", "Luke", "Bobby", "Isaac", "Bradley");
  private static final List<String> FEMALE_FN = List.of("Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Karen", "Sarah", "Lisa", "Nancy", "Sandra", "Ashley", "Emily", "Kimberly", "Betty", "Margaret", "Donna", "Michelle", "Carol", "Amanda", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia", "Amy", "Kathleen", "Angela", "Dorothy", "Shirley", "Emma", "Brenda", "Nicole", "Pamela", "Samantha", "Anna", "Katherine", "Christine", "Debra", "Rachel", "Olivia", "Carolyn", "Maria", "Janet", "Heather", "Diane", "Catherine", "Julie", "Victoria", "Helen", "Joyce", "Lauren", "Kelly", "Christina", "Joan", "Judith", "Ruth", "Hannah", "Evelyn", "Andrea", "Virginia", "Megan", "Cheryl", "Jacqueline", "Madison", "Sophia", "Abigail", "Teresa", "Isabella", "Sara", "Janice", "Martha", "Gloria", "Kathryn", "Ann", "Charlotte", "Judy", "Amber", "Julia", "Grace", "Denise", "Danielle", "Natalie", "Alice", "Marilyn", "Diana", "Beverly", "Jean", "Brittany", "Theresa", "Frances", "Kayla", "Alexis", "Tiffany", "Lori", "Kathy");
  private static final List<String> LN = List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzales", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy", "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper", "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson", "Watson", "Brooks", "Chavez", "Wood", "James", "Bennet", "Gray", "Mendoza", "Ruiz", "Hughes", "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers", "Long", "Ross", "Foster", "Jimenez");

  private static final List<EntityType<? extends AbsAgentEntity>> AGENT_POOL = List.of(
    SOLDIER.get(),
    SNIPER.get(),
    HEAVY.get(),
    DEMOLITION.get(),
    MEDIC.get(),
    ENGINEER.get(),
    SWORDMAN.get()
  );

  public static AbsAgentEntity getRandomAgent(Level level) {return AGENT_POOL.get(level.random.nextInt(AGENT_POOL.size())).create(level);}

  public static BlockPos findSolidGroundBelow(BlockPos start, Level level) {
    BlockPos.MutableBlockPos pos = start.mutable();
    while (pos.getY() > level.getMinBuildHeight()) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir()) {return pos.immutable();}
      pos.move(Direction.DOWN);
    }
    return null; // No ground found (shouldn't happen)
  }

  public static String randomName(boolean isFemale) {
    String first = isFemale
        ? FEMALE_FN.get(ThreadLocalRandom.current().nextInt(FEMALE_FN.size()))
        : MALE_FN.get(ThreadLocalRandom.current().nextInt(MALE_FN.size()));
    String last = LN.get(ThreadLocalRandom.current().nextInt(LN.size()));
    return first + " " + last;
  }

  public static void summonReinforcement(double x, double y, double z, ServerLevel level){
    // generate random trajectory, and perform squad spawning
    float xRandTraj = (level.random.nextFloat()-0.5f) * 0.025f;
    float zRandTraj = (level.random.nextFloat()-0.5f) * 0.025f;
    FallingHellPod pod = new FallingHellPod(FALLING_HELL_POD.get(), level);
    pod.setDrift(xRandTraj, zRandTraj);
    pod.setPos(x, y, z);
    level.addFreshEntity(pod);
    // Spawn agent
    AbsAgentEntity agent = getRandomAgent(level);
    agent.initCosmetic();
    level.addFreshEntity(agent);
    //agent ride pod
    agent.startRiding(pod, true); // force = true in case agent is riding something else
  }

//  public enum TargetMode {
//    OFF,                        // Do not scan or target
//    HOSTILE_ONLY,               // Hostile mobs only
//    ENEMY_AGENTS,  // Hostile mobs + agents not on owner's team
//    ALL;         // Anything not on owner's team
//
//    public static final TargetMode[] VALUES = values();
//    public static TargetMode fromId(int id) {
//      return VALUES[id];
//    }
//    public static TargetMode nextTargetMode(int id) {
//      int next = (id + 1) % VALUES.length;
//      return VALUES[next];
//    }
//  }
//
//  public enum FollowMode {
//    WANDER,     // Move around randomly
//    STAY,       // Stay in place
//    FOLLOW;     // Follow the owner/player
//
//    public static final FollowMode[] VALUES = values();
//
//    public static FollowMode fromId(int id) {
//      return VALUES[id];
//    }
//
//    public static FollowMode nextFollowMode(int id) {
//      int next = (id + 1) % VALUES.length;
//      return VALUES[next];
//    }
//  }

	public static <U extends Entity, T extends Entity> T findNearestEntity(U user, Class<T> type, double radius, Predicate<T> func) {
    AABB box = user.getBoundingBox().inflate(radius);
    List<T> entities = user.level().getEntitiesOfClass(type, box, func);

    return entities.stream()
        .min(Comparator.comparingDouble(user::distanceToSqr))
        .orElse(null);
  }

	public static Player findNearestPlayer(Entity user, double radius, Predicate<Player> func) {
		List<? extends Player> players = user.level().players();

		Player nearest = null;
		double closestDistSq = radius * radius;

		for (Player player : players) {
			if (player.isAlive() &&
				!player.isSpectator() &&
				!player.isCreative() &&
				player != user) {

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
		List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, area, entity ->
			entity != user &&
				entity.isAlive() &&
				user.distanceToSqr(entity) <= radiusSq
		);
		return entities;
	}

  public static boolean hasFriendlyInLineOfFire(Mob user, LivingEntity target) {
    if (!(user instanceof IEntityTeam userTeam)) return false;
    if (target == null) return false;
    Vec3 start = user.getEyePosition();
    Vec3 end = target.getEyePosition();

    AABB pathAABB = new AABB(start, end).inflate(0.5); // widen slightly for tall hitbox
    List<LivingEntity> teammates = user.level().getEntitiesOfClass(
        LivingEntity.class,pathAABB, other ->
            other instanceof IEntityTeam otherTeam &&
            userTeam.isFriendlyMod(otherTeam, other.level()) &&
            other.getBoundingBox().clip(start, end).isPresent()
    );
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
    base = Normalizer.normalize(base, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}", ""); // removes diacritics

    // 4. Replace illegal ResourceLocation characters with _
    base = base.replaceAll("[^a-z0-9._-]", "_");

    // 5. Collapse multiple underscores and trim edges
    base = base.replaceAll("_+", "_")
        .replaceAll("^_|_$", "");

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

	// cast ray cast between 2 point and return true if anything blocking
	public static boolean rayCastHit(Vec3 pos1, Vec3 pos2, ServerLevel level) {
		return level.clip(new ClipContext(pos1,pos2,ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,null)).getType() == HitResult.Type.BLOCK;
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
}
