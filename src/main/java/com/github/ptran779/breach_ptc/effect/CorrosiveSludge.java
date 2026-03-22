package com.github.ptran779.breach_ptc.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.particles.ParticleTypes;

public class CorrosiveSludge extends MobEffect {
	public CorrosiveSludge(MobEffectCategory pCategory, int pColor) {
		super(pCategory, pColor);
		// The Passive: Halves their total armor while the effect is active.
		this.addAttributeModifier(Attributes.ARMOR, "1a110dd9-78c2-4945-a579-97292beac9c0", -0.5D,
			AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (EquipmentSlot slot : armorSlots) {
			ItemStack armorPiece = entity.getItemBySlot(slot);
			if (!armorPiece.isEmpty() && armorPiece.isDamageableItem()) {
				int acidDamage = 3 + amplifier;

				armorPiece.hurtAndBreak(acidDamage, entity, (e) -> {
					e.broadcastBreakEvent(slot);
				});
			}
		}

		// The Visual: Toxic splashing
		if (entity.level().isClientSide()) {
			entity.level().addParticle(ParticleTypes.SNEEZE,
				entity.getRandomX(0.5D), entity.getRandomY(), entity.getRandomZ(0.5D), 0, -0.05, 0);
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// Ticks exactly once per second
		return duration % 20 == 0;
	}
}