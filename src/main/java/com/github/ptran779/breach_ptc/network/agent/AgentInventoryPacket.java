package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

// C->S
public class AgentInventoryPacket {
	private final int entityId;

	public AgentInventoryPacket(int entityId) {this.entityId = entityId;}
	public void encode(FriendlyByteBuf buf) {buf.writeInt(entityId);}
	public static AgentInventoryPacket decode(FriendlyByteBuf buf) {return new AgentInventoryPacket(buf.readInt());}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			Entity entity = player.level().getEntity(entityId);
			if (entity instanceof AbsAgentEntity agent) {

				// Create the provider that uses your base Inventory Menu class
				MenuProvider containerProvider = new MenuProvider() {
					public Component getDisplayName() {return Component.literal("Agent Inventory");}
					public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player playerEntity) {
						return new AgentInventoryMenu(windowId, playerInv, agent);
					}
				};

				// Open the base screen and write the ID for the client packet constructor
				NetworkHooks.openScreen(player, containerProvider, buf -> buf.writeInt(agent.getId()));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}