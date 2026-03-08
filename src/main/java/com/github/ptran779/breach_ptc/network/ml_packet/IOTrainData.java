package com.github.ptran779.breach_ptc.network.ml_packet;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

// C -> S
public class IOTrainData {
  int agentID;
	String filename;
  UUID modelUUID;
	boolean writeMode;
	public IOTrainData(int agentID, UUID uuid, String filename, boolean writeMode) {
		this.agentID = agentID;  // this should break it right?
		this.modelUUID = uuid;
		this.filename = filename;
		this.writeMode = writeMode;
	}

  public void encode(FriendlyByteBuf buf) {
    buf.writeVarInt(agentID);
		buf.writeUUID(modelUUID);
    buf.writeUtf(filename);
		buf.writeBoolean(writeMode);
  }
  public static IOTrainData decode(FriendlyByteBuf buf) {
    return new IOTrainData(buf.readVarInt(), buf.readUUID(), buf.readUtf(), buf.readBoolean());
  }
  public void handle(Supplier<NetworkEvent.Context> ctx){
    ctx.get().enqueueWork(() -> {
	    ServerPlayer player = ctx.get().getSender();
	    if (player == null) {
		    return;
	    }
	    MlModelManager.MLUnit unit = MlModelManager.getMUnit(modelUUID, player.level().getGameTime());
	    if (writeMode) {
				// get proper header
		    String header = "";
		    if (agentID != -1) {
			    Entity e = player.level().getEntity(agentID);
			    if (e instanceof AbsAgentEntity agent) header = agent.getCSVSensorsHeader();
		    };
				// write
				if (unit.dataManager == null || unit.dataManager.getRawDat().isEmpty()) {return;}
					String msg = MlModelManager.exportData(unit, filename, header);
					PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player), new PushDatLog(msg));
					PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
						new GetTrainDataList(MlModelManager.getAvailableCsvs()));
	    } else {
		    if (unit.dataManager == null) {
			    BreachPtc.LOGGER.error("[AegisOps Critical] This should never happen Please check all critical infrastructure!");
			    return;
		    }
		    String msg = MlModelManager.importData(unit, filename);
		    // send to client UpdateDatSizePacket
		    PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player), new PushDatLog(msg));
		    PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
			    new UpdateTrainDataSize(unit.dataManager.getRawDat().size()));
	    }
	    });
    ctx.get().setPacketHandled(true);
  }
}
