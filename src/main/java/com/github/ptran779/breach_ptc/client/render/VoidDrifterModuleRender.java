package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.animation.KSeedCoreAnimation;
import com.github.ptran779.breach_ptc.client.model.HellpodModel;
import com.github.ptran779.breach_ptc.client.model.KSeedCoreModel;
import com.github.ptran779.breach_ptc.client.model.VoidDrifterModuleModel;
import com.github.ptran779.breach_ptc.entity.extra.VoidDrifterModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.swing.*;

@OnlyIn(Dist.CLIENT)
public class VoidDrifterModuleRender extends EntityRenderer<VoidDrifterModule> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(BreachPtc.MOD_ID, "textures/entities" +
	  "/void_drifter_module.png");
	private static final ResourceLocation TEXTURE1 = new ResourceLocation(BreachPtc.MOD_ID, "textures/block/k_seed_core" +
		".png");
  private final VoidDrifterModuleModel modelUnit;
	public static final int[] ROTATIONS = {0, 90, 180, 270};

  public VoidDrifterModuleRender(EntityRendererProvider.Context pContext) {
    super(pContext);
		modelUnit = ShareModel.prepVoidDrifterModuleModel(pContext.bakeLayer(VoidDrifterModuleModel.LAYER_LOCATION));
  }

	public void render(VoidDrifterModule pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
		Vec3 motion = pEntity.getDeltaMovement();
		float xRot = (float)(Math.toDegrees(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
		float yRot = (float)(Math.toDegrees(Math.atan2(-motion.x, motion.z)));

		pPoseStack.pushPose();
		pPoseStack.translate(0.0D, 1.5D, 0.0D); // Adjust Y to match model origin
		pPoseStack.mulPose(Axis.XP.rotationDegrees(180));
		pPoseStack.mulPose(Axis.YP.rotationDegrees(180));

		for (int i =0; i< 4; i++) {
			pPoseStack.pushPose();
			pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
			pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot + 90));
			pPoseStack.mulPose(Axis.YP.rotationDegrees(ROTATIONS[i]));

			VertexConsumer vertexConsumer1 = pBuffer.getBuffer(modelUnit.renderType(TEXTURE));
			modelUnit.renderToBuffer(pPoseStack, vertexConsumer1, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F,
				1.0F);
			pPoseStack.popPose();
		}

		pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot + 90));
		pPoseStack.translate(0.0D, -2.5D, 0.0D);

//		AnimationHelper.animate(model, KSeedCoreAnimation.FLAP_DEPLOY, 0, 1, false);
//		VertexConsumer vertexConsumer = pBuffer.getBuffer(model.renderType(TEXTURE1));
//		model.renderToBuffer(pPoseStack, vertexConsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		pPoseStack.popPose();
  }

  public ResourceLocation getTextureLocation(VoidDrifterModule entity) {return TEXTURE;}
}
