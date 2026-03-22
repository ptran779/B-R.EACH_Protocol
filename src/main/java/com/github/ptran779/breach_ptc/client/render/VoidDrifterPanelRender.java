package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.model.VoidDrifterModuleModel;
import com.github.ptran779.breach_ptc.entity.extra.VoidDrifterPanel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VoidDrifterPanelRender extends EntityRenderer<VoidDrifterPanel> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(BreachPtc.MOD_ID, "textures/entities" +
	  "/void_drifter_module.png");
  private final VoidDrifterModuleModel modelUnit;

  public VoidDrifterPanelRender(EntityRendererProvider.Context pContext) {
    super(pContext);
		modelUnit = ShareModel.prepVoidDrifterModuleModel(pContext.bakeLayer(VoidDrifterModuleModel.LAYER_LOCATION));
  }

	public void render(VoidDrifterPanel pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
		Vec3 motion = pEntity.getDeltaMovement();
		float xRot = (float)(Math.toDegrees(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
		float yRot = (float)(Math.toDegrees(Math.atan2(-motion.x, motion.z)));

		// Tumble spin — interpolated for smoothness
		float spinX = Mth.lerp(pPartialTick, pEntity.prevSpinX, pEntity.spinX);
		float spinY = Mth.lerp(pPartialTick, pEntity.prevSpinY, pEntity.spinY);
		float spinZ = Mth.lerp(pPartialTick, pEntity.prevSpinZ, pEntity.spinZ);

		pPoseStack.pushPose();

		pPoseStack.translate(0.0D, 1.5D, 0.0D);
		pPoseStack.mulPose(Axis.XP.rotationDegrees(180));
		pPoseStack.mulPose(Axis.YP.rotationDegrees(180));

		// Travel direction base
//		pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
//		pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot - 90));

		// Tumble on top
		pPoseStack.mulPose(Axis.XP.rotationDegrees(spinX));
		pPoseStack.mulPose(Axis.YP.rotationDegrees(spinY));
		pPoseStack.mulPose(Axis.ZP.rotationDegrees(spinZ));

		VertexConsumer vertexConsumer = pBuffer.getBuffer(modelUnit.renderType(TEXTURE));
		modelUnit.renderToBuffer(pPoseStack, vertexConsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

		pPoseStack.popPose();
	}

  public ResourceLocation getTextureLocation(VoidDrifterPanel entity) {return TEXTURE;}
}
