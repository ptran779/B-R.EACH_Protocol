package com.github.ptran779.breach_ptc.client.model;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
@OnlyIn(Dist.CLIENT)
public class AgentModel extends PlayerModel<AbsAgentEntity>{
  public final Map<String, ModelPart> BONE_PARTS = new HashMap<>();

  public AgentModel(ModelPart pRoot, boolean pSlim) {
    super(pRoot, pSlim);
    // code in the map for lookup:
    BONE_PARTS.put("Head", this.head);
    BONE_PARTS.put("Body", this.body);
    BONE_PARTS.put("LeftArm", this.leftArm);
    BONE_PARTS.put("RightArm", this.rightArm);
    BONE_PARTS.put("LeftLeg", this.leftLeg);
    BONE_PARTS.put("RightLeg", this.rightLeg);
  }

  public void setupAnim(AbsAgentEntity agent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}
}
