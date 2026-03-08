package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AgentConFlg1Packet {
  private final int entityId;
  private final int payload;          //action payload -- expanse me if need more complex data communication

  public AgentConFlg1Packet(int entityId, int flags){
    this.entityId = entityId;
    this.payload = flags;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeVarInt(entityId);
    buf.writeVarInt(payload);
  }

  public static AgentConFlg1Packet decode(FriendlyByteBuf buf){
    int entityId = buf.readVarInt();
    int payload = buf.readVarInt();
    return new AgentConFlg1Packet(entityId, payload);
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;

      Entity e = player.level().getEntity(entityId);
      if (!(e instanceof AbsAgentEntity agent)) return;
      agent.setControlFlg1(payload);
    });
    ctx.get().setPacketHandled(true);
  }
}
//agent.setAllowSpecial(payload)