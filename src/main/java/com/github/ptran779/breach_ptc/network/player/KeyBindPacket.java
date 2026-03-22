package com.github.ptran779.breach_ptc.network.player;


import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.player.TaticalCommandProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class KeyBindPacket {
	private static final int SCAN_RADIUS = 12;  // FIXME
	private final int payload;

	public KeyBindPacket(int payload) {
		this.payload = payload;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(payload);
	}

	public static KeyBindPacket decode(FriendlyByteBuf buf) {
		int payload = buf.readInt();
		return new KeyBindPacket(payload);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			// need to make sure it has the ability
			player.getCapability(TaticalCommandProvider.TATICAL_COMMAND_CAPABILITY).ifPresent(cap -> {
				switch (payload) {
					case 0: {
						cap.cycleFollowMode();
						boolean follow = cap.getFollowMode();
						player.sendSystemMessage(
							Component.literal("All nearby agent set to " + (follow ? "Following" : "Staying Guard")));

						AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
						List<AbsAgentEntity> agents =
							player.level().getEntitiesOfClass(AbsAgentEntity.class, scanBox, agent -> agent.isBoss(player));

						for (AbsAgentEntity agent : agents) {
							int newFlg = agent.getControlFlg1();
							if ((newFlg & EntityUtils.BF_FOLLOW_GROUP_ORDER) != 0 ) continue;
							if (follow) {
								newFlg |= EntityUtils.BF_FOLLOW;
							} else {
								newFlg &= ~EntityUtils.BF_FOLLOW;
							}
							agent.setControlFlg1(newFlg);
						}
						break;
					}
					case 1: {
						cap.cycleTargetMode();
						player.sendSystemMessage(Component.literal("All nearby agent set to " + switch (cap.getTargetMode()) {
							case 0 -> "relax";
							case 1 -> "hunt hostiles";
							case 2 -> "hunt enemy agents";
							case 3 -> "hunt all dangers";
							default -> "Not sure";
						}));
						// find all agent under control
						AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);

						List<AbsAgentEntity> agents =
							player.level().getEntitiesOfClass(AbsAgentEntity.class, scanBox, agent -> agent.isBoss(player));
						int tgMode = cap.getTargetMode();
						for (AbsAgentEntity agent : agents) {
							int newFlg = agent.getControlFlg1();
							if ((newFlg & EntityUtils.BF_FOLLOW_GROUP_ORDER) != 0 ) continue;
							switch (tgMode) {
								case 0 -> newFlg &= ~(EntityUtils.BF_TARGET_HOSTILE | EntityUtils.BF_TARGET_AGENT);
								case 1 -> {
									newFlg |= EntityUtils.BF_TARGET_HOSTILE;
									newFlg &= ~EntityUtils.BF_TARGET_AGENT;
								}
								case 2 -> {
									newFlg &= ~EntityUtils.BF_TARGET_HOSTILE;
									newFlg |= EntityUtils.BF_TARGET_AGENT;
								}
								case 3 -> {
									newFlg |= EntityUtils.BF_TARGET_HOSTILE;
									newFlg |= EntityUtils.BF_TARGET_AGENT;
								}
							}
							agent.setControlFlg1(newFlg);
						}
						break;
					}
					case 2: {
						cap.toggleSpecialMode();
						boolean special = cap.isSpecialMode();
						player.sendSystemMessage(Component.literal(
							"All nearby agents set to " + (special ? "use special" : "don't use special")));

						AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
						List<AbsAgentEntity> agents =
							player.level().getEntitiesOfClass(AbsAgentEntity.class, scanBox, agent -> agent.isBoss(player));

						for (AbsAgentEntity agent : agents) {
							int newFlg = agent.getControlFlg1();
							if ((newFlg & EntityUtils.BF_FOLLOW_GROUP_ORDER) != 0 ) continue;
							if (special) {
								newFlg |= EntityUtils.BF_ALLOW_SPECIAL;
							} else {
								newFlg &= ~EntityUtils.BF_ALLOW_SPECIAL;
							}
							agent.setControlFlg1(newFlg);
						}
						break;
					}
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
