package com.github.ptran779.breach_ptc.entity.structure;

import com.github.ptran779.breach_ptc.ai.api.GoalWrapper;
import com.github.ptran779.breach_ptc.ai.api.Sensor;
import com.github.ptran779.breach_ptc.ai.other_goal.DBTurretAttackGoal;
import com.github.ptran779.breach_ptc.ai.other_goal.getConditionalAttackable;
import com.github.ptran779.breach_ptc.config.ServerConfig;
import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeamNTarget;
import com.github.ptran779.breach_ptc.entity.extra.TurretBullet;
import com.github.ptran779.breach_ptc.item.EngiHammerItem;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.server.EntityInit;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

//FIXME need to reoptimized, fix a few thing here and there... later issue
import java.util.ArrayList;
import java.util.List;

import static com.github.ptran779.breach_ptc.config.ServerConfig.BD_TURRET_DPS;

public class DBTurret extends AbsAgentStruct implements IEntityTeamNTarget {
  public static final int T_OFFSET = -40;
	public static final double OPERATING_RANGE = 24;

	private int controlFlag = EntityUtils.BF_TARGET_HOSTILE;  // default on

  // sync dat
  public static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(DBTurret.class, EntityDataSerializers.BOOLEAN);
  public static final EntityDataAccessor<Boolean> LEFT_BARREL = SynchedEntityData.defineId(DBTurret.class, EntityDataSerializers.BOOLEAN);

  // animation -- client render only
  public float cannonProgress = Float.MAX_VALUE;

  public DBTurret(EntityType<? extends Mob> pEntityType, Level pLevel) {
    super(pEntityType, pLevel);
  }

  public int getMaxCharge(){return ServerConfig.BD_TURRET_CHARGE_MAX.get();}

	public int getControlFlg1() {
		return controlFlag;
	}
  protected void defineSynchedData(){
    super.defineSynchedData();
    entityData.define(DEPLOYED, false);
    entityData.define(LEFT_BARREL, false);
  }
  public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);
    nbt.putBoolean("deployed", entityData.get(DEPLOYED));
		nbt.putInt("control_flg1", getControlFlg1());
  }
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);
    entityData.set(DEPLOYED, nbt.getBoolean("deployed"));
		controlFlag = nbt.getInt("control_flg1");
  }

  protected void registerGoals() {
		//tiny sensor
	  Sensor<List<LivingEntity>> hostileLongRS = new Sensor<>(() -> {
		  List<LivingEntity> out = new ArrayList<>();
		  for (LivingEntity entity : Utils.getAllLivingInRange(this, OPERATING_RANGE)) {
			  if (isPotentialHostile(this, entity)) out.add(entity);
		  }
		  return out;
	  }, 300);

	  Sensor<LivingEntity> nearestTargetable = new Sensor<>(() -> {
		  LivingEntity target = null;
			double shortestRq = Double.MAX_VALUE;
			for (LivingEntity targetAble : hostileLongRS.get(this.tickCount)){
				if (targetAble != null && targetAble.isAlive() && shouldTargetEntity(this, targetAble)) {
					double distRq = this.distanceTo(targetAble);
					if (distRq < shortestRq && !Utils.rayCastHit(this.getEyePosition(), targetAble.getEyePosition(),
						(ServerLevel) level())){
						target = targetAble;
						shortestRq = distRq;
					}
				}
			}
		  return this.getTarget() == target ? null : target;
	  }, 40);  // match with hostile update speed
	  Sensor<LivingEntity> retarHostileS = new Sensor<>(() -> {
		  LivingEntity target = getLastHurtByMob();
		  if (target == null || !target.isAlive() || target == getTarget() || isAlly(target) ||
			  tickCount - getLastHurtByMobTimestamp() > 600 || Utils.rayCastHit(getEyePosition(),
			  target.getEyePosition(), (ServerLevel) level())) return null;  // only bother with 30s enemy retar
		  return target;
	  }, 20);

		Sensor<Boolean> hasLOStoCurrentTarget = new Sensor<>(()->{
			LivingEntity target = getTarget();
			return target != null && !Utils.rayCastHit(getEyePosition(), target.getEyePosition(), (ServerLevel) level());
		}, 5);

	  this.goalSelector.addGoal(3, new DBTurretAttackGoal(this, 28, 10, hasLOStoCurrentTarget));
	  this.goalSelector.addGoal(4, new GoalWrapper(new getConditionalAttackable(this, 40, 0, 24, retarHostileS, hasLOStoCurrentTarget), false));
	  this.goalSelector.addGoal(5, new GoalWrapper(new getConditionalAttackable(this, 40, 0, 24, nearestTargetable, hasLOStoCurrentTarget), false));
  }

  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (!level().isClientSide) {
      if (sameTeam(player)) {
        if(!entityData.get(DEPLOYED)) {
          player.displayClientMessage(Component.literal("Turret will be ready soon").withStyle(ChatFormatting.GOLD), true);
        } else if (player.getMainHandItem().getItem() instanceof SwordItem) {
					boolean s1 = (controlFlag & EntityUtils.BF_TARGET_HOSTILE) != 0;
					boolean s2 = (controlFlag & EntityUtils.BF_TARGET_AGENT) != 0;
          String disp;
					if (!s1 && !s2) {
						controlFlag |= EntityUtils.BF_TARGET_HOSTILE;
						disp = "Turret will aim at hostile";
					} else if (s1 && !s2) {
						controlFlag &= ~EntityUtils.BF_TARGET_HOSTILE;
						controlFlag |= EntityUtils.BF_TARGET_AGENT;
						disp = "Turret will aim at other humanoid";
					} else if (!s1 && s2) {
						controlFlag |= EntityUtils.BF_TARGET_HOSTILE;
						controlFlag |= EntityUtils.BF_TARGET_AGENT;
						disp = "Turret will aim at all";;
					} else {
						controlFlag &= ~EntityUtils.BF_TARGET_HOSTILE;
						controlFlag &= ~EntityUtils.BF_TARGET_AGENT;
						disp = "Turret will idle";
					}
          player.displayClientMessage(Component.literal(disp), true);
        } else if (player.getMainHandItem().getItem() instanceof EngiHammerItem){
          this.spawnAtLocation(new ItemStack(ItemInit.DB_TURRET_ITEM.get()));
          ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 5, 0, 2, 0, 0.02);
          level().playSound(null, blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
          this.discard();
        } else {
          player.displayClientMessage(Component.literal("Turret has " + charge + "/" + getMaxCharge() + " charge").withStyle(ChatFormatting.GOLD), true);
        }
      }
    }
    return InteractionResult.SUCCESS;
  }

  public void tick() {
    super.tick();
    if (!level().isClientSide()){
      // process deploy system
      if (!entityData.get(DEPLOYED)) {
        int off_tick = tickCount + T_OFFSET;
        ServerLevel level = (ServerLevel) level();
        if (off_tick == 20){
          level.playSound(null, this, SoundEvents.ELDER_GUARDIAN_DEATH, SoundSource.BLOCKS, 1.2f, 1.0f);
          level.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 20, 0, 1, 0, 0.02);
        } else if (off_tick == 100){
          level.playSound(null, this, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.2f, 1.0f);
        } else if (off_tick == 180 || off_tick == 190){
          level.playSound(null, this, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.2f, 1.0f);
          level.sendParticles(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 20, 0, 2, 0, 0.02);
        } else if (off_tick >= 200) {
          entityData.set(DEPLOYED, true);
        }
      }
    }
  }

  public void shoot(){
    if (!entityData.get(DEPLOYED)) return; // safety
    // check ammo count
    if (charge <= 0) return;
    charge--;
    if (Utils.hasFriendlyInLineOfFire(this, this.getTarget(), -1)) return;  // no firing with friend on the line

    // pick the barrel
    boolean gunBarrel = !this.entityData.get(LEFT_BARREL);
    this.entityData.set(LEFT_BARREL, gunBarrel);

    // shot
    double offset = gunBarrel? 0.35:-0.35;
    // Get turret facing
    float pitch = getXRot(); // up/down
    float yaw = getYHeadRot();   // left/right
    TurretBullet bullet = new TurretBullet(EntityInit.TURRET_BULLET.get(), level());
    bullet.init(BD_TURRET_DPS.get());
    bullet.setPos(getX()+ Mth.cos(yaw)*offset, getEyeY()-0.02, getZ() + Mth.sin(yaw)*offset); // or muzzle pos
    Vec3 look = Vec3.directionFromRotation(pitch, yaw); // normalized
    double speed = 2; // blocks per tick
    bullet.setDeltaMovement(look.scale(speed));
    bullet.setOwner(this); // if needed for team logic
    level().addFreshEntity(bullet);
    // sound visual and send packet to server to trigger client render barrel
    level().playSound(null, this, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.6f, 1.2f);
    PacketHandler.CHANNELS.send(
        PacketDistributor.TRACKING_ENTITY.with(() -> this),
        new EntityRenderPacket(this.getId(), 1) // or any float to signal timing
    );
  }

  @Override
  public void resetRenderTick() {cannonProgress = tickCount;}
}
