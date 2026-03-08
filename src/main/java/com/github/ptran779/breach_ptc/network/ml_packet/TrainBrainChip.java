package com.github.ptran779.breach_ptc.network.ml_packet;

import com.github.ptran779.breach_ptc.ai.api.MLServer;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

import static com.github.ptran779.breach_ptc.server.ForgeServerEvent.BRAIN_SERVER;

//C -> S
public class TrainBrainChip {
  UUID unitUUID;
  public TrainBrainChip(UUID unitUUID) {
    this.unitUUID = unitUUID;
  }
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeUUID(unitUUID);
  }
  public static TrainBrainChip decode(FriendlyByteBuf buf) {
    return new TrainBrainChip(buf.readUUID());
  }
  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) {return;}
      MlModelManager.MLUnit unit = MlModelManager.getMUnit(unitUUID, player.level().getGameTime());
      // fixme need to handle max
      if (unit == null || unit.model == null || unit.dataManager == null) {
        PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),new PushDatLog("Missing Stuff Cant train"));
        return;
      }
      BRAIN_SERVER.TASK_QUEUE_TRAIN.add(new MLServer.TrainDatIn(player.getUUID(), MLServer.TARGET_RECEIVER.PLAYER, unitUUID, unit.model, unit.dataManager));
      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),new PushDatLog("Training Sent To Server"));
    });
    ctx.get().setPacketHandled(true);
  }
}
