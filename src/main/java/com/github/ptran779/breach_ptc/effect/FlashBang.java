package com.github.ptran779.breach_ptc.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class FlashBang extends MobEffect {

	public FlashBang(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// f u no target for you
		if (entity instanceof Mob mob) {
			mob.setTarget(null);
			mob.setAggressive(false);
			mob.getNavigation().stop();
		}

		// 3. The Visuals: Short-circuiting brain
		if (entity.level().isClientSide()) {
			// A cluster of sparks around their head every 0.5s
			for (int i = 0; i < 3; i++) {
				entity.level().addParticle(ParticleTypes.FLASH,
					entity.getRandomX(0.5D), entity.getEyeY() + 0.2D, entity.getRandomZ(0.5D),
					0, 0.1, 0);
			}
		}
	}

	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 5 == 0;
	}
}