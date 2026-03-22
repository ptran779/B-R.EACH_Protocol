package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.animation.HellPodAnimation;
import com.github.ptran779.breach_ptc.client.animation.KSeedCoreAnimation;
import com.github.ptran779.breach_ptc.client.model.HellpodModel;
import com.github.ptran779.breach_ptc.client.model.KSeedCoreModel;
import com.github.ptran779.breach_ptc.entity.extra.FallingHellPod;
import com.github.ptran779.breach_ptc.entity.extra.KSeedCore;
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

@OnlyIn(Dist.CLIENT)
public class KSeedCoreRender extends EntityRenderer<KSeedCore> {
	private static final ResourceLocation TEXTURE =
		new ResourceLocation(BreachPtc.MOD_ID, "textures/block/k_seed_core.png");
	private final KSeedCoreModel model;

	public KSeedCoreRender(EntityRendererProvider.Context pContext) {
		super(pContext);
		this.model = ShareModel.prepKSeedCoreModel(pContext.bakeLayer(KSeedCoreModel.LAYER_LOCATION));
	}

	public void render(KSeedCore pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack,
	                   MultiBufferSource pBuffer, int pPackedLight) {
		Vec3 motion = pEntity.getDeltaMovement();
		float xRot = (float) (Math.toDegrees(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
		float yRot = (float) (Math.toDegrees(Math.atan2(-motion.x, motion.z)));

		pPoseStack.pushPose();
		pPoseStack.translate(0.0D, 1.5D, 0.0D); // Adjust Y to match model origin
		pPoseStack.mulPose(Axis.XP.rotationDegrees(180));
		pPoseStack.mulPose(Axis.YP.rotationDegrees(180));

		pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot + 90));
		pPoseStack.translate(0.0D, -2.75D, 0.0D);

		if (pEntity.getDeployedTime() < 0) {
			AnimationHelper.animate(model, KSeedCoreAnimation.FLAP_DEPLOY, 0, 1, false);
		} else {
			AnimationHelper.animate(model, KSeedCoreAnimation.FLAP_DEPLOY,
				(pEntity.tickCount - pEntity.getDeployedTime()) / 20f, 1, false);
		}

		VertexConsumer vertexConsumer = pBuffer.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(pPoseStack, vertexConsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		pPoseStack.popPose();
	}

	public ResourceLocation getTextureLocation(KSeedCore entity) {return TEXTURE;}
}
