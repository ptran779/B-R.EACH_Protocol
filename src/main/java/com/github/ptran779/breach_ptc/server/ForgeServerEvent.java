package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.extra.KSeedCore;
import com.github.ptran779.breach_ptc.entity.extra.VoidDrifterModule;
import com.github.ptran779.breach_ptc.player.TaticalCommandProvider;
import com.github.ptran779.email.ML;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.config.ServerConfig;
import com.github.ptran779.breach_ptc.Utils;
import com.github.ptran779.breach_ptc.ai.api.MLServer;
import com.github.ptran779.breach_ptc.entity.agent.Swordman;
import com.github.ptran779.breach_ptc.entity.extra.FallingHellPod;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.ml_packet.PushDatLog;
import com.github.ptran779.breach_ptc.network.ml_packet.TrainDone;
import com.github.ptran779.breach_ptc.network.player.CameraModePacket;
import com.github.ptran779.breach_ptc.network.player.serverConfigPacket;
//import com.github.ptran779.aegisops.player.TaticalCommandProvider;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.config.AgentConfigManager;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.ptran779.breach_ptc.Utils.summonerTest;
import static com.github.ptran779.breach_ptc.config.ServerConfig.PLAYER_DROP_POD;
import static com.github.ptran779.breach_ptc.network.PacketHandler.CHANNELS;
import static com.github.ptran779.breach_ptc.server.EntityInit.*;

@Mod.EventBusSubscriber(modid = BreachPtc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeServerEvent {
	public static MLServer BRAIN_SERVER = null;
	public static Set<UUID> PLAYERS_SERVER_MONITOR = null;

	@SubscribeEvent public static void onServerStarting(ServerStartingEvent event) {
		AgentConfigManager.serverGenerateDefault();
		MlModelManager.initializePaths(event.getServer());
		BRAIN_SERVER = new MLServer();
		BRAIN_SERVER.start();
		PLAYERS_SERVER_MONITOR = new HashSet<>();

	}
	@SubscribeEvent public static void onServerStopping(ServerStoppingEvent event) {
		if (BRAIN_SERVER != null) {
			BRAIN_SERVER.stop();   // stops loop + interrupts thread
			BRAIN_SERVER = null;
		}
		PLAYERS_SERVER_MONITOR = null;
		MlModelManager.cleanAll();
	}
	@SubscribeEvent public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		// all hostile hunt the shit out of agents
		if (event.getEntity() instanceof Monster mob) {
			mob.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(mob, AbsAgentEntity.class, true,
				e -> e instanceof AbsAgentEntity agent
					&& !mob.isAlliedTo(agent)
					&& mob.distanceToSqr(agent) < (mob.getAttributeValue(Attributes.FOLLOW_RANGE) * 0.5)
					* (mob.getAttributeValue(Attributes.FOLLOW_RANGE) * 0.5) // half range squared
			));
		}
	}

	private static int[] getRankedIndices(float[] decisions) {
		int n = decisions.length;
		// 1. Create the result array (The only allocation, unavoidable)
		int[] indices = new int[n];
		for (int i = 0; i < n; i++) indices[i] = i;

		// 2. Insertion Sort (Fastest for tiny arrays like Size 10)
		// Sorts 'indices' based on the values in 'decisions'
		for (int i = 1; i < n; i++) {
			int currentIdx = indices[i];
			float currentVal = decisions[currentIdx];
			int j = i - 1;

			// Shift indices down if their value is smaller than current (Descending)
			while (j >= 0 && decisions[indices[j]] < currentVal) {
				indices[j + 1] = indices[j];
				j--;
			}
			indices[j + 1] = currentIdx;
		}

		return indices;
	}
	private static Entity findEntityByUUID(MinecraftServer server, UUID uuid) {
		// 1. Fast Check: Iterate all loaded levels (Overworld, Nether, End, Modded)
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(uuid);
			if (entity != null) {
				return entity;
			}
		}
		return null; // Entity is offline or chunk unloaded
	}
	public static void printObservation(String msg, MinecraftServer server) {
		if (PLAYERS_SERVER_MONITOR.isEmpty()) return;
		Component msgCom = Component.literal(msg);
		for (UUID id : PLAYERS_SERVER_MONITOR) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				PLAYERS_SERVER_MONITOR.remove(id);
				continue;
			}
			player.sendSystemMessage(msgCom);
		}
	}
	@SubscribeEvent public static void processAgentBehavior(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) return;
		ServerLevel level = event.getServer().getLevel(Level.OVERWORLD);  // this level should always run
		if (level == null) return;
		if (level.getGameTime() % 2 != 0) return;  // fixme remove later? it's pretty cheap to run the queue check
		//infer
		while (BRAIN_SERVER.RESULT_QUEUE_INF.peek() != null) {
			MLServer.InfDatOut payload = BRAIN_SERVER.RESULT_QUEUE_INF.poll();
			// get max payload
			// get agent entity from server?
			Entity rawEntity = findEntityByUUID(event.getServer(), payload.agentUUID());
			if (rawEntity instanceof AbsAgentEntity agent) {
				agent.getSuperBrain().doneComputing();     // mark brain finish
				int[] dec = getRankedIndices(payload.decision());
				printObservation(
					agent.getAgentType() + " " + agent.getDisplayName().getString() + " will try " + Arrays.toString(
						payload.decision()), level.getServer());
				agent.getSuperBrain().tryBehaviorChain(dec);
			}
		}
		//train
		while (BRAIN_SERVER.RESULT_QUEUE_TRAIN.peek() != null) {
			MLServer.TrainDatOut payload = BRAIN_SERVER.RESULT_QUEUE_TRAIN.poll();
			// push into tmp model storage
			MlModelManager.MLUnit target = MlModelManager.getMUnit(payload.modelUUID(), level.getGameTime());
			target.model2 = payload.model();  // push to tmp storage
			if (payload.receiver() == MLServer.TARGET_RECEIVER.PLAYER) {
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(payload.targetUUID());
				if (player != null) {
					ML.TrainStat stat = payload.stats();
					PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player), new PushDatLog(
						String.format("Training Finished [%.0f Epochs] | Time: %.2fms | Score: %.4f -> %.4f", stat.epochRun(),
							stat.trainTimeNs() / 1_000_000f, stat.startScore(), stat.endScore())));
					PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> player), new TrainDone());
				}
			} else if (payload.receiver() == MLServer.TARGET_RECEIVER.AGENT) {
				ML.TrainStat stat = payload.stats();

				boolean upgrade = stat.endScore() - target.model2.minDelta > stat.startScore();
				if (upgrade) target.model = target.model2;
				target.model2 = null;

				for (ServerLevel lev : event.getServer().getAllLevels()) {
					Entity entity = lev.getEntity(payload.targetUUID());
					if (entity instanceof AbsAgentEntity agent) {
						printObservation(agent.getAgentType() + " " + agent.getDisplayName()
								.getString() + " finish self learning with score improvement of " + (stat.endScore() - stat.startScore()),
							level.getServer());
						agent.getSuperBrain().doneComputing();  // turn off blocker, allow brain resume
						if (upgrade) {
							agent.getSuperBrain().failTime = 0;
						} else {
							if (++agent.getSuperBrain().failTime >= agent.getSuperBrain().impTime) {
								agent.getSuperBrain().autotrain = false;
								agent.getSuperBrain().failTime = 0;
							}
							;
						}
						return;
					}
				}
				;
			}
		}
	}

	/// Player deployment on world join first time
	@SubscribeEvent public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		// sync server config so everyone has the same copy of agent config
		if (player instanceof ServerPlayer serverPlayer) {
			// Get the "Long String" from your manager
			String json = AgentConfigManager.getSyncPayload();

			// Send the packet (using your channel instance name 'CHANNELS')
			CHANNELS.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
				new serverConfigPacket(json) // Or S2CAgentConfigSyncPacket, whatever you named it
			);
		}

		// 1st time joining get to be delivered in a hell pod :) UNLESS YOU BORING PPL DONT WANT TO
		if (PLAYER_DROP_POD.get()){
			CompoundTag persistentData = player.getPersistentData();
			CompoundTag data;
			if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
				data = new CompoundTag();
				persistentData.put(Player.PERSISTED_NBT_TAG, data);
			} else {
				data = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
			}
			if (!data.getBoolean("hasJoinedBefore")) {
				data.putBoolean("hasJoinedBefore", true);

				// 🚀 This is the first join!
				player.sendSystemMessage(Component.literal("Welcome Survivor."));
				// spawn pod, play sound, set tags, etc.

				VoidDrifterModule voidDrifter = new VoidDrifterModule(VOID_DRIFTER_MODULE_ENT.get(), player.level());
				voidDrifter.setPos(player.getX(), player.level().getMaxBuildHeight() - 1, player.getZ());
				KSeedCore kSeedCore = new KSeedCore(K_SEED_CORE_ENT.get(), player.level());
				kSeedCore.setPos(player.getX(), player.level().getMaxBuildHeight() - 1, player.getZ());
				kSeedCore.startRiding(voidDrifter);
				player.startRiding(kSeedCore, true);
				float speed = 2;
				// Random horizontal direction
				float yaw = player.level().random.nextFloat() * 360F;
				float yawRad = yaw * (float) (Math.PI / 180F);
				voidDrifter.setDeltaMovement(Mth.sin(-yawRad) * speed, 0D, Mth.cos(yawRad) * speed);
				player.level().addFreshEntity(voidDrifter);
				player.level().addFreshEntity(kSeedCore);

				CHANNELS.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new CameraModePacket());
			}
		}

		// fixme change this/update as needed
		// warning message
		player.sendSystemMessage(Component.literal("[B-R.EACH PROTOCOL (FORMALLY AEGIS OPS) BETA V.2] COMPLETE AI REWORK")
			.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD));
		player.sendSystemMessage(
			Component.literal("Based AI is ready. All special ability has been transfer over").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal(
				"Not recommend for long term ai yet. Based goal system is stable, but just in case I might need to upgrade the AI IO.")
			.withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD));
		player.sendSystemMessage(
			Component.literal("You can get help in discord (link on B-REACH Protocol's modridth page).")
				.withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC));
	}

	/// Telemetry command
	@SubscribeEvent public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> breachPtcCommand =
			Commands.literal("breach_ptc").requires(s -> s.hasPermission(0));

		// server monitoring of the ML compute
		breachPtcCommand.then(Commands.literal("monitorserver").executes(context -> {
			// Logic for server monitoring toggle
			ServerPlayer player = context.getSource().getPlayerOrException();
			UUID uuid = player.getUUID();
			if (PLAYERS_SERVER_MONITOR.contains(uuid)) {
				PLAYERS_SERVER_MONITOR.remove(uuid);
				player.sendSystemMessage(Component.literal("Server monitoring disabled."));
			} else {
				PLAYERS_SERVER_MONITOR.add(uuid);
				player.sendSystemMessage(Component.literal("Server monitoring enabled."));
			}
			return 1;
		}));

		// individual entity monitoring (1 Branch with Tab-Complete)
		breachPtcCommand.then(Commands.literal("monitorentity")
			.then(Commands.argument("id", UuidArgument.uuid()).suggests((context, builder) -> {
				ServerPlayer player = context.getSource().getPlayerOrException();

				// 1. Raytrace 8 blocks to see if they are looking directly at an agent
				EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, player.getEyePosition(),
					player.getEyePosition().add(player.getLookAngle().scale(8)),
					player.getBoundingBox().expandTowards(player.getLookAngle().scale(8)).inflate(1.0D),
					(e) -> e instanceof AbsAgentEntity, 8 * 8);

				if (hit != null && hit.getEntity() instanceof AbsAgentEntity target) {
					// If looking right at one, suggest ONLY that agent's UUID
					builder.suggest(target.getUUID().toString());
				} else {
					// 2. Fallback: Suggest all agents within a 10-block radius
					player.level().getEntitiesOfClass(AbsAgentEntity.class, player.getBoundingBox().inflate(10.0D))
						.forEach(agent -> builder.suggest(agent.getUUID().toString()));
				}

				return builder.buildFuture();
			}).executes(context -> {
				// The execution path is now perfectly clean. Just grab the ID and toggle.
				ServerPlayer player = context.getSource().getPlayerOrException();
				UUID targetId = UuidArgument.getUuid(context, "id");

				Entity entity = ((ServerLevel) player.level()).getEntity(targetId);

				if (entity instanceof AbsAgentEntity agent) {
					// Optional toggle logic: if already monitoring, remove them. Otherwise, add them.
					// (Assuming you have a way to check observers, otherwise just do addObserver)
					if (agent.toggleObserver(player.getUUID())) {
						context.getSource()
							.sendSuccess(() -> Component.literal("Monitoring: " + agent.getDisplayName().getString()), false);
					} else {
						context.getSource()
							.sendSuccess(() -> Component.literal("Stop Monitoring: " + agent.getDisplayName().getString()), false);
					}
				} else {
					context.getSource().sendFailure(Component.literal("Agent with that UUID not found."));
				}
				return 1;
			})));

		breachPtcCommand.then(Commands.literal("summoner_test").requires(s -> s.hasPermission(2)) // op level
			.executes(context -> {
				summonerTest(context);
				return 1;
			}));

		// 4. Finally, register the fully built tree
		event.getDispatcher().register(breachPtcCommand);
	}

	// Capabilities stuff
	@SubscribeEvent public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player) {
			if (!event.getObject().getCapability(TaticalCommandProvider.TATICAL_COMMAND_CAPABILITY).isPresent()) {
				event.addCapability(new ResourceLocation(BreachPtc.MOD_ID, "properties"), new TaticalCommandProvider());
			}
		}
	}
	@SubscribeEvent public static void onPlayerCloned(PlayerEvent.Clone event) {
		if (event.isWasDeath()) {
			event.getOriginal().getCapability(TaticalCommandProvider.TATICAL_COMMAND_CAPABILITY).ifPresent(oldStore -> {
				event.getOriginal().getCapability(TaticalCommandProvider.TATICAL_COMMAND_CAPABILITY).ifPresent(newStore -> {
					newStore.copyFrom(oldStore);
				});
			});
		}
	}
	@SubscribeEvent public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.register(TaticalCommandProvider.class);
	}
}
