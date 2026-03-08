package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AgentDismissPacket {
  private final int entityId;

  public AgentDismissPacket(int entityId){
    this.entityId = entityId;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeInt(entityId);
  }

  public static AgentDismissPacket decode(FriendlyByteBuf buf){
    int entityId = buf.readInt();
    return new AgentDismissPacket(entityId);
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;

      Entity e = player.level().getEntity(entityId);
      if (!(e instanceof AbsAgentEntity agent)) return;
      agent.setBossUUID(null);
    });
    ctx.get().setPacketHandled(true);
  }
}