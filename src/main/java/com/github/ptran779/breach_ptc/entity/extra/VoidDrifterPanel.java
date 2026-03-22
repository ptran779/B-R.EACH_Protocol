package com.github.ptran779.breach_ptc.entity.extra;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;

public class VoidDrifterPanel extends LivingEntity {
	public static final int DESPAWN_TIME = 600;
	public float spinX = 0F, prevSpinX = 0F, spinSpeedX = 0F;
	public float spinY = 0F, prevSpinY = 0F, spinSpeedY = 0F;
	public float spinZ = 0F, prevSpinZ = 0F, spinSpeedZ = 0F;

	public VoidDrifterPanel(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);

// In constructor:
		this.spinSpeedX = (this.random.nextFloat() * 50 - 25F);
		this.spinSpeedY = (this.random.nextFloat() * 50 - 25F);
		this.spinSpeedZ = (this.random.nextFloat() * 50 - 25F);
	}

	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			// Start death sequence 20 ticks before despawn
			if (this.tickCount > DESPAWN_TIME - 20) {
				ServerLevel serverLevel = (ServerLevel) this.level();
				// Crumbling sparks
				serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					this.getX(), this.getY(), this.getZ(),
					5, 0.3, 0.3, 0.3, 0.05);
				// Smoke trail dying out
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
					this.getX(), this.getY(), this.getZ(),
					3, 0.2, 0.2, 0.2, 0.02);
				// Play sound once at start of death sequence
				if (this.tickCount == DESPAWN_TIME - 20) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
						SoundEvents.CONDUIT_DEACTIVATE,
						SoundSource.NEUTRAL, 1.0F, 0.6F);
				}
			}

			if (this.tickCount > DESPAWN_TIME) this.discard();
		}

		boolean onGround = this.onGround();

		this.prevSpinX = this.spinX;
		this.prevSpinY = this.spinY;
		this.prevSpinZ = this.spinZ;

		if (!onGround) {
			this.spinX += this.spinSpeedX;
			this.spinY += this.spinSpeedY;
			this.spinZ += this.spinSpeedZ;
			// Normal air decay
			this.spinSpeedX *= 0.98F;
			this.spinSpeedY *= 0.98F;
			this.spinSpeedZ *= 0.98F;
		} else {
			// Hit ground — kill rotation fast
			this.spinSpeedX *= 0.1F;
			this.spinSpeedY *= 0.1F;
			this.spinSpeedZ *= 0.1F;
			this.spinX += this.spinSpeedX;
			this.spinY += this.spinSpeedY;
			this.spinZ += this.spinSpeedZ;
		}
	}

	public Iterable<ItemStack> getArmorSlots() {return Collections.emptyList();}
	public ItemStack getItemBySlot(EquipmentSlot slot) {return ItemStack.EMPTY;}
	public HumanoidArm getMainArm() {return null;}
	public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}
}
