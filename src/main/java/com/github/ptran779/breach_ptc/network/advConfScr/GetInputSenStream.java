package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.client.screens.AgentAdvanceConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

//S->C
public class GetInputSenStream {
	int inLen;
	float[] dat;
	public GetInputSenStream(float[] dat){
		this.dat = dat;
		inLen = dat.length;
	};
	public void encode(FriendlyByteBuf buf){
		buf.writeVarInt(inLen);
		for (int i=0; i< inLen; i++){buf.writeFloat(dat[i]);}
	}
	public static GetInputSenStream decode(FriendlyByteBuf buf){
		int len = buf.readVarInt();
		float[] configArr = new float[len];
		for (int i=0; i< len; i++){configArr[i] = buf.readFloat();}
		return new GetInputSenStream(configArr);
	}
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < dat.length; i++) {
				// We append the raw float
				sb.append(dat[i]);

				// Only add a comma if it's not the last element
				if (i < dat.length - 1) {sb.append(", ");}
			}
			String payload = sb.toString();;
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handlePacket(payload));});
		ctx.get().setPacketHandled(true);
	}
	private static class ClientHandler {
		public static void handlePacket(String payload) {
			// Check if the player is actually looking at the screen
			if (Minecraft.getInstance().screen instanceof AgentAdvanceConfigScreen screen) {
				screen.updateInSenBox(payload);
			}
		}
	}
}
