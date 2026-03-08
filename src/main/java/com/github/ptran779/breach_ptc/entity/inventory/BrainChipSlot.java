package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.item.BrainChipItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BrainChipSlot extends Slot {
  public BrainChipSlot(Container agentInventory, int pSlot, int pX, int pY) {
    super(agentInventory, pSlot, pX, pY);
  }

  @Override
  public boolean mayPlace(ItemStack pStack) {return pStack.getItem() instanceof BrainChipItem;}
}
