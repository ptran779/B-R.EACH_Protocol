package com.github.ptran779.breach_ptc.entity.extra;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

import static com.github.ptran779.breach_ptc.server.EntityInit.VOID_DRIFTER_PANEL;

public class VoidDrifterModule extends LivingEntity {
	public static final float EXPLODE_HEIGHT = 80;
	private float triggerY = Float.MAX_VALUE;

	public VoidDrifterModule(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}
	protected void positionRider(Entity passenger, MoveFunction moveFunc) {
		if (passenger != null && this.hasPassenger(passenger)) {
			moveFunc.accept(passenger, this.getX(), this.getY(), this.getZ());
		}
		passenger.setDeltaMovement(getDeltaMovement());
	}
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!level().isClientSide) {
			player.startRiding(this);
		}
		return InteractionResult.SUCCESS;
	}

	public void tick() {
		double prevX = this.getDeltaMovement().x;
		double prevZ = this.getDeltaMovement().z;
		super.tick();
		if (!this.level().isClientSide) {
			// Preserve horizontal, let gravity handle Y
			this.setDeltaMovement(prevX, this.getDeltaMovement().y, prevZ);

			// Particles — emit from the entity position
			ServerLevel serverLevel = (ServerLevel) this.level();
			serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(), this.getZ(), 8, 1.0, 1.0, 1.0,
				0.08);
			serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY(), this.getZ(), 12, 1.2, 1.2,
				1.2, 0.12);
			serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 3, 0.8, 0.8, 0.8,
				0.05);

			if (this.tickCount % 10 == 0) {
				this.level()
					.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS,
						3.0F, 1.2F); // high pitch = urgent
			}

			if (this.getY() <= this.triggerY) {
				this.explode();
			}
		}
	}
	public void explode() {
		if (this.level().isClientSide) return;
		// Eject any riders
		this.ejectPassengers();
		Vec3 current = this.getDeltaMovement();
		// 4 panels flung in cardinal directions + inherit current travel vector
		float[] panelYaws = {0F, 90F, 180F, 270F};
		for (float panelYaw : panelYaws) {
			float yawRad = panelYaw * (float) (Math.PI / 180F);

			VoidDrifterPanel panel = new VoidDrifterPanel(VOID_DRIFTER_PANEL.get(), this.level());
			panel.setPos(this.getX(), this.getY(), this.getZ());

			// Inherit current travel + fling radially outward
			double flingX = current.x + Mth.sin(-yawRad) * 1.5D;
			double flingY = current.y + (this.random.nextFloat() * 0.5D);
			double flingZ = current.z + Mth.cos(yawRad) * 1.5D;
			panel.setDeltaMovement(flingX, flingY, flingZ);

			this.level().addFreshEntity(panel);
		}

		// Spawn BREACH Shell at this position pointing downward
		// YOUR BREACH SHELL ENTITY HERE
		// breachShell.setPos(this.getX(), this.getY(), this.getZ());
		// breachShell.setDeltaMovement(current.x * 0.3, -2.0D, current.z * 0.3);
		// this.level().addFreshEntity(breachShell);

		// Sound
		this.level()
			.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 2.0F,
				0.8F);

		this.discard();
	}
	public void onAddedToWorld() {
		super.onAddedToWorld();
		if (!this.level().isClientSide) {

			BlockHitResult hit = this.level().clip(
				new ClipContext(this.position(), this.position().add(0, -512, 0), ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, this));

			if (hit.getType() != HitResult.Type.MISS) {
				this.triggerY = (float) hit.getLocation().y + EXPLODE_HEIGHT;
			} else {
				this.triggerY = (float) this.getY() - 100F;
			}
		}
	}
	// storage
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("triggerY", this.triggerY);
	}
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.triggerY = tag.getFloat("triggerY");
	}

	// no pushy
	public boolean isPushable() {return false;}
	protected void doPush(Entity pEntity) {}
	public boolean canBeCollidedWith() {return false;}

	public boolean shouldRenderAtSqrDistance(double distanceSq) {return distanceSq < 102400;}
	public Iterable<ItemStack> getArmorSlots() {return Collections.emptyList();}
	public ItemStack getItemBySlot(EquipmentSlot slot) {return ItemStack.EMPTY;}
	public HumanoidArm getMainArm() {return null;}
	public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}
}
