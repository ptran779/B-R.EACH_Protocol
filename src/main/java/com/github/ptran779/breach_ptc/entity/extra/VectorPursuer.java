package com.github.ptran779.breach_ptc.entity.extra;

import com.github.ptran779.breach_ptc.ai.other_goal.DronePursudeGoal;
import com.github.ptran779.breach_ptc.entity.api.IEntityRender;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeamNTarget;
import com.github.ptran779.breach_ptc.entity.structure.DBTurret;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.UUID;

public class VectorPursuer extends PathfinderMob implements IEntityRender, IEntityTeamNTarget {
	public UUID deployerUUID = null;
	public UUID bossUUID = null;
	public int timeTrigger = 0;

	// 1. Fixed the copy-paste class target for DataTracker
	private static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(VectorPursuer.class, EntityDataSerializers.BOOLEAN);

	public VectorPursuer(EntityType<? extends VectorPursuer> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);

		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
	}

	protected void registerGoals(){
		this.goalSelector.addGoal(0, new DronePursudeGoal(this));
	}

	protected PathNavigation createNavigation(Level level) {
		// Keeps the 3D A* pathfinding
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(false);
		nav.setCanFloat(true);
		nav.setCanPassDoors(true);
		return nav;
	}
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createLivingAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)  // use this for homing return
			.add(Attributes.FLYING_SPEED, 0.32)
			.add(Attributes.ARMOR, 6)
			.add(Attributes.FOLLOW_RANGE, 32.0D);
	}

	public void travel(Vec3 travelVector) {
		if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
			if (this.isInWater() || this.isInLava()) {
				this.moveRelative(0.02F, travelVector);
				this.move(MoverType.SELF, this.getDeltaMovement());
				this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
			} else {
				float actualThrust = this.getSpeed() * 3.0F;

				this.moveRelative(actualThrust, travelVector);
				this.move(MoverType.SELF, this.getDeltaMovement());
				this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
			}
		}
	}

	public boolean getDeployed() { return entityData.get(DEPLOYED); }
	public void setDeployed(boolean flag) { entityData.set(DEPLOYED, flag); }

	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DEPLOYED, false);
	}

	// 2. Fixed NBT Serialization bugs
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("Deployed", this.getDeployed());
		if (deployerUUID != null) nbt.putUUID("deployerUUID", deployerUUID);
		if (bossUUID != null) nbt.putUUID("bossUUID", bossUUID);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("Deployed")) this.setDeployed(nbt.getBoolean("Deployed"));
		if (nbt.contains("deployerUUID")) deployerUUID = nbt.getUUID("deployerUUID");
		if (nbt.contains("bossUUID")) bossUUID = nbt.getUUID("bossUUID");
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			if (!getDeployed()) {
				if (tickCount == 1) {
					setDeltaMovement(0, 0.5, 0);
				} else if (tickCount == 10) {
					setDeployed(true);
					setDeltaMovement(0, 0, 0);
				}
			}
		}
	}

	public Iterable<ItemStack> getArmorSlots() { return Collections.emptyList(); }
	public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
	public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
	public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}

	public void resetRenderTick() { timeTrigger = tickCount; }
	public UUID getBossUUID() { return bossUUID; }
	public int getControlFlg1() { return 0; }
}

//public class VectorPursuer extends FlyingMob implements IEntityRender, IEntityTeamNTarget {
//  public UUID deployerUUID = null;
//  public UUID bossUUID = null;
//  public int timeTrigger = 0;
//  public VectorPursuer(EntityType<? extends VectorPursuer> pEntityType, Level pLevel) {
//    super(pEntityType, pLevel);
//    this.moveControl = new FlyingMoveControl(this, 20, true);
//  }
//
//  protected SoundEvent getAmbientSound() {
//    return AMETHYST_BLOCK_CHIME; // or vanilla SoundEvents
//  }
//
//  protected PathNavigation createNavigation(Level level) {
//    return new FlyingPathNavigation(this, level);
//  }
//
//  private static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(DBTurret.class, EntityDataSerializers.BOOLEAN);
//  public boolean getDeployed() {return entityData.get(DEPLOYED);}
//  public void setDeployed(boolean flag) {entityData.set(DEPLOYED, flag);}
//
//  protected void defineSynchedData() {
//    super.defineSynchedData();
//    entityData.define(DEPLOYED, false);
//  }
//  public void addAdditionalSaveData(CompoundTag nbt){
//    super.addAdditionalSaveData(nbt);
//    nbt.putBoolean("Deployed", this.getDeployed());
//    if (deployerUUID != null) {nbt.putUUID("deployerUUID", deployerUUID);}
//    if (bossUUID != null) {nbt.putUUID("bossUUID", bossUUID);}
//  }
//  public void readAdditionalSaveData(CompoundTag nbt){
//    super.readAdditionalSaveData(nbt);
//    if (nbt.contains("Deployed")) {deployerUUID = nbt.getUUID("deployerUUID");}
//    if (nbt.contains("BossUUID")) {bossUUID = nbt.getUUID("bossUUID");}
//  }
//  public static AttributeSupplier.Builder createAttributes() {
//    return Mob.createLivingAttributes()
//        .add(Attributes.MAX_HEALTH, 20.0)
//        .add(Attributes.MOVEMENT_SPEED, 1)
//        .add(Attributes.FLYING_SPEED, 1)
//        .add(Attributes.ARMOR, 6)
//        .add(Attributes.FOLLOW_RANGE, 32.0D);
//  }
//
//  public void tick() {
//    super.tick();
//    if (!level().isClientSide()){
//      if (!getDeployed()) {
//        if (tickCount == 1) {setDeltaMovement(0, 0.5, 0);}
//        else if (tickCount == 10) {
//          setNoGravity(true);
//          setDeployed(true);
//          setDeltaMovement(0, 0, 0);
//        }
//      }
//    }
//  }
//  public Iterable<ItemStack> getArmorSlots() {return Collections.emptyList();}
//  public ItemStack getItemBySlot(EquipmentSlot slot) {return ItemStack.EMPTY;}
//  public HumanoidArm getMainArm() {return null;}
//  public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}
//  public void resetRenderTick() {timeTrigger = tickCount;}
//
//  public UUID getBossUUID() {return bossUUID;}
//	public int getControlFlg1() {return 0;}
//}
