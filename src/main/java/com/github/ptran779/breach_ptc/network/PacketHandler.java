package com.github.ptran779.breach_ptc.network;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.network.advConfScr.*;
import com.github.ptran779.breach_ptc.network.ml_packet.*;
import com.github.ptran779.breach_ptc.network.agent.*;
import com.github.ptran779.breach_ptc.network.player.CameraModePacket;
//import com.github.ptran779.aegisops.network.player.KeyBindPacket;
import com.github.ptran779.breach_ptc.network.player.serverConfigPacket;
import com.github.ptran779.breach_ptc.network.render.EntityRenderPacket;
import com.github.ptran779.breach_ptc.network.render.StructureRenderPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
  private static final String PROTOCOL_VERSION = "1";
  public static final SimpleChannel CHANNELS = NetworkRegistry.newSimpleChannel(new ResourceLocation(BreachPtc.MOD_ID, "main"), ()->PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

  public static void register() {
    int id = 0;
	  CHANNELS.registerMessage(id++, AgentInventoryPacket.class, AgentInventoryPacket::encode, AgentInventoryPacket::decode, AgentInventoryPacket::handle);
	  CHANNELS.registerMessage(id++, AgentAdvanceConfigPacket.class, AgentAdvanceConfigPacket::encode, AgentAdvanceConfigPacket::decode, AgentAdvanceConfigPacket::handle);

		CHANNELS.registerMessage(id++, AgentDismissPacket.class, AgentDismissPacket::encode, AgentDismissPacket::decode, AgentDismissPacket::handle);
	  CHANNELS.registerMessage(id++, AgentFollowTargetPacket.class, AgentFollowTargetPacket::encode, AgentFollowTargetPacket::decode, AgentFollowTargetPacket::handle);
	  CHANNELS.registerMessage(id++, AgentConFlg1Packet.class, AgentConFlg1Packet::encode, AgentConFlg1Packet::decode, AgentConFlg1Packet::handle);

	  CHANNELS.registerMessage(id++, ChangeSkinPacket.class, ChangeSkinPacket::encode, ChangeSkinPacket::decode, ChangeSkinPacket::handle);
    CHANNELS.registerMessage(id++, AgentBrainChipPacket.class, AgentBrainChipPacket::encode, AgentBrainChipPacket::decode, AgentBrainChipPacket::handle);

    CHANNELS.registerMessage(id++, CameraModePacket.class, CameraModePacket::encode, CameraModePacket::decode, CameraModePacket::handle);
    CHANNELS.registerMessage(id++, EntityRenderPacket.class, EntityRenderPacket::encode, EntityRenderPacket::decode, EntityRenderPacket::handle);
    CHANNELS.registerMessage(id++, StructureRenderPacket.class, StructureRenderPacket::encode, StructureRenderPacket::decode, StructureRenderPacket::handle);
    // for ML purpose
    CHANNELS.registerMessage(id++, BrainChipScreen.class, BrainChipScreen::encode, BrainChipScreen::decode, BrainChipScreen::handle);
    CHANNELS.registerMessage(id++, CreateNewBrain.class, CreateNewBrain::encode, CreateNewBrain::decode, CreateNewBrain::handle);
    CHANNELS.registerMessage(id++, UpdateBrainConfig.class, UpdateBrainConfig::encode, UpdateBrainConfig::decode, UpdateBrainConfig::handle);
    CHANNELS.registerMessage(id++, GetTrainDataList.class, GetTrainDataList::encode, GetTrainDataList::decode, GetTrainDataList::handle);
    CHANNELS.registerMessage(id++, TurnOnTrainMode.class, TurnOnTrainMode::encode, TurnOnTrainMode::decode, TurnOnTrainMode::handle);
    CHANNELS.registerMessage(id++, IOTrainData.class, IOTrainData::encode, IOTrainData::decode, IOTrainData::handle);
	  CHANNELS.registerMessage(id++, UpdateTrainDataSize.class, UpdateTrainDataSize::encode, UpdateTrainDataSize::decode, UpdateTrainDataSize::handle);
	  CHANNELS.registerMessage(id++, ClearTrainData.class, ClearTrainData::encode, ClearTrainData::decode, ClearTrainData::handle);
    CHANNELS.registerMessage(id++, PushDatLog.class, PushDatLog::encode, PushDatLog::decode, PushDatLog::handle);
    CHANNELS.registerMessage(id++, PrepDatForTrain.class, PrepDatForTrain::encode, PrepDatForTrain::decode, PrepDatForTrain::handle);
    CHANNELS.registerMessage(id++, TrainBrainChip.class, TrainBrainChip::encode, TrainBrainChip::decode, TrainBrainChip::handle);
    CHANNELS.registerMessage(id++, TrainDone.class, TrainDone::encode, TrainDone::decode, TrainDone::handle);
    CHANNELS.registerMessage(id++, CommitTrainModel.class, CommitTrainModel::encode, CommitTrainModel::decode, CommitTrainModel::handle);

		// advance Config Screen
	  CHANNELS.registerMessage(id++, GetInputSenStream.class, GetInputSenStream::encode, GetInputSenStream::decode, GetInputSenStream::handle);
	  CHANNELS.registerMessage(id++, SetInputSenStream.class, SetInputSenStream::encode, SetInputSenStream::decode, SetInputSenStream::handle);
	  CHANNELS.registerMessage(id++, GetScoreStream.class, GetScoreStream::encode, GetScoreStream::decode, GetScoreStream::handle);
	  CHANNELS.registerMessage(id++, SetScoreStream.class, SetScoreStream::encode, SetScoreStream::decode, SetScoreStream::handle);
	  CHANNELS.registerMessage(id++, GetATrainConf.class, GetATrainConf::encode, GetATrainConf::decode, GetATrainConf::handle);
	  CHANNELS.registerMessage(id++, SetATrainConf.class, SetATrainConf::encode, SetATrainConf::decode, SetATrainConf::handle);

		// key bind
//		CHANNELS.registerMessage(id++, KeyBindPacket.class, KeyBindPacket::encode, KeyBindPacket::decode, KeyBindPacket::handle);

		// Agent config
		CHANNELS.registerMessage(id++, serverConfigPacket.class, serverConfigPacket::encode, serverConfigPacket::decode, serverConfigPacket::handle);

		CHANNELS.registerMessage(id++, ReloadBrain.class, ReloadBrain::encode, ReloadBrain::decode, ReloadBrain::handle);
  }
}
