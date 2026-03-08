package com.github.ptran779.breach_ptc.entity.inventory;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.server.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AgentInventoryMenu extends AbstractContainerMenu {
    private final AgentInventory agentInv;
    private final Container playerInv;
    public final AbsAgentEntity agent;

    //client packet
    public AgentInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (AbsAgentEntity) playerInventory.player.level().getEntity(buf.readInt()));
    }

    //server construct
    public AgentInventoryMenu(int containerId, Inventory playerInventory, AbsAgentEntity agent) {
        super(MenuInit.BREACH_PTC_MENU1.get(), containerId);
        this.agent = agent;
        this.agentInv = agent.inventory1;
        this.playerInv = playerInventory;

        // sync armor in event of modification
        agentInv.syncArmor();

        //load main
        loadInventoryMenu();
    }

    private void loadInventoryMenu() {
        for (int i = 7; i < this.agentInv.getContainerSize(); i++) {
            this.addSlot(new Slot(this.agentInv, i, 61 + (i-7) * 18, 61));
        }
        // agent load out
        this.addSlot(new GunSlot(this.agentInv, agent.GUN_SLOT, 14, 127));    // gun
        this.addSlot(new MeleeSlot(this.agentInv, agent.MELEE_SLOT, 34, 127));    // sword
        this.addSlot(new Slot(this.agentInv, agent.SPECIAL_SLOT, 24, 147));    // special
        this.addSlot(new ArmorSlot(this.agentInv, agent.GEAR_SLOTS[0], 14, 87, EquipmentSlot.HEAD));
        this.addSlot(new ArmorSlot(this.agentInv, agent.GEAR_SLOTS[1], 34, 87, EquipmentSlot.CHEST));
        this.addSlot(new ArmorSlot(this.agentInv, agent.GEAR_SLOTS[2], 14, 107, EquipmentSlot.LEGS));
        this.addSlot(new ArmorSlot(this.agentInv, agent.GEAR_SLOTS[3], 34, 107, EquipmentSlot.FEET));

        //player hotbar
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(this.playerInv, c, 61 + c * 18, 149));
        }

        // player slot
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(this.playerInv, c + r * 9+9, 61 + c * 18, 90 + r * 18));
            }
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

    public void removed(Player player) {
        super.removed(player);
        agentInv.loadArmor();
	      agent.updateWeaponStack();
    }
}
