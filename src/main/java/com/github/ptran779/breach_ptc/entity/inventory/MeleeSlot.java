package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MeleeSlot extends Slot {
  private final AbsAgentEntity agent;
  public MeleeSlot(AgentInventory agentInventory, int pSlot, int pX, int pY) {
    super(agentInventory, pSlot, pX, pY);
    this.agent = agentInventory.getAgent();
  }

  @Override
  public boolean mayPlace(ItemStack pStack) {return agent.isEquipableMelee(pStack);}
}
