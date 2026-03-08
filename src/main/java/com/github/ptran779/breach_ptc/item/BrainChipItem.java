package com.github.ptran779.breach_ptc.item;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.email.ML;
import com.github.ptran779.breach_ptc.config.MlModelManager;
import com.github.ptran779.breach_ptc.network.agent.BrainChipScreen;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.ml_packet.GetTrainDataList;
import com.github.ptran779.breach_ptc.network.ml_packet.UpdateTrainDataSize;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static com.github.ptran779.breach_ptc.config.MlModelManager.getAvailableCsvs;

public class BrainChipItem extends Item {
  public BrainChipItem(Properties pProperties) {
    super(pProperties);
  }

  // generate chipUUID tag if no tag exist -- this is used to identify the model
  public UUID getOrCreateUUID(ItemStack stack) {
    CompoundTag tag = stack.getOrCreateTag(); // creates NBT if missing
    if (!tag.contains("chipUUID")) {
      UUID uuid = UUID.randomUUID();
      tag.putUUID("chipUUID", uuid);  // vanilla helper for UUIDs
      return uuid;
    } else {
      return tag.getUUID("chipUUID");
    }
  }
	// tag for last user
	public void linkAgent(ItemStack stack, LivingEntity agent) {
		// Every entity has a UUID. Period. Just grab it.
		stack.getOrCreateTag().putUUID("linkedAgent", agent.getUUID());
	}
	@Nullable
	public UUID getLinkedAgent(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return (tag != null && tag.contains("linkedAgent")) ? tag.getUUID("linkedAgent") : null;
	}

  public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity livingEntity, InteractionHand hand) {
    if (player.isShiftKeyDown()) {
      if (!player.level().isClientSide) {
        if (livingEntity instanceof AbsAgentEntity agent) {
          UUID modelUUID = getOrCreateUUID(stack);  //get UUID item tag
          MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(modelUUID, player.level().getGameTime());

          // if mUnit already has IOsize, ignore. also, IO size of either 0 sound wrong bth
          if (mUnit.inSize == 0 || mUnit.outSize == 0) {
            mUnit.inSize = agent.getSensorSize();
            mUnit.outSize = agent.getBehaviorSize();
            player.displayClientMessage(Component.literal("chip: " + modelUUID + " bound IO to " + agent.getAgentType()
                + "class"), false);
						// link it
	          linkAgent(stack, agent);
          } else {
            player.displayClientMessage(Component.literal("chip: " + modelUUID + " already has IO bound at In:"
                    + mUnit.inSize + " Out:" + mUnit.outSize),false);
          }
        }
      }
    }
    return InteractionResult.PASS;
  }

  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (!level.isClientSide) {
      UUID modelUUID = getOrCreateUUID(stack);  //get UUID item tag
      MlModelManager.MLUnit mUnit = MlModelManager.getMUnit(modelUUID, level.getGameTime());
      //request to send data and byte chain of the rest of the model
      byte[] rawModel = mUnit.model==null ? new byte[0]: mUnit.model.modelSimpleSerialize();
      byte[] rawConfig = ML.trainConfigSerialize(mUnit.model);
      boolean trainMode = false;
      int trainDatLen = 0;
      if(mUnit.dataManager != null) {
        trainMode = true;
        trainDatLen = mUnit.dataManager.getRawDat().size();
      }

	    UUID agentUUID = getLinkedAgent(stack);
			int agentID = -1;
			if (agentUUID != null) {
				Entity agent = ((ServerLevel) level).getEntity(agentUUID);
				if (!(agent == null)){agentID = agent.getId();}
			}
      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
          new BrainChipScreen(agentID, modelUUID, mUnit.inSize, mUnit.outSize, trainMode, rawConfig, rawModel));
      List<String> messy = getAvailableCsvs();
      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
          new GetTrainDataList(messy));
      PacketHandler.CHANNELS.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
          new UpdateTrainDataSize(trainDatLen));
    }
    return InteractionResultHolder.success(stack);
  }
}
