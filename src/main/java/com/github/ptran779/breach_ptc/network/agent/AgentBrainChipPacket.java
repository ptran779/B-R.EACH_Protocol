package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.email.ML;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.ml_packet.GetTrainDataList;
import com.github.ptran779.breach_ptc.network.ml_packet.UpdateTrainDataSize;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.github.ptran779.breach_ptc.config.MlModelManager.getAvailableCsvs;

//C->S
public class AgentBrainChipPacket {
	final int agentID;
	final UUID unitUUID;
	public AgentBrainChipPacket(int agentID, UUID uuid) {
		this.agentID = agentID;
		this.unitUUID = uuid;
	}
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(agentID);
		buf.writeUUID(unitUUID);
	}
	public static AgentBrainChipPacket decode(FriendlyByteBuf buf) {
		return new AgentBrainChipPacket(buf.readVarInt(),buf.readUUID());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(unitUUID, player.level().getGameTime());
			byte[] rawModel = mUnit.model==null ? new byte[0]: mUnit.model.modelSimpleSerialize();
			byte[] rawConfig = ML.trainConfigSerialize(mUnit.model);
			boolean trainMode = false;
			int trainDatLen = 0;
			if(mUnit.dataManager != null) {
				trainMode = true;
				trainDatLen = mUnit.dataManager.getRawDat().size();
			}
			PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
				new BrainChipScreen(agentID, unitUUID, mUnit.inSize, mUnit.outSize, trainMode, rawConfig, rawModel));
			List<String> messy = getAvailableCsvs();
			PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
				new GetTrainDataList(messy));
			PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player),
				new UpdateTrainDataSize(trainDatLen));
		});
		ctx.get().setPacketHandled(true);
	}
}
