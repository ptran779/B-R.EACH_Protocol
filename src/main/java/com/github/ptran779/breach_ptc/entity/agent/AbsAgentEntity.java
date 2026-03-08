package com.github.ptran779.breach_ptc.entity.agent;

import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.config.SkinManager;
import com.github.ptran779.breach_ptc.config.AgentConfig;
import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventory;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventoryMenu;
import com.github.ptran779.breach_ptc.entity.api.IEntityRender;
import com.github.ptran779.breach_ptc.entity.api.IEntityTarget;
import com.github.ptran779.breach_ptc.entity.api.IEntityTeam;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
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
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.ptran779.breach_ptc.attribute.AgentAttribute.*;
import static com.tacz.guns.api.item.nbt.GunItemDataAccessor.GUN_ID_TAG;

public abstract class AbsAgentEntity extends PathfinderMob implements InventoryCarrier, MenuProvider, IEntityTeam, IEntityTarget, IEntityRender {
  private boolean persistedFromNBT = false;
  public boolean invincible = false;
  public IGunOperator op;
	protected boolean brainMode = false;  // fixme Need to check for the item, but this is map latter

  //inventory slot
  public AgentInventory inventory1 = new AgentInventory(16, this);  // so I can handle inventory related stuff cleaner
  public final int[] GEAR_SLOTS = {0,1,2,3};
  public final int GUN_SLOT = 4;
  public final int MELEE_SLOT = 5;
  public final int SPECIAL_SLOT = 6;

  public SimpleContainer inventory2 = new SimpleContainer(1);  // might upgrade to agent inv later

  // agent custom config
//  private UUID bossUUID = null;
  public UUID followPlayer = null;
  public int maxfood = 40;
  private int pathCooldown = 0;

  /// auto sync variable. Useful for setting flag
	// Control flag list: 1 wander, 2 follow, 3 target hostile, 4 target agent / player, 5 allow special, 6 rapid shooting
	private static final EntityDataAccessor<Integer> CONTROL_FLAG1 = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);

  public static final EntityDataAccessor<Integer> FOOD_VALUE = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> VIRTUAL_AMMO = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);  // for render purpose

  private static final EntityDataAccessor<Optional<UUID>> BOSS_UUID = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<ItemStack> BRAIN_CHIP_STACK = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<ItemStack> MELEE_STACK = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<ItemStack> GUN_STACK = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.ITEM_STACK);;

  // Client animation flag
  private static final EntityDataAccessor<Boolean> ANI_MOVE_STATE_CHANGE = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.BOOLEAN);  // for render purpose when animation has more than 1 option
  private static final EntityDataAccessor<Integer> ANI_MOVE_POSE_START = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ANI_MOVE_POSE_END = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Float> ANI_MOVE_TIME_START = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> ANI_MOVE_TIME_END = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> ANI_MOVE_TIME_TRAN = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.FLOAT);

  public static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.BOOLEAN);
  public static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(AbsAgentEntity.class, EntityDataSerializers.STRING);

	/// skin quick lookup
  private transient ResourceLocation cachedSkin;
  public float renderTimeTrigger = 0;
  public void resetRenderTick() {renderTimeTrigger = tickCount;}
  public void flushSkinCache() {cachedSkin = null;}  // flush for render
  public ResourceLocation getResolvedSkin() {  // cache this for render. so O1 instead of hashmap
    if (cachedSkin == null) {cachedSkin = SkinManager.get(getFemale(), getSkin());}
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

  public AbsAgentEntity(EntityType<? extends AbsAgentEntity> entityType, Level level) {
    super(entityType, level);
    ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
    this.getNavigation().setCanFloat(true);
    setPersistenceRequired();  // do not despawn agent
    this.op = IGunOperator.fromLivingEntity(this);  // for gun
    this.setCanPickUpLoot(true);
  }

	///  mc entity based func
  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20)
        .add(Attributes.MOVEMENT_SPEED, 0.5)
        .add(Attributes.JUMP_STRENGTH, 1)
        .add(Attributes.FOLLOW_RANGE, 16)
        .add(Attributes.ATTACK_DAMAGE, 1)
        .add(AGENT_ATTACK_SPEED, 1);
  }
  protected void defineSynchedData(){
    super.defineSynchedData();
	  entityData.define(CONTROL_FLAG1,0);
//    entityData.define(KEEP_EAT_F, false);
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
  }

	public String getAgentType(){return "abstract agent";};
  public String getOwner() {
    UUID bossUUID = getBossUUID();
    if (bossUUID == null) return "";
    Player boss = this.level().getPlayerByUUID(bossUUID);
    if (boss == null) return "";
    return boss.getGameProfile().getName();
  }
  public UUID getBossUUID() {return this.entityData.get(BOSS_UUID).orElse(null);}
  public void setBossUUID(UUID uuid) {
    if (uuid == null) {this.entityData.set(BOSS_UUID, Optional.empty());}
    else {this.entityData.set(BOSS_UUID, Optional.of(uuid));}
  }

	///  combat config -- fixme reconfig to use toggle
	public static final int BF_TARGET_HOSTILE  = 1;
	public static final int BF_TARGET_AGENT    = 1 << 1;
	public static final int BF_WANDER          = 1 << 2;
	public static final int BF_FOLLOW          = 1 << 3;
	public static final int BF_ALLOW_SPECIAL   = 1 << 4;
	public static final int BF_RAPID_SHOOTING  = 1 << 5;
	public int getControlFlg1() {return this.entityData.get(CONTROL_FLAG1);}
	public void setControlFlg1(int dat) {this.entityData.set(CONTROL_FLAG1, dat);}

	public void setFollowEntity(UUID player) {this.followPlayer = player;}
  public int getVirtualAmmo(){return this.entityData.get(VIRTUAL_AMMO);}
  public void setVirtualAmmo(int ammo){this.entityData.set(VIRTUAL_AMMO, ammo);};
  public Integer getFood() {return this.entityData.get(FOOD_VALUE);}
  public void setFood(Integer val) {this.entityData.set(FOOD_VALUE, val);}
  public boolean getFemale() {return this.entityData.get(FEMALE);}
  public void setFemale(boolean flag) {this.entityData.set(FEMALE, flag);}
  public String getSkin() {return this.entityData.get(SKIN);}
  public void setSkin(String skin) {this.entityData.set(SKIN, skin);}

	/// for client cosmetic render
	public ItemStack getChipBrainStack(){return this.entityData.get(BRAIN_CHIP_STACK);}
	public ItemStack getMeleeStack(){return this.entityData.get(MELEE_STACK);}
	public ItemStack getGunStack(){return this.entityData.get(GUN_STACK);}

	public void updateBrainChipStack(){
		entityData.set(BRAIN_CHIP_STACK, inventory2.getItem(0));  // fixme hardcode brainChip Location
	}
	public void updateWeaponStack(){
		entityData.set(MELEE_STACK, inventory1.getItem(MELEE_SLOT));
		entityData.set(GUN_STACK, inventory1.getItem(GUN_SLOT));
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

	public boolean preShoot(boolean aim){
		ItemStack gunStack = getMainHandItem();
		if (!(gunStack.getItem() instanceof AbstractGunItem gunItem)){return false;}
		ResourceLocation gunResource = gunItem.getGunId(gunStack);
		CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
		GunData gundat = gunIndex.getGunData();

		List<FireMode> allFireMode = gundat.getFireModeSet();
		// auto preConfig firing mode for fun.
		if ((getControlFlg1() & BF_RAPID_SHOOTING) == 0) {
			if (allFireMode.contains(FireMode.SEMI)) fmode = FireMode.SEMI;
			else fmode = allFireMode.get(0);  // if this fail, someone else fault
		}
		else if (allFireMode.contains(FireMode.BURST)) fmode = FireMode.BURST;
		else if (allFireMode.contains(FireMode.AUTO)) fmode = FireMode.AUTO;
		else if (allFireMode.contains(FireMode.SEMI)) fmode = FireMode.SEMI;

		long fireCD = gundat.getShootInterval(this, fmode, gunStack);
		tickFireCD = switch (fmode) {
			case AUTO -> (int) (fireCD /50 * 4);  //  full auto get 1/4 speed
			case SEMI -> (int) (fireCD /50 * 16);  //  semi stay at 1/16 speed
			case BURST -> (int) (fireCD /50 * 12);  //  burst stay at 1/12 speed due to multi shot spray
			case UNKNOWN -> 1000;
		};
		// set firing mode. it just nbt so safe to change instance
		if (gunItem.getFireMode(gunStack) != fmode) {gunItem.setFireMode(gunStack, fmode);}
		//handle bolt type gun flag,
		bolt = gundat.getBolt() == Bolt.MANUAL_ACTION;

		ShooterDataHolder dat = op.getDataHolder();
		if (dat.reloadStateType.isReloading() || dat.isBolting) return false;
		if (Utils.hasFriendlyInLineOfFire(this, getTarget())) return false;
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
	public void postShoot(){
		op.aim(false);
		if (bolt) op.bolt();
	}

	private boolean canReload() {
		if (getVirtualAmmo() > 0) return true;
		return inventory1.findGunAmmo(getMainHandItem()) != -1;
	}
	public void executeAmmoReloadMath(){
		int reloadAmount = 0;
		ItemStack gunStack = getMainHandItem();
		if (!(gunStack.getItem() instanceof AbstractGunItem gunItem)) return;  // safety check
		ResourceLocation gunResource = gunItem.getGunId(gunStack);
		CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
		int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, gunIndex.getGunData());
		int curAmmoCount = inventory1.checkAmmoInChamber(gunStack, gunItem);

		// if use virtual ammo
		int virtAmmo = getVirtualAmmo();
		if (virtAmmo > 0){
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
				iAmmoBoxItem.setAmmoCount(ammoStack,iAmmoBoxItem.getAmmoCount(ammoStack)-reloadAmount);
			} else if(ammoStack.getItem() instanceof IAmmo){
				reloadAmount = Math.min(maxAmmoCount - curAmmoCount, ammoStack.getCount());
				ammoStack.setCount(ammoStack.getCount() - reloadAmount);
			}
		}
		gunItem.setCurrentAmmoCount(gunStack,curAmmoCount+reloadAmount);
	}
	private ShootResult shootGun(){return op.shoot(() -> getViewXRot(1f), () -> getViewYRot(1f));}
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
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new EntityRenderPacket(this.getId(), 1));
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

	public boolean shootingTick(){
		if (handleReloadLogic()){return true;}
		return handleShootingLogic();
	}
	public float getGunDmg(){
		ItemStack gunStack = inventory1.getItem(GUN_SLOT);
		if (gunStack.getItem() instanceof ModernKineticGunItem gunItem){
			ResourceLocation gunResource = gunItem.getGunId(gunStack);
			CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunResource).orElse(null);
			return (float) AttachmentDataUtils.getDamageWithAttachment(gunStack, gunIndex.getGunData());
		}
		return 0;
	}

	// switch weapon -- critical: make sure weapon check perform before calling this
	private boolean switching = false;
	public boolean pullWeapon(int type, int tickP){
		switch (type) {
			case 0:{  // empty
				setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				switching = false;
				return true;
			}
			case 1:{  // melee
				if (getMainHandItem() == inventory1.getItem(MELEE_SLOT) && !switching) return true;
				if (tickP == 0) {
					setAniMoveStatic(AnimationID.A_SWORD_DRAW);
					switching = true;
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
						new EntityRenderPacket(getId(), 1));
				}
				else if (tickP == 15) {equipMelee();}
				else if (tickP >= 20) {
					setAniMoveStatic(AnimationID.A_LIVING);
					switching = false;
					return true;
				};
				return false;
			}
			case 2:{
//				System.out.println("TICKING " +tickP);
				if (getMainHandItem() == inventory1.getItem(GUN_SLOT) && !switching) return true;
				if (tickP == 0){
					setAniMoveStatic(AnimationID.A_GUN_DRAW);
					switching = true;
					PacketHandler.CHANNELS.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
						new EntityRenderPacket(getId(), 1));
				}
				else if (tickP == 15) {equipGun();}
				else if (tickP >= 40) {
					setAniMoveStatic(AnimationID.A_LIVING);
					switching = false;
					return true;
				};
				return false;
			}
			default: {return true;}
		}
	}

  /// brain and AI
  protected void registerGoals() {
  }
	public void reloadBrain(){} // fixme make this abstract?
	public void brainOFF(){}
	public void printObservation(String msg, MinecraftServer server){}
	public boolean toggleObserver(UUID id){return false;};

  // check to make sure same owner, or owner in same team,
  public boolean sameTeam(LivingEntity entity) {  // fixme move this to interface dealing?
    if (entity instanceof Player player) {
      return isFriendlyPlayer(player, level());
    } else if (entity instanceof IEntityTeam teamer){
      return isFriendlyMod(teamer, level());
    }
    return false;
  }

	/// inventory & menu
  public SimpleContainer getInventory(){return inventory1;}
  public boolean wantsToPickUp(ItemStack pStack) {return true;}
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
    }
    else if (ItemStack.isSameItemSameTags(special, input) && special.getCount() < special.getMaxStackSize()) {
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
  public boolean hurt(DamageSource source, float amount) {
    if (invincible) {// fixme move this to heavy specific,
      level().playSound(null, this, SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 1.0f, 0.5f);
      return false;
    }
    return super.hurt(source, amount);
  }
  protected void hurtArmor(DamageSource pSource, float pDamage) {
    if (!(pDamage <= 0.0F)) {
      pDamage /= 4.0F;
      if (pDamage < 1.0F) {
        pDamage = 1.0F;
      }
      AtomicBoolean brokenArmorPiece = new AtomicBoolean(false);
      for(ItemStack piece : this.getArmorSlots()) {
        if ((!pSource.is(DamageTypeTags.IS_FIRE) || !piece.getItem().isFireResistant()) && piece.getItem() instanceof ArmorItem) {
          piece.hurtAndBreak((int)pDamage, this, (e) -> {
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
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if(!this.level().isClientSide()) {
			if (player.getMainHandItem().getItem() instanceof BrainChipItem){
				return InteractionResult.PASS;
			}

			if (getBossUUID() == null) {
				setBossUUID(player.getUUID());}
			if (sameTeam(player)) {
				if (player.getMainHandItem().getItem() instanceof SwordItem) {

				} else if (player.isCrouching()) {

				} else {
					NetworkHooks.openScreen((ServerPlayer) player, this, buf -> buf.writeInt(this.getId()));
				}
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
    if (getBossUUID() != null) {nbt.putUUID("owner_uuid", getBossUUID());}
		nbt.putInt("control_flag1", getControlFlg1());

	  nbt.putBoolean("is_female", getFemale());
	  nbt.putString("skin",getSkin());

		nbt.putInt("virtual_ammo", this.getVirtualAmmo());
		nbt.putBoolean("brain_mode", this.brainMode);
  }
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);
    this.persistedFromNBT = true;
    // Load inventory1
    if (nbt.contains("AgentInventory", 9)){
	    ListTag invTag = nbt.getList("AgentInventory", Tag.TAG_COMPOUND);
	    for (int i = 0; i < invTag.size(); i++) {
		    CompoundTag itemTag = invTag.getCompound(i);
		    int slot = itemTag.getByte("Slot") & 255;
		    if (slot < this.inventory1.getContainerSize()) {this.inventory1.setItem(slot, ItemStack.of(itemTag));}
	    }
    }
	  // Load Inv 2
	  if (nbt.contains("AgentInventoryExtra", 9)) {
		  ListTag extraTag = nbt.getList("AgentInventoryExtra", 10);
		  for (int i = 0; i < extraTag.size(); i++) {
			  CompoundTag itemTag = extraTag.getCompound(i);
			  int slot = itemTag.getByte("Slot") & 255;
			  if (slot < this.inventory2.getContainerSize()) {this.inventory2.setItem(slot, ItemStack.of(itemTag));}
		  }
	  }

    // load other data
    setFood(nbt.getInt("Food"));
    if (nbt.contains("owner_uuid")){setBossUUID(nbt.getUUID("owner_uuid"));}
    else {setBossUUID(null);}

	  setControlFlg1(nbt.getInt("control_flag1"));
    setFemale(nbt.getBoolean("is_female"));
    setSkin(nbt.getString("skin"));

    this.setFollowEntity(this.getBossUUID());
    this.setVirtualAmmo(nbt.getInt("virtual_ammo"));
    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.brainMode = nbt.getBoolean("brain_mode");
		//cosmetic render
	  updateBrainChipStack();
		updateWeaponStack();
  }

	/// cosmetic and spawn
  public void initCosmetic(){
    boolean isFemale = ThreadLocalRandom.current().nextBoolean();
    this.setCustomName(Component.literal(Utils.randomName(isFemale)));
    setFemale(isFemale);
    setSkin(isFemale ? Utils.makeSafeSkinName(getAgentConfig().defaultFemaleSkin) : Utils.makeSafeSkinName(getAgentConfig().defaultMaleSkin));
  }
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
    SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    this.setLeftHanded(false);  // everyone use right hand pls
    if (!persistedFromNBT) {
      boolean isFemale = ThreadLocalRandom.current().nextBoolean();
      this.setCustomName(Component.literal(Utils.randomName(isFemale)));
      setFemale(isFemale);
      setSkin(isFemale ? Utils.makeSafeSkinName(getAgentConfig().defaultFemaleSkin) : Utils.makeSafeSkinName(getAgentConfig().defaultMaleSkin));
    }
    return data;
  }
  protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
    super.dropCustomDeathLoot(source, looting, recentlyHit);
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
  }

	/// ticking
  private void passiveRegen() {
    this.setFood(this.getFood() - 1);
    this.heal(1);
  }
  public void tick(){
    super.tick();
    if(!level().isClientSide() && tickCount % 80 == 0) {
      var attr = getAttribute(Attributes.MOVEMENT_SPEED);
      if (getFood() >= maxfood * 0.4) {
        if (attr != null && !attr.hasModifier(WELL_FEED_SPEED_BOOST)) {
          attr.addTransientModifier(WELL_FEED_SPEED_BOOST);
        }
      } else {
        // Remove if no longer well-fed
        if (attr != null && attr.hasModifier(WELL_FEED_SPEED_BOOST)) {
          attr.removeModifier(WELL_FEED_SPEED_BOOST);
        }
      }
      if (getHealth()<getMaxHealth() && getFood() >= maxfood * 0.25) {
        passiveRegen();
      }
    }
  }

	/// navigation
  public boolean moveto(Entity pEntity, double pSpeed){
    if (tickCount - pathCooldown >10) {
      pathCooldown = tickCount;
      return this.getNavigation().moveTo(pEntity, pSpeed);
    }
    return true;
  }
  public boolean moveto(Vec3 target, double pSpeed){
	  if (tickCount - pathCooldown >10) {
		  pathCooldown = tickCount;
      return this.getNavigation().moveTo(target.x, target.y, target.z, pSpeed);
    }
    return true;
  }
  public void stopNav(){
    this.getNavigation().stop();
    this.pathCooldown = 0;
  }

	///  weapon system
  //just stack to mainhand, perfrom check since it call draw, which take entity a few tick to process drawing
  public void equipGun() {
    if (getMainHandItem() != inventory1.getItem(GUN_SLOT)){
      setItemInHand(InteractionHand.MAIN_HAND, inventory1.getItem(GUN_SLOT));
      IGunOperator op = IGunOperator.fromLivingEntity(this);
      op.draw(this::getMainHandItem);
    }
  }
  // just stack to mainhand
  public void equipMelee() {setItemInHand(InteractionHand.MAIN_HAND, inventory1.getItem(MELEE_SLOT));}
  // special
  public void equipSpecial(boolean offhand) {setItemInHand(offhand? InteractionHand.OFF_HAND: InteractionHand.MAIN_HAND, inventory1.getItem(SPECIAL_SLOT));}
  public ItemStack getSpecialSlot() {return inventory1.getItem(SPECIAL_SLOT);}

  // for overwrite later in final class
  public boolean isEquipableGun(ItemStack stack) {
    CompoundTag nbt = stack.getOrCreateTag();
    String gunId = nbt.getString(GUN_ID_TAG);
    if (gunId.isEmpty()) return false;
    return getAgentConfig().allowGuns.contains(gunId);
  }
  public boolean isEquipableMelee(ItemStack stack) {
    if (stack.isEmpty()) return false;
    return (getAgentConfig().allowMelees.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) ||
        stack.getTags().anyMatch(tagKey -> getAgentConfig().allowMelees.contains("#" + tagKey.location())));
  }
  public int getMaxVirtualAmmo(){return getAgentConfig().maxVirtualAmmo;}
  public int getAmmoPerCharge(){return getAgentConfig().chargePerAmmo;}
	public int getScrCustVarSize(){return 1;}
  public int getSensorSize(){return 1;}
	public String getCSVSensorsHeader(){return "";}
	public int getBehaviorSize(){return 1;}
}
