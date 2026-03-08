package com.github.ptran779.breach_ptc.network.agent;

import com.github.ptran779.email.ML;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

//S->C
public class BrainChipScreen {
	final int agentID;
	final UUID modelUUID;
	final int inputLen;
	final int outputLen;
	final boolean trainMode;
	final byte[] modelByte;
	final byte[] configByte;

	public BrainChipScreen(int agentID, UUID modelUUID, int inputLen, int outputLen, boolean trainMode, byte[] configByte,
	                       byte[] modelByte) {
		this.agentID = agentID;
		this.modelUUID = modelUUID;
		this.inputLen = inputLen;
		this.outputLen = outputLen;
		this.trainMode = trainMode;
		this.configByte = configByte;
		this.modelByte = modelByte;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(agentID);
		buf.writeUUID(modelUUID);
		buf.writeVarInt(inputLen);
		buf.writeVarInt(outputLen);
		buf.writeBoolean(trainMode);
		buf.writeBytes(configByte);
		buf.writeByteArray(modelByte);
		// safety
		if (configByte.length != ML.CONFIG_BYTE_SIZE) {
			throw new RuntimeException("Config must be " + ML.CONFIG_BYTE_SIZE + " bytes!");
		}
	}

	public static BrainChipScreen decode(FriendlyByteBuf buf) {
		int agentID = buf.readVarInt();
		UUID uuid = buf.readUUID();
		int input = buf.readVarInt();
		int output = buf.readVarInt();
		boolean trainMode = buf.readBoolean();
		// 1. Read Config (Fixed bytes)
		byte[] config = new byte[ML.CONFIG_BYTE_SIZE];
		buf.readBytes(config);
		// 2. Read Model (Variable length)
		byte[] model = buf.readByteArray();
		return new BrainChipScreen(agentID, uuid, input, output, trainMode, config, model);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> ClientHandler.openScreen(agentID, modelUUID, inputLen, outputLen, trainMode, configByte, modelByte)));
		ctx.get().setPacketHandled(true);
	}

	// This inner class is only loaded if DistExecutor calls it
	// It keeps the imports isolated.
	private static class ClientHandler {
		public static void openScreen(int agentID, UUID uuid, int inputLen, int outputLen, boolean trainMode,
		                              byte[] rawConfig,
		                              byte[] rawDat) {
			// It is safe to import/use client classes here
			com.github.ptran779.breach_ptc.client.screens.BrainChipScreen screen =
				new com.github.ptran779.breach_ptc.client.screens.BrainChipScreen(agentID, uuid, inputLen, outputLen, trainMode);
			// wip load canvas layer from json
			screen.setCurModel(rawDat);
			screen.setTrainConfig(rawConfig);
			// load screen
			net.minecraft.client.Minecraft.getInstance().setScreen(screen);
		}
	}
}