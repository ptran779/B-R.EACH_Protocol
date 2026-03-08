package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// C->S
public class SetATrainConf {
	int entityId;
	boolean autoTrain;
	boolean collectExp;
	int impTime;
	float exploreRate;

	public SetATrainConf(int agentId, boolean autoTrain, boolean collectExp, int impTime, float exploreRate){
		this.entityId = agentId;
		this.autoTrain = autoTrain;
		this.collectExp = collectExp;
		this.impTime = impTime;
		this.exploreRate = exploreRate;
	}
	public void encode(FriendlyByteBuf buf){
		buf.writeVarInt(entityId);
		buf.writeBoolean(autoTrain);
		buf.writeBoolean(collectExp);
		buf.writeVarInt(impTime);
		buf.writeFloat(exploreRate);
	}
	public static SetATrainConf decode(FriendlyByteBuf buf){
		return new SetATrainConf(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readFloat());
	}
	public void handle(Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			Entity entity = player.level().getEntity(entityId);

			// Swap "AbstractAgentEntity" with whatever your actual base swordman class is
			if (entity instanceof Swordman swordman) {
				swordman.swordBrain.autotrain = autoTrain;
				swordman.swordBrain.trySetCollectExp(collectExp);
				swordman.swordBrain.impTime = impTime;
				swordman.swordBrain.exploreRate = exploreRate;
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
