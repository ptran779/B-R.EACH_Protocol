package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.ai.brain.AbsAgentBrain;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.config.SkinManager;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventory;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventoryMenu;
import com.github.ptran779.breach_ptc.entity.api.IEntityRender;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeamNTarget;
import com.github.ptran779.breach_ptc.item.BadgeItem;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import com.github.ptran779.breach_ptc.item.ModularShieldItem;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.ptran779.breach_ptc.entity.api.EntityUtils.BF_RAPID_SHOOTING;
import static com.github.ptran779.breach_ptc.server.AttributeInit.WELL_FEED_SPEED_BOOST;
import static com.tacz.guns.api.item.nbt.GunItemDataAccessor.GUN_ID_TAG;

public abstract class AbsAgentEntity extends PathfinderMob implements InventoryCarrier, MenuProvider,
	IEntityTeamNTarget, IEntityRender {
	private boolean persistedFromNBT = false;
	public boolean isShieldActive = false;
	public IGunOperator op;
	protected boolean brainMode = false;  // fixme Need to check for the item, but this is map latter
	protected int lastPatrolIdx = -1;

	/// inventory slot
	public AgentInventory inventory1 = new AgentInventory(16, this);  // so I can handle inventory related stuff cleaner
	public final int[] GEAR_SLOTS = {0, 1, 2, 3};
	public final int GUN_SLOT = 4;
	public final int MELEE_SLOT = 5;
	public final int SPECIAL_SLOT = 6;

	public SimpleContainer inventory2 = new SimpleContainer(2);  // might upgrade to agent inv later
	public final int BRAIN_CHIP_SLOT = 0;
	public final int PATROL_LIST_SLOT = 1;

	// agent custom config
//  private UUID bossUUID = null;
	public UUID followPlayer = null;
	public int maxfood = 40;
	private int pathCooldown = 0;

	/// constructor
	public AbsAgentEntity(EntityType<? extends AbsAgentEntity> entityType, Level level) {
		super(entityType, level);
		((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
		this.getNavigation().setCanFloat(true);
		setPersistenceRequired();  // do not despawn agent
		this.op = IGunOperator.fromLivingEntity(this);  // for gun
		this.setCanPickUpLoot(true);
	}

	/// auto sync variable. Useful for setting flag
	private static final EntityDataAccessor<Integer> CONTROL_FLAG1 =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);

	public static final EntityDataAccessor<Integer> FOOD_VALUE =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VIRTUAL_AMMO =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);  // for render purpose

	private static final EntityDataAccessor<Optional<UUID>> BOSS_UUID =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<ItemStack> BRAIN_CHIP_STACK =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<ItemStack> MELEE_STACK =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<ItemStack> GUN_STACK =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<ItemStack> SPECIAL_STACK =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);

	// Client animation flag
	private static final EntityDataAccessor<Boolean> ANI_MOVE_STATE_CHANGE =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.BOOLEAN);
		// for render purpose when animation has more than 1 option
	private static final EntityDataAccessor<Integer> ANI_MOVE_POSE_START =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANI_MOVE_POSE_END =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> ANI_MOVE_TIME_START =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ANI_MOVE_TIME_END =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ANI_MOVE_TIME_TRAN =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);

	public static final EntityDataAccessor<Boolean> FEMALE =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> SKIN =
		SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.STRING);

	/// skin quick lookup
	private transient ResourceLocation cachedSkin;
	public float renderTimeTrigger = 0;
	public void resetRenderTick() {renderTimeTrigger = tickCount;}
	public void flushSkinCache() {cachedSkin = null;}  // flush for render
	public ResourceLocation getResolvedSkin() {  // cache this for render. so O1 instead of hashmap
		if (cachedSkin == null) {
			cachedSkin = SkinManager.get(getFemale(), getSkin());
		}
		return cachedSkin;
	}

	/// animation helper: setter and getter
	public void setAniMoveTransition(int pStart, int pEnd, float tStart, float tEnd, float tTran) {
		entityData.set(ANI_MOVE_POSE_START, pStart);
		entityData.set(ANI_MOVE_POSE_END, pEnd);
		entityData.set(ANI_MOVE_TIME_START, tStart);
		entityData.set(ANI_MOVE_TIME_END, tEnd);
		entityData.set(ANI_MOVE_TIME_TRAN, tTran);
		entityData.set(ANI_MOVE_STATE_CHANGE, true);
	}
	public void setAniMoveStatic(int pStart) {
		entityData.set(ANI_MOVE_POSE_START, pStart);
		entityData.set(ANI_MOVE_STATE_CHANGE, false);
	}
	public int getAniMovePoseStart() {return entityData.get(ANI_MOVE_POSE_START);}
	public int getAniMovePoseEnd() {return entityData.get(ANI_MOVE_POSE_END);}
	public float getAniMoveTimeStart() {return entityData.get(ANI_MOVE_TIME_START);}
	public float getAniMoveTimeEnd() {return entityData.get(ANI_MOVE_TIME_END);}
	public float getAniMoveTimeTran() {return entityData.get(ANI_MOVE_TIME_TRAN);}
	public boolean getAniMoveStateChange() {return this.entityData.get(ANI_MOVE_STATE_CHANGE);}

	///  mc entity based func
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20)
			.add(Attributes.MOVEMENT_SPEED, 0.5)
			.add(Attributes.JUMP_STRENGTH, 1)
			.add(Attributes.FOLLOW_RANGE, 16)
			.add(Attributes.ATTACK_DAMAGE, 1)
			.add(Attributes.ATTACK_SPEED, 4)
			.add(ForgeMod.ENTITY_REACH.get(), 3);  // as long as player
	}
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(CONTROL_FLAG1, 0);
		entityData.define(FOOD_VALUE, 0);

		entityData.define(ANI_MOVE_STATE_CHANGE, false);
		entityData.define(ANI_MOVE_POSE_START, AnimationID.A_LIVING);
		entityData.define(ANI_MOVE_POSE_END, AnimationID.A_LIVING);
		entityData.define(ANI_MOVE_TIME_START, 0f);
		entityData.define(ANI_MOVE_TIME_END, 0f);
		entityData.define(ANI_MOVE_TIME_TRAN, 0f);

		entityData.define(FEMALE, false);
		entityData.define(SKIN, "");
		entityData.define(BOSS_UUID, Optional.empty());
		entityData.define(VIRTUAL_AMMO, 0);

		entityData.define(BRAIN_CHIP_STACK, ItemStack.EMPTY);
		entityData.define(MELEE_STACK, ItemStack.EMPTY);
		entityData.define(GUN_STACK, ItemStack.EMPTY);
		entityData.define(SPECIAL_STACK, ItemStack.EMPTY);
	}

	public String getAgentType() {return "abstract agent";}
	public String getOwner() {
		UUID bossUUID = getBossUUID();
		if (bossUUID == null) return "";
		Player boss = this.level().getPlayerByUUID(bossUUID);
		if (boss == null) return "";
		return boss.getGameProfile().getName();
	}
	public UUID getBossUUID() {return this.entityData.get(BOSS_UUID).orElse(null);}
	public void setBossUUID(UUID uuid) {
		if (uuid == null) {
			this.entityData.set(BOSS_UUID, Optional.empty());
		} else {
			this.entityData.set(BOSS_UUID, Optional.of(uuid));
		}
	}

	///  combat config
	public int getControlFlg1() {return this.entityData.get(CONTROL_FLAG1);}
	public void setControlFlg1(int dat) {this.entityData.set(CONTROL_FLAG1, dat);}

	public void setFollowEntity(UUID player) {this.followPlayer = player;}
	public int getVirtualAmmo() {return this.entityData.get(VIRTUAL_AMMO);}
	public void setVirtualAmmo(int ammo) {this.entityData.set(VIRTUAL_AMMO, ammo);}

	public Integer getFood() {return this.entityData.get(FOOD_VALUE);}
	public void setFood(Integer val) {this.entityData.set(FOOD_VALUE, val);}
	public boolean getFemale() {return this.entityData.get(FEMALE);}
	public void setFemale(boolean flag) {this.entityData.set(FEMALE, flag);}
	public String getSkin() {return this.entityData.get(SKIN);}
	public void setSkin(String skin) {this.entityData.set(SKIN, skin);}

	///  Patrol Logic
	public int getLastPatrolIdx(){return lastPatrolIdx;}
	public void setLastPatrolIdx(int i) {lastPatrolIdx = i;}
	public ItemStack getPatrolItem(){return inventory2.getItem(PATROL_LIST_SLOT);}

	/// for client cosmetic render
	public ItemStack getChipBrainStack() {return this.entityData.get(BRAIN_CHIP_STACK);}
	public ItemStack getMeleeStack() {return this.entityData.get(MELEE_STACK);}
	public ItemStack getGunStack() {return this.entityData.get(GUN_STACK);}
	public ItemStack getSpecialStack() {return this.entityData.get(SPECIAL_STACK);}

	public void updateBrainChipStack() {entityData.set(BRAIN_CHIP_STACK, inventory2.getItem(BRAIN_CHIP_SLOT));}
	public void updateWeaponStack() {
		entityData.set(MELEE_STACK, inventory1.getItem(MELEE_SLOT));
		entityData.set(GUN_STACK, inventory1.getItem(GUN_SLOT));
		entityData.set(SPECIAL_STACK, inventory1.getItem(SPECIAL_SLOT));
	}

	public abstract AgentConfig getAgentConfig();

	/// Combat
	/// re split gun logic for easier handling and allow rapid firing
	/// move this to some handler later
	FireMode fmode = FireMode.UNKNOWN;
	int tickFireCD = 1000;  // how many tick per shot
	int lastTickShot = 0;
	int reloadTimer = 0;
	boolean bolt = false;

	// ammo consumption USE FOR ACCURATE TRACK OF HOW MUCH AMMO WAS DITCHED OUT
	int ammoUsage = 0;
	int lastSeenInChamber = 0;
	boolean measureUsage = false;

	public void preAmmoUsageCount() {
		measureUsage = true;
		ammoUsage = 0;
		lastSeenInChamber = inventory1.checkAmmoInChamber();
	}
	public int postAmmoUsageCount() {
		measureUsage = false;
		return ammoUsage + Math.max(0, lastSeenInChamber - inventory1.checkAmmoInChamber());  // just in case of a glitch
	}

	public boolean preShoot(boolean aim) {
		ItemStack gunStack = getMainHandItem();
		if (!(gunStack.getItem() instanceof AbstractGunItem gunItem)) {
			return false;
		}
		ResourceLocation gunResource = gunItem.getGunId(gunStack);
		CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
		GunData gundat = gunIndex.getGunData();

		List<FireMode> allFireMode = gundat.getFireModeSet();
		// auto preConfig firing mode for fun.
		if ((getControlFlg1() & BF_RAPID_SHOOTING) == 0) {
			if (allFireMode.contains(FireMode.SEMI)) fmode = FireMode.SEMI;
			else fmode = allFireMode.get(0);  // if this fail, someone else fault
		} else if (allFireMode.contains(FireMode.BURST)) fmode = FireMode.BURST;
		else if (allFireMode.contains(FireMode.AUTO)) fmode = FireMode.AUTO;
		else if (allFireMode.contains(FireMode.SEMI)) fmode = FireMode.SEMI;

		long fireCD = gundat.getShootInterval(this, fmode, gunStack);
		tickFireCD = switch (fmode) {
			case AUTO -> (int) (fireCD / 50 * 4);  //  full auto get 1/4 speed
			case SEMI -> (int) (fireCD / 50 * 16);  //  semi stay at 1/16 speed
			case BURST -> (int) (fireCD / 50 * 12);  //  burst stay at 1/12 speed due to multi shot spray
			case UNKNOWN -> 1000;
		};
		// set firing mode. it just nbt so safe to change instance
		if (gunItem.getFireMode(gunStack) != fmode) {
			gunItem.setFireMode(gunStack, fmode);
		}
		//handle bolt type gun flag,
		bolt = gundat.getBolt() == Bolt.MANUAL_ACTION;

		ShooterDataHolder dat = op.getDataHolder();
		if (dat.reloadStateType.isReloading() || dat.isBolting) return false;
//		if (Utils.hasFriendlyInLineOfFire(this, getTarget())) return false;
		if (op.getDataHolder().currentGunItem == null) {
			op.draw(this::getMainHandItem);
			return false;
		}
		if (op.getSynIsAiming() != aim) {
			op.aim(aim);
			return false;
		}

		return true;
	}
	public void postShoot() {
		op.aim(false);
		if (bolt) op.bolt();
	}

	private boolean canReload() {
		if (getVirtualAmmo() > 0) return true;
		return inventory1.findGunAmmo(getMainHandItem()) != -1;
	}
	public void executeAmmoReloadMath() {
		int reloadAmount = 0;
		ItemStack gunStack = getMainHandItem();
		if (!(gunStack.getItem() instanceof AbstractGunItem gunItem)) return;  // safety check
		ResourceLocation gunResource = gunItem.getGunId(gunStack);
		CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
		int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, gunIndex.getGunData());
		int curAmmoCount = inventory1.checkAmmoInChamber(gunStack, gunItem);

		// if use virtual ammo
		int virtAmmo = getVirtualAmmo();
		if (virtAmmo > 0) {
			reloadAmount = Math.min(virtAmmo, maxAmmoCount - curAmmoCount);
			setVirtualAmmo(virtAmmo - reloadAmount);
		} else {
			// find ammo
			int i = inventory1.findGunAmmo(gunStack);
			if (i == -1) return;
			// compute amount
			ItemStack ammoStack = inventory1.getItem(i);
			if (ammoStack.getItem() instanceof IAmmoBox iAmmoBoxItem) {
				reloadAmount = Math.min(maxAmmoCount - curAmmoCount, iAmmoBoxItem.getAmmoCount(ammoStack));
				iAmmoBoxItem.setAmmoCount(ammoStack, iAmmoBoxItem.getAmmoCount(ammoStack) - reloadAmount);
			} else if (ammoStack.getItem() instanceof IAmmo) {
				reloadAmount = Math.min(maxAmmoCount - curAmmoCount, ammoStack.getCount());
				ammoStack.setCount(ammoStack.getCount() - reloadAmount);
			}
		}
		if (measureUsage) {
			ammoUsage += Math.max(0, lastSeenInChamber - curAmmoCount);
			lastSeenInChamber = curAmmoCount + reloadAmount;  // safety check maynot need but maybe future
		}
		// code use it
		gunItem.setCurrentAmmoCount(gunStack, curAmmoCount + reloadAmount);
	}
	public ShootResult shootGun() {return op.shoot(() -> getViewXRot(1f), () -> getViewYRot(1f));}
	private boolean handleReloadLogic() {
		if (this.reloadTimer <= 0) {
			return false; // Not reloading, proceed to normal shooting logic
		}
		this.reloadTimer--;
		ServerLevel lev = (ServerLevel) level();
		// Timer counts down from 70 to 0.
		if (this.reloadTimer == 69) {  // Old mag drop
			lev.playSound(null, this, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 2f, 0.5f);
		} else if (this.reloadTimer == 50) { // Shove in new mag
			lev.playSound(null, this, SoundEvents.LADDER_HIT, SoundSource.BLOCKS, 2f, 1.8f);
		} else if (this.reloadTimer == 35) { // Lock in new mag
			lev.playSound(null, this, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 2f, 2.0f);
		} else if (this.reloadTimer == 20) { // Prime the gun
			lev.playSound(null, this, SoundEvents.CROSSBOW_LOADING_END, SoundSource.BLOCKS, 2f, 0.8f);
		} else if (this.reloadTimer == 0) { // Ammo in!
			executeAmmoReloadMath();
		}
		return true; // Still reloading, keep the behavior alive
	}
	private boolean handleShootingLogic() {
		int deltaT = tickCount - lastTickShot;
		// 1. Handle gun mechanics (Bolt & Cooldown)
		if (bolt && deltaT == 10) {
			op.bolt();
			return true;
		} else if (deltaT < tickFireCD) {
			return true;
		}
		// 2. Attempt to pull the trigger
		switch (shootGun()) {
			case SUCCESS -> {
				lastTickShot = tickCount;
				return true;
			}
			case NO_AMMO -> {
				if (canReload()) {
					// Initiate the reload sequence
					this.reloadTimer = 70;
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
						new EntityRenderPacket(this.getId(), 1));
					setAniMoveStatic(AnimationID.A_RELOAD);
					return true;
				} else {
					return false; // Gun is empty and inventory has no ammo
				}
			}
			default -> {
				return false; // Something broke (e.g., gun jammed or invalid item)
			}
		}
	}
	public boolean shootingTick() {
		if (handleReloadLogic()) {
			return true;
		}
		return handleShootingLogic();
	}
	public float getGunDmg() {
		ItemStack gunStack = inventory1.getItem(GUN_SLOT);
		if (gunStack.getItem() instanceof ModernKineticGunItem gunItem) {
			ResourceLocation gunResource = gunItem.getGunId(gunStack);
			CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
			return (float) AttachmentDataUtils.getDamageWithAttachment(gunStack, gunIndex.getGunData());
		}
		return 0;
	}

	// switch weapon -- critical: make sure weapon check perform before calling this
	private boolean switching = false;
	public boolean pullWeapon(int type, int tickP) {
		switch (type) {
			case 0: {  // empty
				setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				switching = false;
				return true;
			}
			case 1: {  // melee
				if (getMainHandItem() == inventory1.getItem(MELEE_SLOT) && !switching) return true;
				if (tickP == 0) {
					setAniMoveStatic(AnimationID.A_SWORD_DRAW);
					switching = true;
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
						new EntityRenderPacket(getId(), 1));
				} else if (tickP == 15) {
					level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
						SoundSource.PLAYERS, 1.5F, 1.0F);
					level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_IRON,
						SoundSource.PLAYERS, 1.2F, 1.0F);
					equipMelee();
				} else if (tickP >= 20) {
					setAniMoveStatic(AnimationID.A_LIVING);
					switching = false;
					return true;
				}
				;
				return false;
			}
			case 2: {
				if (getMainHandItem() == inventory1.getItem(GUN_SLOT) && !switching) return true;
				if (tickP == 0) {
					setAniMoveStatic(AnimationID.A_GUN_DRAW);
					switching = true;
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
						new EntityRenderPacket(getId(), 1));
				} else if (tickP == 15) {
					equipGun();
				} else if (tickP >= 40) {
					level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS,
						1.0F, 2.0F);
					level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE,
						SoundSource.PLAYERS, 1.0F, 1.5F);
					setAniMoveStatic(AnimationID.A_LIVING);
					switching = false;
					return true;
				}
				;
				return false;
			}
			default: {
				return true;
			}
		}
	}

	/// brain and AI
	protected void registerGoals() {
	}  // kick all ai off
	protected final Set<UUID> observingPlayer = new HashSet<>();

	// check for brain chip Existence, confirm IO equal, deregister goal, turn on brain mode
	public boolean brainChipValid(){
	ItemStack item = getChipBrainStack();
	if (item.getItem() instanceof BrainChipItem brainChipItem) {
		// brain item tag
		UUID chipTag = brainChipItem.getOrCreateUUID(item);
		// verified Live Brain
		MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(chipTag, level().getGameTime());
		if (mUnit.model == null) {
			return false;
		} else if (mUnit.model.getInsize() != getInputSpace() || mUnit.model.getOutsize() != getOutputSpace()) {
			return false;
		} else {
			printObservation("agent " + getDisplayName() + " turn on brain mode", level().getServer());
			return true;
		}
	}
	return false;
}
	public void reloadBrain(){
		if (brainChipValid()){
			brainMode = true;
			removeFreeWill();  // purge all goal
		} else brainOFF();
	}
	public void brainOFF() {
		brainMode = false;
		if (goalSelector.getAvailableGoals().isEmpty()) getSuperBrain().activateGoalWrapper();
		printObservation("agent " + getDisplayName() + " turn off brain mode", level().getServer());
	}
	public boolean toggleObserver(UUID id) {
		if (observingPlayer.contains(id)) {
			observingPlayer.remove(id);
			return false; // Turned OFF
		} else {
			observingPlayer.add(id);
			return true; // Turned ON
		}
	}
	public void printObservation(String msg, MinecraftServer server){
		if (observingPlayer.isEmpty()) return;
		Component msgCom = Component.literal(msg);
		for (UUID id : observingPlayer) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {observingPlayer.remove(id);continue;}
			player.sendSystemMessage(msgCom);
		}
	}
	public abstract AbsAgentBrain getSuperBrain();

	/// inventory & menu
	public SimpleContainer getInventory() {return inventory1;}
	public boolean wantsToPickUp(ItemStack pStack) {return false;}
	protected void pickUpItem(@NotNull ItemEntity itemEntity) {
		ItemStack input = itemEntity.getItem();
		if (input.isEmpty()) return;
		// --- Armor ---
		if (input.getItem() instanceof ArmorItem armor) {
			EquipmentSlot slot = armor.getEquipmentSlot();
			int invSlot = switch (slot) {
				case HEAD -> GEAR_SLOTS[0];
				case CHEST -> GEAR_SLOTS[1];
				case LEGS -> GEAR_SLOTS[2];
				case FEET -> GEAR_SLOTS[3];
				default -> -1;
			};
			if (invSlot != -1 && inventory1.getItem(invSlot).isEmpty()) {
				inventory1.setItem(invSlot, input);
				setItemSlot(slot, input); // visually equip
				itemEntity.discard();
				return;
			}
		}
		// --- Gun ---
		if (isEquipableGun(input) && inventory1.getItem(GUN_SLOT).isEmpty()) {
			inventory1.setItem(GUN_SLOT, input);
			itemEntity.discard();
			return;
		}
		// --- Melee ---
		if (isEquipableMelee(input) && inventory1.getItem(MELEE_SLOT).isEmpty()) {
			inventory1.setItem(MELEE_SLOT, input);
			itemEntity.discard();
			return;
		}
		// --- Special Slot ---
		ItemStack special = inventory1.getItem(SPECIAL_SLOT);
		if (special.isEmpty()) {
			inventory1.setItem(SPECIAL_SLOT, input);
			itemEntity.discard();
			return;
		} else if (ItemStack.isSameItemSameTags(special, input) && special.getCount() < special.getMaxStackSize()) {
			int move = Math.min(input.getCount(), special.getMaxStackSize() - special.getCount());
			special.grow(move);
			input.shrink(move);
			if (input.isEmpty()) {
				itemEntity.discard();
				return;
			}
		}
		// --- Backpack (slot 7–16) ---
		for (int i = 7; i < inventory1.getContainerSize(); i++) {
			ItemStack slot = inventory1.getItem(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, input) && slot.getCount() < slot.getMaxStackSize()) {
				int move = Math.min(input.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(move);
				input.shrink(move);
				if (input.isEmpty()) {
					itemEntity.discard();
					return;
				}
			}
		}
		for (int i = 7; i < inventory1.getContainerSize(); i++) {
			if (inventory1.getItem(i).isEmpty()) {
				inventory1.setItem(i, input);
				itemEntity.discard();
				return;
			}
		}
		// ❌ Couldn’t insert fully
		itemEntity.setItem(input);
	}
	public AbstractContainerMenu createMenu(int containerID, Inventory inventory, Player player) {
		return new AgentInventoryMenu(containerID, inventory, this);
	}

	/// interaction
	// check to make sure same owner, or owner in same team,
	public boolean sameTeam(LivingEntity entity) {  // fixme move this to interface dealing?
		if (entity instanceof Player player) {
			return isFriendlyPlayer(player, level());
		} else if (entity instanceof IEntityTeamNTarget teamer) {
			return isFriendlyMod(teamer, level());
		}
		return false;
	}
	public boolean hurt(DamageSource source, float amount) {
		if (isShieldActive) {
			level().playSound(null, this, SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 1.0f, 0.5f);
			if (source.getSourcePosition() != null && Utils.isInFrontArc(this, source.getSourcePosition(), 180)) {
				amount = 0;  // cancel all dmg
			} else {
				amount *= 0.5f;  // 50% reduction from flanking
			}
			// dirty check both hands for shield item, prefer offhand
			ItemStack shield = getOffhandItem().getItem() instanceof ModularShieldItem ? getOffhandItem()
				: getMainHandItem().getItem() instanceof ModularShieldItem ? getMainHandItem()
				: ItemStack.EMPTY;

			if (!shield.isEmpty()) {
				shield.hurtAndBreak(1, this, e -> e.broadcastBreakEvent(InteractionHand.OFF_HAND));
			}
		}
		getSuperBrain().forceAwake();
		return super.hurt(source, amount);
	}
	protected void hurtArmor(DamageSource pSource, float pDamage) {
		if (!(pDamage <= 0.0F)) {
			pDamage /= 4.0F;
			if (pDamage < 1.0F) {
				pDamage = 1.0F;
			}
			AtomicBoolean brokenArmorPiece = new AtomicBoolean(false);
			for (ItemStack piece : this.getArmorSlots()) {
				if ((!pSource.is(DamageTypeTags.IS_FIRE) || !piece.getItem()
					.isFireResistant()) && piece.getItem() instanceof ArmorItem) {
					piece.hurtAndBreak((int) pDamage, this, (e) -> {
						e.broadcastBreakEvent(LivingEntity.getEquipmentSlotForItem(piece));
						brokenArmorPiece.set(true);
					});
				}
			}
			if (brokenArmorPiece.get()) {
				inventory1.loadArmor();
				brokenArmorPiece.set(false);
			}
		}
	}
	///  CRITICAL EXPERIMENTAL
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack item = player.getItemInHand(hand);

		// Leash — boss only
		if (item.is(Items.LEAD) && sameTeam(player) && this.canBeLeashed(player)) {
			if (!this.level().isClientSide) this.setLeashedTo(player, true);
			return InteractionResult.SUCCESS;
		}
		// badge
		if (item.getItem() instanceof BadgeItem) {return InteractionResult.PASS;}

		if (!this.level().isClientSide) {
			if (getBossUUID() == null) setBossUUID(player.getUUID());
			if (sameTeam(player)) {
				NetworkHooks.openScreen((ServerPlayer) player, this, buf -> buf.writeInt(this.getId()));
			}
		}

		return InteractionResult.SUCCESS;
	}

	/// save and load
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		// save inventory 1
		if (!inventory1.isEmpty()) {
			ListTag invTag = new ListTag();
			for (int i = 0; i < inventory1.getContainerSize(); i++) {
				ItemStack stack = inventory1.getItem(i);
				if (!stack.isEmpty()) {
					CompoundTag itemTag = new CompoundTag();
					itemTag.putByte("Slot", (byte) i);
					stack.save(itemTag);
					invTag.add(itemTag);
				}
			}
			nbt.put("AgentInventory", invTag); // save entire inventory as one list
		}
		// Save Inv 2
		if (!inventory2.isEmpty()) {
			ListTag invTag = new ListTag();
			for (int i = 0; i < inventory2.getContainerSize(); i++) {
				ItemStack stack = inventory2.getItem(i);
				if (!stack.isEmpty()) {
					CompoundTag itemTag = new CompoundTag();
					itemTag.putByte("Slot", (byte) i);
					stack.save(itemTag);
					invTag.add(itemTag);
				}
			}
			nbt.put("AgentInventoryExtra", invTag); // save entire inventory as one list
		}
		// save other data
		nbt.putInt("Food", this.getFood());
		if (getBossUUID() != null) {
			nbt.putUUID("owner_uuid", getBossUUID());
		}
		nbt.putInt("control_flag1", getControlFlg1());

		nbt.putBoolean("is_female", getFemale());
		nbt.putString("skin", getSkin());

		nbt.putInt("virtual_ammo", this.getVirtualAmmo());
		nbt.putBoolean("brain_mode", this.brainMode);
		nbt.putInt("last_patrol_idx", this.lastPatrolIdx);

		getSuperBrain().diskWrite(nbt);
	}
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		this.persistedFromNBT = true;
		// Load inventory1
		if (nbt.contains("AgentInventory", 9)) {
			ListTag invTag = nbt.getList("AgentInventory", Tag.TAG_COMPOUND);
			for (int i = 0; i < invTag.size(); i++) {
				CompoundTag itemTag = invTag.getCompound(i);
				int slot = itemTag.getByte("Slot") & 255;
				if (slot < this.inventory1.getContainerSize()) {
					this.inventory1.setItem(slot, ItemStack.of(itemTag));
				}
			}
		}
		// Load Inv 2
		if (nbt.contains("AgentInventoryExtra", 9)) {
			ListTag extraTag = nbt.getList("AgentInventoryExtra", 10);
			for (int i = 0; i < extraTag.size(); i++) {
				CompoundTag itemTag = extraTag.getCompound(i);
				int slot = itemTag.getByte("Slot") & 255;
				if (slot < this.inventory2.getContainerSize()) {
					this.inventory2.setItem(slot, ItemStack.of(itemTag));
				}
			}
		}

		// load other data
		setFood(nbt.getInt("Food"));
		if (nbt.contains("owner_uuid")) {
			setBossUUID(nbt.getUUID("owner_uuid"));
		} else {
			setBossUUID(null);
		}

		setControlFlg1(nbt.getInt("control_flag1"));
		setFemale(nbt.getBoolean("is_female"));
		setSkin(nbt.getString("skin"));

		this.setFollowEntity(this.getBossUUID());
		this.setVirtualAmmo(nbt.getInt("virtual_ammo"));
		this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.brainMode = nbt.getBoolean("brain_mode");
		this.lastPatrolIdx = nbt.getInt("last_patrol_idx");

		//cosmetic render
		updateBrainChipStack();
		updateWeaponStack();

		if(!brainMode) {getSuperBrain().activateGoalWrapper();}
		getSuperBrain().diskRead(nbt);
	}

	/// cosmetic and spawn
	public void initCosmetic() {
		boolean isFemale = ThreadLocalRandom.current().nextBoolean();
		this.setCustomName(Component.literal(Utils.randomName(isFemale)));
		setFemale(isFemale);
		setSkin(isFemale ? getAgentConfig().defaultFemaleSkin : getAgentConfig().defaultMaleSkin);
	}
	public void setCosmetic(boolean female, String skin) {
		setFemale(female);
		setSkin(skin);
		this.setCustomName(Component.literal(Utils.randomName(female)));  // still need a name
	}

	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
	                                    @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
		getSuperBrain().activateGoalWrapper();

		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
		this.setLeftHanded(false);  // everyone use right hand pls
		if (!persistedFromNBT) {
			boolean isFemale = ThreadLocalRandom.current().nextBoolean();
			this.setCustomName(Component.literal(Utils.randomName(isFemale)));
			setFemale(isFemale);
			setSkin(isFemale ? getAgentConfig().defaultFemaleSkin : getAgentConfig().defaultMaleSkin);
		}
		return data;
	}
	public void die(DamageSource pSource) {
		super.die(pSource);
		if (!level().isClientSide()){
			// drop inv
			for (int i = 0; i < inventory1.getContainerSize(); i++) {
				ItemStack stack = inventory1.getItem(i);
				if (!stack.isEmpty()) {
					this.spawnAtLocation(stack);
				}
			}
			for (int i = 0; i < inventory2.getContainerSize(); i++) {
				ItemStack stack = inventory2.getItem(i);
				if (!stack.isEmpty()) {
					this.spawnAtLocation(stack);
				}
			}
			// message boss
			if (getBossUUID() != null) {
				ServerPlayer boss = ((ServerLevel) this.level()).getServer()
					.getPlayerList().getPlayer(getBossUUID());
				if (boss != null) {
					boss.sendSystemMessage(Component.literal(
						"[" + this.getDisplayName().getString() + "] RIP — "
							+ pSource.getMsgId() + " at "
							+ this.blockPosition().getX() + ", "
							+ this.blockPosition().getY() + ", "
							+ this.blockPosition().getZ()
					));
				}
			}
		}
	}

	/// ticking
	private void passiveRegen() {
		this.setFood(this.getFood() - 1);
		this.heal(1);
	}
	private void scanAndPickupItems() {  // some dumb ass optimization mod disables our pickup capability. screw them we'll tick ourselves
		AABB searchBox = this.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
		List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(ItemEntity.class, searchBox);

		for (ItemEntity itemEntity : nearbyItems) {
			// Skip dead items, empty items, or items a player just threw (pickup delay)
			if (itemEntity.isRemoved() || itemEntity.getItem().isEmpty() || itemEntity.hasPickUpDelay()) {continue;}
			pickUpItem(itemEntity);
		}
	}
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (tickCount % 40 == 0) {
			scanAndPickupItems();
		}
		if (tickCount % 80 == 0) {
			var attr = getAttribute(Attributes.MOVEMENT_SPEED);
			if (getFood() >= maxfood * 0.4) {  // maybe register as an effect ?
				if (attr != null && !attr.hasModifier(WELL_FEED_SPEED_BOOST)) {
					attr.addTransientModifier(WELL_FEED_SPEED_BOOST);
				}
			} else {
				// Remove if no longer well-fed
				if (attr != null && attr.hasModifier(WELL_FEED_SPEED_BOOST)) {
					attr.removeModifier(WELL_FEED_SPEED_BOOST);
				}
			}
			if (getHealth() < getMaxHealth() && getFood() >= maxfood * 0.25) {
				passiveRegen();
			}
		}
		if (brainMode) getSuperBrain().tick();
	}

	/// navigation
	public boolean moveto(Entity pEntity, double pSpeed) {
		if (tickCount - pathCooldown > 10) {
			pathCooldown = tickCount;
			return this.getNavigation().moveTo(pEntity, pSpeed);
		}
		return true;
	}
	public boolean moveto(Vec3 target, double pSpeed) {
		if (tickCount - pathCooldown > 10) {
			pathCooldown = tickCount;
			return this.getNavigation().moveTo(target.x, target.y, target.z, pSpeed);
		}
		return true;
	}
	public void stopNav() {
		this.getNavigation().stop();
		this.pathCooldown = 0;
	}

	///  weapon system
	//just stack to mainhand, perfrom check since it call draw, which take entity a few tick to process drawing
	public void equipGun() {
		if (getMainHandItem() != inventory1.getItem(GUN_SLOT)) {
			setItemInHand(InteractionHand.MAIN_HAND, inventory1.getItem(GUN_SLOT));
			IGunOperator op = IGunOperator.fromLivingEntity(this);
			op.draw(this::getMainHandItem);
		}
	}
	// just stack to mainhand
	public void equipMelee() {setItemInHand(InteractionHand.MAIN_HAND, inventory1.getItem(MELEE_SLOT));}
	// special
	public void equipSpecial(boolean offhand) {
		setItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, inventory1.getItem(SPECIAL_SLOT));
	}
	public ItemStack getSpecialSlot() {return inventory1.getItem(SPECIAL_SLOT);}
	// inv
	public boolean isEquipableGun(ItemStack stack) {
		CompoundTag nbt = stack.getOrCreateTag();
		String gunId = nbt.getString(GUN_ID_TAG);
		if (gunId.isEmpty()) return false;
		return getAgentConfig().allowGuns.contains(gunId);
	}
	public boolean isEquipableMelee(ItemStack stack) {
		if (stack.isEmpty()) return false;
		return (getAgentConfig().allowMelees.contains(
			BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) || stack.getTags()
			.anyMatch(tagKey -> getAgentConfig().allowMelees.contains("#" + tagKey.location())));
	}

	// for overwrite later in final class
	public int getMaxVirtualAmmo() {return getAgentConfig().maxVirtualAmmo;}
	public int getAmmoPerCharge() {return getAgentConfig().chargePerAmmo;}
	public String getCSVSensorsHeader() {return "";}
	public abstract int getInputSpace();
	public abstract int getOutputSpace();
	public abstract int getCustSpace();
}
