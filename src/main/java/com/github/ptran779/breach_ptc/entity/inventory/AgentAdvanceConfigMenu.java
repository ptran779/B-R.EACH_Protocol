package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import com.github.ptran779.breach_ptc.server.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AgentAdvanceConfigMenu extends AbstractContainerMenu implements ContainerListener {
  public final SimpleContainer agentInv;
  public final AbsAgentEntity agent;
  public final Container playerInv;
  public final DataSlot mlInput = DataSlot.standalone();
  public final DataSlot mlOutput = DataSlot.standalone();

    //client packet
    public AgentAdvanceConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (AbsAgentEntity) playerInventory.player.level().getEntity(buf.readInt()));
    }

    //server construct
    public AgentAdvanceConfigMenu(int containerId, Inventory playerInventory, AbsAgentEntity agent) {
      super(MenuInit.BREACH_PTC_MENU2.get(), containerId);
      this.agent = agent;
      this.agentInv = agent.inventory2;
      playerInv = playerInventory;

      this.addDataSlot(mlInput);
      this.addDataSlot(mlOutput);

	    loadInventoryMenu();
	    this.agentInv.addListener(this);
	    updateML();
    }

    private void loadInventoryMenu() {
      this.addSlot(new BrainChipSlot(this.agentInv, 0, 14, 87));
      //player hotbar
      for (int c = 0; c < 9; c++) {
          this.addSlot(new Slot(this.playerInv, c, 61 + c * 18, 149));
      }
    }

  public ItemStack quickMoveStack(Player player, int stackIndex) {
    Slot slot = this.slots.get(stackIndex);
    ItemStack stack = slot.getItem();
    // empty slot
    if (!slot.hasItem() || stack.getCount() <=0) {return ItemStack.EMPTY;}
    ItemStack copyStack = stack.copy();

    ItemStack itemStack = slot.getItem();
    if (stackIndex < this.agentInv.getContainerSize() && !this.moveItemStackTo(itemStack, this.agentInv.getContainerSize(), this.slots.size(), false)) {
      return ItemStack.EMPTY;
    } else if (!this.moveItemStackTo(itemStack, 0, this.agentInv.getContainerSize(), true)) {
      return ItemStack.EMPTY;
    }

    slot.onTake(player, itemStack);
    return copyStack;
  }

  @Override
  public boolean stillValid(Player player) {
      return this.agentInv.stillValid(player);
  }

	@Override
	public void containerChanged(Container pContainer) {
    if (this.agent.level().isClientSide) {return;}
		updateML();
	}

	private void updateML(){
		ItemStack stack = getSlot(0).getItem();
		if (stack.getItem() instanceof BrainChipItem brainChipItem){
			brainChipItem.linkAgent(stack, agent);
			MlModelManager.MLUnit unit = MlModelManager.getMUnit(brainChipItem.getOrCreateUUID(stack), agent.level().getGameTime());
			mlInput.set(unit.inSize);
			mlOutput.set(unit.outSize);
		} else {
			agent.brainOFF();  //critical just turn brain off as safety
		}
		agent.updateBrainChipStack();
	}

	@Override
	public void removed(Player player) {
	    super.removed(player);
	    this.agentInv.removeListener(this);
	}
}
