package com.github.ptran779.breach_ptc.item;


import com.github.ptran779.breach_ptc.entity.extra.VectorPursuer;
import com.github.ptran779.breach_ptc.server.EntityInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.github.ptran779.breach_ptc.config.ServerConfig.VP_MIN_TARGET_HEALTH;

public class VPTerminalItem extends Item {
	public VPTerminalItem(Properties pProperties) {
		super(pProperties);
	}

	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!level.isClientSide) {
			// Create the drone
			VectorPursuer drone = new VectorPursuer(EntityInit.VECTOR_PURSUER.get(), level);

			// Position the drone in front of the player
			Vec3 spawnPos = player.position().add(player.getLookAngle().scale(1.5));
			drone.setPos(spawnPos.x, spawnPos.y + 1, spawnPos.z);

			drone.deployer = player;
			drone.setTarget(findHighestHealthTarget(player));  // drone needs manual target set
			level.addFreshEntity(drone);

			// Optional: reduce item count
			if (!player.isCreative()) {
				stack.shrink(1);
			}
		}

		return InteractionResultHolder.consume(stack);
	}
	public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents,
	                            TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.literal(
			"Demolition Special Item. Deploy a seeker kamikaze drone for high health target (configurable)."));
		super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
	}

	public static <U extends Entity> LivingEntity findHighestHealthTarget(U user) {
		AABB box = user.getBoundingBox().inflate(32);
		List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, box,
			(e) -> e != null && e.isAlive() && e instanceof Enemy);
		return findHighestHealthValid(entities,  e -> e.getHealth() >= VP_MIN_TARGET_HEALTH.get());
	}

	public static LivingEntity findHighestHealthValid(List<LivingEntity> entities, Predicate<LivingEntity> extraFilter) {
		return entities.stream()
			// 1. Is it real? 2. Is it alive? 3. Does it pass whatever custom test we just injected?
			.filter(e -> e != null && e.isAlive() && (extraFilter == null || extraFilter.test(e)))
			.max(Comparator.comparingDouble(LivingEntity::getHealth))
			.orElse(null);
	}
}

