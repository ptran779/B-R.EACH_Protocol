package com.github.ptran779.breach_ptc.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;

public class ThermalBleed extends MobEffect {
	public ThermalBleed(MobEffectCategory pCategory, int pColor) {
		super(pCategory, pColor);
	}

	public void applyEffectTick(LivingEntity entity, int amplifier) {
		entity.hurt(entity.damageSources().magic(), 1.0F + (float) amplifier /2);

		if (entity.level().isClientSide()) { // Added parentheses here
			entity.level().addParticle(ParticleTypes.LAVA,
				entity.getRandomX(0.5D), entity.getRandomY(), entity.getRandomZ(0.5D), 0, 0.05, 0);
		}
	}

	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}
}