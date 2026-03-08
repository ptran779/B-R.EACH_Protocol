package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class AgentInventory extends SimpleContainer {
  private final AbsAgentEntity agent;
  private int availAmmoSlot = -1;  // use in quick lookup for reload logic when needed
  public AgentInventory(int size, AbsAgentEntity owner) {
    super(size);
    this.agent = owner;
  }

  /// ARMOR STUFF
  // put on the armor, depending on if using auto equip or just whatever in the slot.
  public void loadArmor(){
    // get current load
    //Actually, it maybe fine, Im not responsible for someone else bad design, but FYI
    ItemStack helmet = agent.getItemBySlot(EquipmentSlot.HEAD);
    ItemStack chest = agent.getItemBySlot(EquipmentSlot.CHEST);
    ItemStack legs = agent.getItemBySlot(EquipmentSlot.LEGS);
    ItemStack boots = agent.getItemBySlot(EquipmentSlot.FEET);
    // find best gear
    for (int i=0; i<getContainerSize(); i++){
      ItemStack stack = getItem(i);
      if (stack.getItem() instanceof ArmorItem armor){
        switch (armor.getEquipmentSlot()){
          case HEAD:
            if (helmet.isEmpty() || armor.getDefense() > ((ArmorItem)helmet.getItem()).getDefense()){
              helmet = stack;
              swapItem(agent.GEAR_SLOTS[0], i);
              agent.setItemSlot(armor.getEquipmentSlot(), helmet);
            }
            break;
          case CHEST:
            if (chest.isEmpty() || armor.getDefense() > ((ArmorItem)chest.getItem()).getDefense()){
              chest = stack;
              swapItem(agent.GEAR_SLOTS[1], i);
              agent.setItemSlot(armor.getEquipmentSlot(), chest);
            }
            break;
          case LEGS:
            if (legs.isEmpty() || armor.getDefense() > ((ArmorItem)legs.getItem()).getDefense()){
              legs = stack;
              swapItem(agent.GEAR_SLOTS[2], i);
              agent.setItemSlot(armor.getEquipmentSlot(), legs);
            }
            break;
          case FEET:
            if (boots.isEmpty() || armor.getDefense() > ((ArmorItem)boots.getItem()).getDefense()){
              boots = stack;
              swapItem(agent.GEAR_SLOTS[3], i);
              agent.setItemSlot(armor.getEquipmentSlot(), boots);
            }
            break;
        }
      }
    }
  }
  // external modification may equip living entity with armor. ex. dispenser. This call the itemstack to sync to inventory armor slot
  // empty slot will not overwrite
  public void syncArmor(){
    ItemStack [] fullLoad = {agent.getItemBySlot(EquipmentSlot.HEAD), agent.getItemBySlot(EquipmentSlot.CHEST), agent.getItemBySlot(EquipmentSlot.LEGS), agent.getItemBySlot(EquipmentSlot.FEET)};
    for (int i=0; i<4; i++) {
      if (fullLoad[i].getItem() instanceof ArmorItem) {setItem(agent.GEAR_SLOTS[i], fullLoad[i]);}
    }
  }

  /// FOOD STUFF
  //inv prety small, just find me the best one -- return empty stack if no food
  public ItemStack getBestFood() {
    ItemStack out = ItemStack.EMPTY;
    for (int i=0; i<getContainerSize(); i++) {
      ItemStack stack = getItem(i);
      if (stack.isEdible() && (!out.isEdible() || (stack.getItem().getFoodProperties(stack, agent).getNutrition() > out.getItem().getFoodProperties(stack, agent).getNutrition()))) {
        out = stack;
      }
    }
    return out;
  }

  /// MELEE STUFF
  //assume no autoequip for now
  public boolean meleeExist(){
    return !getItem(agent.MELEE_SLOT).isEmpty();
  }

  /// Fire ARM :) // soft check gun type only. Strict check on menu.
  public boolean gunExist(){
	  return !getItem(agent.GUN_SLOT).isEmpty();
  }
	/// quick scan for if gun has ammo in first place
	public boolean gunExistWithAmmo(){
		ItemStack stack = getItem(agent.GUN_SLOT);
		if (stack.getItem() instanceof ModernKineticGunItem gunItem) {
			return checkAmmoInChamber(stack, gunItem) > 0 || agent.getVirtualAmmo() > 0 || findGunAmmo(stack) != -1;
		}
		return false;
	}
	/// ammoInChamber
	public int checkAmmoInChamber(ItemStack gunStack, AbstractGunItem gunItem){return gunItem.getCurrentAmmoCount(gunStack);}
	public int checkAmmoInChamber(){
		ItemStack gunStack = getItem(agent.GUN_SLOT);
		if (gunStack.getItem() instanceof AbstractGunItem gunItem)return gunItem.getCurrentAmmoCount(gunStack);
		return 0;
	}
	public int maxAmmoInChamber(){
		ItemStack gunStack = getItem(agent.GUN_SLOT);
		if (!(gunStack.getItem() instanceof AbstractGunItem gunItem))return 0;
		CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunItem.getGunId(gunStack)).orElse(null);
		return AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, gunIndex.getGunData());
	}
	/// expensive check: scan everything and figure out how much ammo you have
  public int totalUsableAmmo(){
    ItemStack stack = getItem(agent.GUN_SLOT);
    if (stack.getItem() instanceof ModernKineticGunItem gunItem) {
      int total = checkAmmoInChamber(stack, gunItem) + agent.getVirtualAmmo();  // need to count entire inventory :(
      for (int i=0; i<this.getContainerSize(); i++) total += countAmmoGunSlot(i, stack);
      return total;
    }
    return 0;
  }

  private int countAmmoGunSlot(int slotId, ItemStack gunStack){
    ItemStack checkAmmoStack = getItem(slotId);
    if (checkAmmoStack.getItem() instanceof IAmmo iAmmo) {
      if (iAmmo.isAmmoOfGun(gunStack, checkAmmoStack)) {
        availAmmoSlot = slotId;
        return checkAmmoStack.getCount();
      }
    } else if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox) {
      if (iAmmoBox.isAmmoBoxOfGun(gunStack, checkAmmoStack)){
        availAmmoSlot = slotId;
        return iAmmoBox.getAmmoCount(checkAmmoStack);
      }
    }
    return 0;
  }
  public int findGunAmmo(ItemStack gunStack){
    int ammoCount;
    // check cache
    if (availAmmoSlot != -1){
      ammoCount = countAmmoGunSlot(this.availAmmoSlot, gunStack);
      if (ammoCount != 0){return availAmmoSlot;}
    }
    //scan all inv
    for (int i=0; i<getContainerSize(); i++){
      ammoCount = countAmmoGunSlot(i, gunStack);
      if (ammoCount != 0){return i;}
    }
    return -1;
  };

  // quick check to see if there's weapon in our slot
  public boolean haveWeapon(){return meleeExist() || gunExistWithAmmo();}

  /// UTIL
  protected void swapItem(int id1, int id2){
    if(id1 != id2){
      ItemStack tmp = getItem(id2);
      setItem(id2, getItem(id1));
      setItem(id1, tmp);
    }
  }

  public AbsAgentEntity getAgent(){return agent;}
}
