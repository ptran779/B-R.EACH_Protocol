package com.github.ptran779.breach_ptc.network.ml_packet;

import com.github.ptran779.breach_ptc.client.screens.BrainChipScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

//S->C
public class PushDatLog {
  String log;
  public PushDatLog(String log) {
    this.log = log;
  }
  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(log);
  }
  public static PushDatLog decode(FriendlyByteBuf buf) {
    return new PushDatLog(buf.readUtf());
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handlePacket(log)));
    ctx.get().setPacketHandled(true);
  }
  private static class ClientHandler {
    public static void handlePacket(String str) {
      // Check if the player is actually looking at the screen
      if (net.minecraft.client.Minecraft.getInstance().screen instanceof BrainChipScreen screen) {
        screen.addLog(str);
      }
    }
  }
}
