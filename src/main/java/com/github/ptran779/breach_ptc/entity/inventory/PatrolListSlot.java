package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.item.BrainChipItem;
import com.github.ptran779.breach_ptc.item.PatrolList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PatrolListSlot extends Slot {
  public PatrolListSlot(Container agentInventory, int pSlot, int pX, int pY) {
    super(agentInventory, pSlot, pX, pY);
  }

  @Override
  public boolean mayPlace(ItemStack pStack) {return pStack.getItem() instanceof PatrolList;}
}
