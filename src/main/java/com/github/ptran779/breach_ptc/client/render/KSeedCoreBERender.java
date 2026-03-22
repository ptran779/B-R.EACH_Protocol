package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.block_entity.KSeedCoreBE;
import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.animation.AgentSpecialAnimation;
import com.github.ptran779.breach_ptc.client.animation.KSeedCoreAnimation;
import com.github.ptran779.breach_ptc.client.model.AgentModel;
import com.github.ptran779.breach_ptc.client.model.KSeedCoreModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KSeedCoreBERender implements BlockEntityRenderer<KSeedCoreBE> {
	private static final ResourceLocation TEXTURE =
		new ResourceLocation(BreachPtc.MOD_ID, "textures/block/k_seed_core.png");
	private final KSeedCoreModel model;
	private AgentModel amodel;
	private final AgentModel aThickModel, aSlimModel;

	public KSeedCoreBERender(BlockEntityRendererProvider.Context context) {
		this.model = ShareModel.prepKSeedCoreModel(context.bakeLayer(KSeedCoreModel.LAYER_LOCATION));
		this.aThickModel = ShareModel.prepAgentThickModel(context.bakeLayer(ModelLayers.PLAYER));
		this.aSlimModel = ShareModel.prepAgentSlimModel(context.bakeLayer(ModelLayers.PLAYER_SLIM));
		amodel = null;
	}

	public void render(KSeedCoreBE bEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
	                   int packedLight, int packedOverlay) {
		float aniTime = (bEntity.tickcount + partialTick) / 20f;
		// 1. RENDER THE AGENT FIRST (This fixes the glass blocking him)
		if (bEntity.getHasRider() || bEntity.getSpawnRandom()){
			VertexConsumer builder2 = buffer.getBuffer(RenderType.entityTranslucent(bEntity.getCryoSkin()));
			poseStack.pushPose();
			poseStack.translate(0.5D, 1.5D, 0.5D);
			poseStack.scale(0.9375F, -0.9375F, -0.9375F);
			amodel = bEntity.getFemale() ? aSlimModel : aThickModel;
			amodel.getRoot().getAllParts().forEach(ModelPart::resetPose);
			AnimationHelper.animateHumanoid(amodel, AgentSpecialAnimation.EXIST_CRYO, amodel.BONE_PARTS, aniTime, 1, false);
			amodel.renderToBuffer(poseStack, builder2, packedLight, packedOverlay, 1, 1, 1, 1);
			poseStack.popPose();
		}

		// 2. RENDER THE POD SECOND (The glass will now correctly wrap around the agent)
		VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
		poseStack.pushPose();
		poseStack.translate(0.5D, 1.5D, 0.5D);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		AnimationHelper.animate(model, KSeedCoreAnimation.POD_DEPLOY, aniTime, 1, false);
		model.renderToBuffer(poseStack, builder, packedLight, packedOverlay, 1, 1, 1, 1);
		poseStack.popPose();
	}
}
