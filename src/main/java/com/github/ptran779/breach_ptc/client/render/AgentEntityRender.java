package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.animation.AnimationID;
import com.github.ptran779.breach_ptc.client.animation.AnimationLibrary;
import com.github.ptran779.breach_ptc.client.model.AgentModel;
import com.github.ptran779.breach_ptc.entity.agent.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AgentEntityRender extends HumanoidMobRenderer<AbsAgentEntity, AgentModel> {
  private final AgentModel standardModel;
  private final AgentModel slimModel;

  public AgentEntityRender(Context context) {
    super(context, new AgentModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.25f);
    this.standardModel = this.model;
    this.slimModel = new AgentModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

    this.addLayer(new HumanoidArmorLayer<>(this,
        new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
        new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
        context.getModelManager()
    ));
  }

  public ResourceLocation getTextureLocation(AbsAgentEntity agent) {
    return agent.getResolvedSkin();
  }

  @Override
  public void render(AbsAgentEntity agent, float pEntityYaw, float partialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
    this.model = agent.getFemale() ? slimModel : standardModel;
    // pose reset
    for (ModelPart part : model.BONE_PARTS.values()) {part.resetPose();}

    //attack ani
    float aniTime = (agent.tickCount - agent.renderTimeTrigger + partialTicks)/20f;

    // based animation
    if (agent.getAniMoveStateChange()){
      AnimationHelper.animateHumanoidToPose(model,
          AnimationLibrary.get(agent.getAniMovePoseStart()),
          agent.getAniMoveTimeStart(),
          AnimationLibrary.get(agent.getAniMovePoseEnd()),
          agent.getAniMoveTimeEnd(),
          aniTime, agent.getAniMoveTimeTran(),
          model.BONE_PARTS);
    } else {
      if (agent.getAniMovePoseStart() == AnimationID.A_LIVING){
        // speed dictation
        double speed = agent.getDeltaMovement().horizontalDistanceSqr(); // X² + Z²
        if (speed > 0.015) {AnimationHelper.animateHumanoid(model,AnimationLibrary.get(AnimationID.A_RUN), model.BONE_PARTS,aniTime,1,true);
        } else if (speed > 0.001) {
          AnimationHelper.animateHumanoid(model,AnimationLibrary.get(AnimationID.A_WALK), model.BONE_PARTS,aniTime, 1, true);
        } else if (Math.abs(Mth.wrapDegrees(agent.yBodyRot - agent.yBodyRotO)) > 0.0001f) {
	        AnimationHelper.animateHumanoid(model, AnimationLibrary.get(AnimationID.A_ROTATE), model.BONE_PARTS, aniTime, 1, true);
        } else {
	        AnimationHelper.animateHumanoid(model, AnimationLibrary.get(AnimationID.A_IDLE), model.BONE_PARTS, aniTime, 1, true);
        }
        // gun holding
        if (agent.getMainHandItem().getItem() instanceof ModernKineticGunItem) {
          model.rightArm.yRot = model.head.yRot;
          model.leftArm.yRot = 0.7F + model.head.yRot;
          // tilt correction
          model.rightArm.xRot = (-(float) Math.PI / 2F) + model.head.xRot;
          model.leftArm.xRot = (-(float) Math.PI / 2F) + model.head.xRot;
          model.leftSleeve.copyFrom(model.leftArm);
          model.rightSleeve.copyFrom(model.rightArm);
        }
      } else {
        AnimationHelper.animateHumanoid(model,
            AnimationLibrary.get(agent.getAniMovePoseStart()),
            model.BONE_PARTS,
            aniTime,
            1,
            false
        );
      }
    }

    // render head looking
    float headYaw = Mth.rotLerp(partialTicks, agent.yHeadRotO, agent.yHeadRot) - agent.yBodyRot;
    float headPitch = Mth.lerp(partialTicks, agent.xRotO, agent.getXRot());
    model.head.yRot += headYaw * (Mth.PI / 180F);
    model.head.xRot += headPitch * (Mth.PI / 180F);
    model.hat.copyFrom(model.head);

    super.render(agent, pEntityYaw, partialTicks, pPoseStack, pBuffer, pPackedLight);
  }
}



