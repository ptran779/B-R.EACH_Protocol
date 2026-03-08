package com.github.ptran779.breach_ptc.network.player;

//import com.github.ptran779.aegisops.player.TaticalCommandProvider;


//public class KeyBindPacket {
//  private static final int SCAN_RADIUS = 16;  // FIXME
//  private final int payload;
//
//  public KeyBindPacket(int payload) {
//    this.payload = payload;
//  }
//
//  public void encode(FriendlyByteBuf buf) {
//    buf.writeInt(payload);
//  }
//
//  public static KeyBindPacket decode(FriendlyByteBuf buf) {
//    int payload = buf.readInt();
//    return new KeyBindPacket(payload);
//  }
//
//  public void handle(Supplier<NetworkEvent.Context> ctx) {
//    ctx.get().enqueueWork(() -> {
//      ServerPlayer player = ctx.get().getSender();
//      if (player == null) return;
//      // need to make sure it has the ability
//      player.getCapability(TaticalCommandProvider.TATICAL_COMMAND_CAPABILITY).ifPresent(cap -> {
//        switch (payload){
//          case 0: {
//            cap.cycleFollowMode();
//            player.sendSystemMessage(Component.literal("All nearby agent set to " +
//                switch (cap.getFollowMode()) {
//                  case WANDER -> "Wandering";
//                  case STAY -> "Staying Guard";
//                  case FOLLOW -> "Following";
//                }
//            ));
//            // find all agent under control
//            AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
//
//            List<AbstractAgentEntity> agents = player.level().getEntitiesOfClass(
//                AbstractAgentEntity.class, scanBox, agent -> agent.isBoss(player)
//            );
//            for (AbstractAgentEntity agent : agents) {
//              agent.setFollowEntity(player.getUUID());
//            }
//            break;
//          }
//          case 1: {
//            cap.cycleTargetMode();
//            player.sendSystemMessage(Component.literal("All nearby agent set to " +
//                switch (cap.getTargetMode()) {
//                  case OFF -> "relax";
//                  case HOSTILE_ONLY -> "hunt hostiles";
//                  case ENEMY_AGENTS -> "hunt enemy agents";
//                  case ALL -> "hunt all dangers";
//                }
//            ));
//            // find all agent under control
//            AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
//
//            List<AbstractAgentEntity> agents = player.level().getEntitiesOfClass(
//                AbstractAgentEntity.class, scanBox, agent -> agent.isBoss(player)
//            );
//            for (AbstractAgentEntity agent : agents) {
//              agent.setTargetMode(cap.getTargetMode());
//            }
//            break;
//          }
//          case 2: {
//            cap.toggleSpecialMode();
//            player.sendSystemMessage(Component.literal("All nearby agents set to " +
//                (cap.isSpecialMode() ? "use special" : "don't use special")
//                ));
//            // find all agent under control
//            AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
//
//            List<AbstractAgentEntity> agents = player.level().getEntitiesOfClass(
//                AbstractAgentEntity.class, scanBox, agent -> agent.isBoss(player)
//            );
//            for (AbstractAgentEntity agent : agents) {
//              agent.setAllowSpecial(cap.isSpecialMode());
//            }
//            break;
//          }
//        }
//      });
//    });
//    ctx.get().setPacketHandled(true);
//  }
//}
