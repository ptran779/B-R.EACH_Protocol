package com.github.ptran779.breach_ptc.network.ml_packet;

import com.github.ptran779.email.ML;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

// C -> S
public class CreateNewBrain {
  UUID modelUUID;
  byte[] data;
	int input;
	int output;

	// write input & output size to trigger overwrite of mUnit. Else keep internal
  public CreateNewBrain(UUID modelUUID, int input, int output, byte[] data) {
    this.modelUUID = modelUUID;
    this.data = data;
		this.input = input;
		this.output = output;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeUUID(modelUUID);
		buf.writeVarInt(input);
		buf.writeVarInt(output);
    buf.writeByteArray(data);
  }

  public static CreateNewBrain decode(FriendlyByteBuf buf) {
    return new CreateNewBrain(buf.readUUID(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray());
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) {
        return;
      }
      MlModelManager.MLUnit mlunit = MlModelManager.getMUnit(modelUUID, player.level().getGameTime());
      if (mlunit.model == null) {
        PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),new PushDatLog("Model Initialized"));
      } else {
        PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player), new PushDatLog("Model Overwritten"));
      }
      mlunit.model = ML.createModelFromSerialization(data, true);
			// fixme missing config
			if (input != 0 && output != 0) {
				mlunit.inSize = input;
				mlunit.outSize = output;
			}
    });
    ctx.get().setPacketHandled(true);
  }
}
