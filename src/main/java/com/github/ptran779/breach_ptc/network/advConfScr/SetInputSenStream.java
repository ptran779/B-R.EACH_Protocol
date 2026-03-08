package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

//C->S
public class SetInputSenStream {
	private final int entityId;
	private final int inLen;
	private final float[] dat;

	public SetInputSenStream(int entityId, float[] dat) {
		this.entityId = entityId;
		this.dat = dat;
		this.inLen = dat.length;
	}
	// Encoder: Client writes to buffer
	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId); // Tell server which agent to update
		buf.writeVarInt(inLen);
		for (int i = 0; i < inLen; i++) {
			buf.writeFloat(dat[i]);
		}
	}
	public static SetInputSenStream decode(FriendlyByteBuf buf) {
		int eId = buf.readInt();
		int len = buf.readVarInt();
		float[] configArr = new float[len];
		for (int i = 0; i < len; i++) {
			configArr[i] = buf.readFloat();
		}
		return new SetInputSenStream(eId, configArr);
	}
	// Handler: Server processes the data
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			Entity entity = player.level().getEntity(entityId);

			// Swap "AbstractAgentEntity" with whatever your actual base swordman class is
			if (entity instanceof Swordman swordman) {
				// "Ask the brain for public int getInputSpace()"
				int brainSpace = SwordBrain.INPUT_SPACE;

				// "If user is stupid... just do as much as you can"
				// This safely prevents ArrayOutOfBounds if dat is longer than brainSpace,
				// and prevents crashes if dat is shorter than brainSpace.
				int safeLength = Math.min(this.inLen, brainSpace);

				for (int i = 0; i < safeLength; i++) {
					swordman.swordBrain.inputDeviation[i] = this.dat[i];
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}

	// --- CLIENT-SIDE HELPER ---

	/**
	 * Call this when the user clicks "Save" or closes the text box.
	 * It splits the comma-separated string and verifies every piece is a valid float.
	 * Returns the float[] if valid, or null if the user typed garbage.
	 */
	public static float[] parseAndValidate(String payload) {
		if (payload == null || payload.isBlank()) return new float[0];

		String[] parts = payload.split(",");
		float[] result = new float[parts.length];

		for (int i = 0; i < parts.length; i++) {
			try {
				// trim() removes accidental spaces like "0.5, 0.1 ,  0.8"
				result[i] = Float.parseFloat(parts[i].trim());
			} catch (NumberFormatException e) {
				// Validation failed: User typed a letter or multiple decimals
				return null;
			}
		}
		return result;
	}
}