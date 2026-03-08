package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.ai.api.ScoreCompiler;
import com.github.ptran779.breach_ptc.client.screens.AgentAdvanceConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

//S->C
public class GetScoreStream {
	private final byte[] instructions;
	private final float[] constants;

	public GetScoreStream(byte[] instructions, float[] constants) {
		this.instructions = instructions;
		this.constants = constants;
	}

	public void encode(FriendlyByteBuf buf) {
		// FriendlyByteBuf has native byte[] support (writes length + bytes automatically)
		buf.writeByteArray(this.instructions);

		// Write floats manually: length first, then loop the data
		buf.writeVarInt(this.constants.length);
		for (float c : this.constants) {
			buf.writeFloat(c);
		}
	}
	public static GetScoreStream decode(FriendlyByteBuf buf) {
		byte[] instructions = buf.readByteArray();

		int constLen = buf.readVarInt();
		float[] constants = new float[constLen];
		for (int i = 0; i < constLen; i++) {
			constants[i] = buf.readFloat();
		}

		return new GetScoreStream(instructions, constants);
	}
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handlePacket(this.instructions, this.constants));
		});
		ctx.get().setPacketHandled(true);
	}

	private static class ClientHandler {
		public static void handlePacket(byte[] instructions, float[] constants) {
			if (Minecraft.getInstance().screen instanceof AgentAdvanceConfigScreen screen) {
				// Reassemble the formula using the decompiler
				String reconstructedFormula = ScoreCompiler.decompile(instructions, constants);
				screen.updateCostFuncBox(reconstructedFormula);
			}
		}
	}
}
