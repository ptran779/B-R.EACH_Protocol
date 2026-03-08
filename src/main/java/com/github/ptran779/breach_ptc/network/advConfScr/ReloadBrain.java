package com.github.ptran779.breach_ptc.network.advConfScr;

import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

//C->S
public class ReloadBrain {
	int agentId;

	public ReloadBrain(int agentId){
		this.agentId = agentId;
	}

	public void encode(FriendlyByteBuf buf){buf.writeInt(agentId);}
	public static ReloadBrain decode(FriendlyByteBuf buf){return new ReloadBrain(buf.readInt());}
	public void handle(Supplier<NetworkEvent.Context> ctx){
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			Entity e = player.level().getEntity(agentId);
			if (!(e instanceof Swordman swordAgent)) return;  // fixme for quick test only
			swordAgent.reloadBrain();

			ctx.get().setPacketHandled(true);
		});
	}
}
