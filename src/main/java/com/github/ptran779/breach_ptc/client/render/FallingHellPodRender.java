package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.animation.HellPodAnimation;
import com.github.ptran779.breach_ptc.client.model.HellpodModel;
import com.github.ptran779.breach_ptc.entity.extra.FallingHellPod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingHellPodRender extends EntityRenderer<FallingHellPod> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(BreachPtc.MOD_ID, "textures/block/hell_pod.png");
  private final HellpodModel model;

  public FallingHellPodRender(EntityRendererProvider.Context pContext) {
    super(pContext);
    this.model = ShareModel.prepHellPodModel(pContext.bakeLayer(HellpodModel.LAYER_LOCATION));
  }

  public void render(FallingHellPod pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
    pPoseStack.pushPose();
    // Positioning the model at the entity’s current coordinates
    pPoseStack.translate(0.0D, 2.5D, 0.0D); // Adjust Y to match model origin
    pPoseStack.scale(-1.0F, -1.0F, 1.0F); // Flip model upright (Forge quirk)

    if (pEntity.isDeployed()) {
      float aniTick = (pEntity.tickCount - pEntity.timeTrigger + pPartialTick);
      AnimationHelper.animate(model, HellPodAnimation.DEPLOY1, aniTick / 20, 1, false);
    } else {
      AnimationHelper.animate(model, HellPodAnimation.DEPLOY1, 0, 1, false);
      float spin = (pEntity.tickCount + pPartialTick) * 5F;
      pPoseStack.mulPose(Axis.YP.rotationDegrees(spin));
    }

    VertexConsumer vertexConsumer = pBuffer.getBuffer(model.renderType(TEXTURE));
    model.renderToBuffer(pPoseStack, vertexConsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

    pPoseStack.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(FallingHellPod entity) {return TEXTURE;}
}
