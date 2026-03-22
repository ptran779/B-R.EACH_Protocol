package com.github.ptran779.breach_ptc.entity.structure;

import com.github.ptran779.breach_ptc.config.ServerConfig;
import com.github.ptran779.breach_ptc.item.EngiHammerItem;
import com.github.ptran779.breach_ptc.server.ItemInit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PortDisp extends AbsAgentStruct {
  // animation -- client render only -- send packet to server to update when to play
  public float timeTrigger = 0;
	public static final int OPEN_TIME = 70;
  public static final int STAY_OPEN = 200;

	private static final EntityDataAccessor<Integer> OPENING_TIME = SynchedEntityData.defineId(PortDisp.class,
		EntityDataSerializers.INT);
  public PortDisp(EntityType<? extends Mob> pEntityType, Level pLevel) {
    super(pEntityType, pLevel);
  }

	public int getOpenTime() {
		return entityData.get(OPENING_TIME);
	}
	public void setOpenNowIfPossible() {
		int openTime = tickCount - getOpenTime();
		if (openTime < OPEN_TIME) return;
		else if (openTime < OPEN_TIME + STAY_OPEN) {
			entityData.set(OPENING_TIME, tickCount + OPEN_TIME);
		} else {
			entityData.set(OPENING_TIME, tickCount);
			level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 2.0F
				, 1.2F);
		}
	}

  protected void defineSynchedData(){
    super.defineSynchedData();
		entityData.define(OPENING_TIME, -400);  // 340 should be enough
  }
  public void addAdditionalSaveData(CompoundTag nbt) {super.addAdditionalSaveData(nbt);}
  public void readAdditionalSaveData(CompoundTag nbt) {super.readAdditionalSaveData(nbt);}
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (!level().isClientSide) {
      if (isFriendlyPlayer(player, level())) {
        if (player.getMainHandItem().getItem() instanceof EngiHammerItem) {
					if (player.isShiftKeyDown() && player.isCreative()){
						player.displayClientMessage(Component.literal("Dispenser is fully charged").withStyle(ChatFormatting.GOLD), true);
						this.charge = this.getMaxCharge();
					} else {
						this.spawnAtLocation(new ItemStack(ItemInit.PORT_DISP_ITEM.get()));
						((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 5, 0, 2, 0, 0.02);
						level().playSound(null, blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
						this.discard();
					}
        } else {
	        setOpenNowIfPossible();
          player.displayClientMessage(Component.literal("Dispenser has " + charge + "/" + getMaxCharge() + " charge").withStyle(ChatFormatting.GOLD), true);
        }
      }
    }
    return InteractionResult.SUCCESS;
  }

  public void tick() {
    super.tick();
  }

  public int getMaxCharge(){return ServerConfig.PORT_DIS_CHARGE_MAX.get();}
  public void resetRenderTick() {timeTrigger = tickCount;}
	@Override public int getControlFlg1() {
		return 0;
	}
}
