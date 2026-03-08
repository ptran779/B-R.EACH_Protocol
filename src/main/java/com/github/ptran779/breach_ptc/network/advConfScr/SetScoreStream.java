package com.github.ptran779.breach_ptc.network.advConfScr;


import com.github.ptran779.breach_ptc.ai.api.ScoreCompiler;
import com.github.ptran779.breach_ptc.ai.brain.SwordBrain;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.github.ptran779.breach_ptc.BreachPtc.LOGGER;

//C->S
public class SetScoreStream {
	private final int entityId;
	private final byte[] instructions;
	private final float[] constants;

	public SetScoreStream(int entityId, byte[] instructions, float[] constants) {
		this.entityId = entityId;
		this.instructions = instructions;
		this.constants = constants;
	}
	public void encode(FriendlyByteBuf buf) {
		// FriendlyByteBuf has native byte[] support (writes length + bytes automatically)
		buf.writeVarInt(entityId);
		buf.writeByteArray(instructions);

		// Write floats manually: length first, then loop the data
		buf.writeVarInt(constants.length);
		for (float c : constants) {
			buf.writeFloat(c);
		}
	}
	public static SetScoreStream decode(FriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		byte[] instructions = buf.readByteArray();

		int constLen = buf.readVarInt();
		float[] constants = new float[constLen];
		for (int i = 0; i < constLen; i++) {
			constants[i] = buf.readFloat();
		}

		return new SetScoreStream(entityId, instructions, constants);
	}
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			Entity entity = player.level().getEntity(entityId);
			// Swap "AbstractAgentEntity" with whatever your actual base swordman class is
			if (entity instanceof Swordman swordman) {
				// "Ask the brain for public int getInputSpace()"
				ScoreCompiler score = new ScoreCompiler(instructions, constants);
				try{
					score.validate(swordman.getSensorSize(), swordman.getScrCustVarSize());
					SwordBrain brain = swordman.swordBrain;
					brain.scoreFunc = score;
				} catch (RuntimeException exception){
					LOGGER.warn("Someone trying to inject wrong score func on server for {}", swordman.getUUID());
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
