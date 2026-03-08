package com.github.ptran779.breach_ptc.item;

import com.github.ptran779.breach_ptc.server.ParticleInit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class TheGreatDebugStick extends Item {
	public TheGreatDebugStick(Properties pProperties) {
		super(pProperties);
	}

	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		// Spawn slightly above the clicked block
		double x = context.getClickedPos().getX();
		double y = context.getClickedPos().getY() + 3;
		double z = context.getClickedPos().getZ();

		if (!level.isClientSide) {
			ServerLevel serverLevel = (ServerLevel) level;
			// Send the packet to all nearby clients
			serverLevel.sendParticles(
				ParticleInit.MAGAZINE_PARTICLE.get(), // The ID
				x, y, z,
				1,       // Count
				0.0, 1.0, 0.0, // Speed/Delta
				0.0      // Speed multiplier
			);
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
