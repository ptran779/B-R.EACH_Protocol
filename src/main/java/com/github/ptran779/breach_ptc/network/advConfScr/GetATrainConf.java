package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.client.screens.AgentAdvanceConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// S->C
public class GetATrainConf {
	boolean autoTrain;
	boolean collectExp;
	int impTime;
	float exploreRate;

	public GetATrainConf(boolean autoTrain, boolean collectExp, int impTime, float exploreRate){
		this.autoTrain = autoTrain;
		this.collectExp = collectExp;
		this.impTime = impTime;
		this.exploreRate = exploreRate;
	}
	public void encode(FriendlyByteBuf buf){
		buf.writeBoolean(autoTrain);
		buf.writeBoolean(collectExp);
		buf.writeVarInt(impTime);
		buf.writeFloat(exploreRate);
	}
	public static GetATrainConf decode(FriendlyByteBuf buf){
		return new GetATrainConf(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readFloat());
	}
	public void handle(Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GetATrainConf.ClientHandler.handlePacket(autoTrain, collectExp, impTime, exploreRate));
		});
		ctx.get().setPacketHandled(true);
	}
	private static class ClientHandler {
		public static void handlePacket(boolean autoTrain, boolean collectExp, int impTime, float exploreRate) {
			if (Minecraft.getInstance().screen instanceof AgentAdvanceConfigScreen screen) {
				// Reassemble the formula using the decompiler
				screen.updateATrainConf(autoTrain, collectExp, impTime, exploreRate);
			}
		}
	}
}
