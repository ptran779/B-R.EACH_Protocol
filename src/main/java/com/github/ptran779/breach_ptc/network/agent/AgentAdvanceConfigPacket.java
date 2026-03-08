package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import com.github.ptran779.breach_ptc.entity.inventory.AgentAdvanceConfigMenu;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.advConfScr.GetATrainConf;
import com.github.ptran779.breach_ptc.network.advConfScr.GetInputSenStream;
import com.github.ptran779.breach_ptc.network.advConfScr.GetScoreStream;
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
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;
//C->S
public class AgentAdvanceConfigPacket {
  private final int entityId;

  public AgentAdvanceConfigPacket(int entityId) {
    this.entityId = entityId;
  }

  // Encoder: Write to buffer
  public void encode(FriendlyByteBuf buf) {
    buf.writeInt(entityId);
  }

  // Decoder: Read from buffer
  public static AgentAdvanceConfigPacket decode(FriendlyByteBuf buf) {
    return new AgentAdvanceConfigPacket(buf.readInt());
  }

  // Handler: Logic on the Server
  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;

      Entity entity = player.level().getEntity(entityId);
      if (entity instanceof AbsAgentEntity agent) {
        // Create the provider that uses YOUR existing Menu class
        MenuProvider containerProvider = new MenuProvider() {
          public Component getDisplayName() {return Component.literal("Advance Config");}
          public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player playerEntity) {
            return new AgentAdvanceConfigMenu(windowId, playerInv, agent);
          }
        };

        // Open screen and write the ID for the //client packet constructor
        NetworkHooks.openScreen(player, containerProvider, buf -> buf.writeInt(agent.getId()));
				// init some update based for the container -- universalzied later -- fixme
	      if (agent instanceof Swordman swordman){
		      SwordBrain brain = swordman.swordBrain;
		      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
			      new GetInputSenStream(brain.inputDeviation));
					if (brain.scoreFunc != null){
						PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
							new GetScoreStream(brain.scoreFunc.getInstructions(),
								brain.scoreFunc.getConstants()));
					}
		      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
			      new GetATrainConf(brain.autotrain, brain.getCollectExp(), brain.impTime, brain.exploreRate));
	      }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}