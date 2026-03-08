package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AgentFollowTargetPacket {
  private final int entityId;
  private final UUID targetUUID;

  public AgentFollowTargetPacket(int entityId, UUID targetUUID){
    this.entityId = entityId;
    this.targetUUID = targetUUID;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeVarInt(entityId);
    buf.writeUUID(targetUUID);
  }

  public static AgentFollowTargetPacket decode(FriendlyByteBuf buf){
    int entityId = buf.readVarInt();
    UUID optinalData = buf.readUUID();
    return new AgentFollowTargetPacket(entityId, optinalData);
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      if (player.level().getEntity(entityId) instanceof AbsAgentEntity agent) {
        agent.setFollowEntity(targetUUID);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
