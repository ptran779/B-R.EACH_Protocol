package com.github.ptran779.breach_ptc.ai.other_goal;

import com.github.ptran779.breach_ptc.entity.extra.VectorPursuer;
import com.github.ptran779.breach_ptc.server.ItemInit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED;
import static net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;

public class DronePursudeGoal extends Goal {
  VectorPursuer vp;
  LivingEntity mainTarget;
  boolean lockOn = false;
	double explodingRQ;

  public DronePursudeGoal(VectorPursuer vp) {
    this.vp = vp;
  }

  public boolean canUse() {
    return vp.getDeployed();
  }
  public void start() {
    if (vp.getTarget() != null && vp.getTarget().isAlive()) {
			mainTarget = vp.getTarget();
			explodingRQ = mainTarget.getBoundingBox().getSize() * mainTarget.getBoundingBox().getSize()+1;  // easier to kill
	    // small thing
		}
  }

  public boolean requiresUpdateEveryTick() {return true;}
  public void tick() {
    if (mainTarget != null && mainTarget.isAlive()) {
      double distSqr = vp.distanceToSqr(mainTarget);
      if (!lockOn && distSqr < 144 && vp.getSensing().hasLineOfSight(mainTarget)) {lockOn = true;}
      vp.getLookControl().setLookAt(mainTarget, 30.0F, 30.0F);
      if (lockOn && vp.tickCount % 4 == 0) {
        Vec3 viewVec = vp.getLookAngle(); // already normalized
        vp.setDeltaMovement(viewVec.scale(0.75)); // adjust speed
        if (distSqr <= explodingRQ) {
          vp.level().explode(vp.deployer, vp.getX(), vp.getY(), vp.getZ(), 4.0f, Level.ExplosionInteraction.NONE);
          vp.discard();
        }
      } else {
        vp.getNavigation().moveTo(mainTarget, vp.getAttributeValue(FLYING_SPEED));
      }
    } else if (vp.deployer != null) {
      if (vp.distanceToSqr(vp.deployer) <= 2) {dropItems();}
      vp.getNavigation().moveTo(vp.deployer, vp.getAttributeValue(MOVEMENT_SPEED));
    } else {dropItems();}
  }
  protected void dropItems() {
    ItemStack drop = new ItemStack(ItemInit.VP_ITEM.get());
    ItemEntity itemEntity = new ItemEntity(vp.level(),vp.getX(),vp.getY(),vp.getZ(),drop);
    vp.level().addFreshEntity(itemEntity);
    vp.discard();
  }
}