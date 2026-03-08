package com.github.ptran779.breach_ptc.client.layer;

import com.github.ptran779.breach_ptc.client.model.AgentModel;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static com.github.ptran779.breach_ptc.client.animation.AnimationID.A_RELOAD;
import static com.github.ptran779.breach_ptc.client.animation.AnimationID.A_STATION_RELOAD;

public class AgentCosmeticLayer extends RenderLayer<AbsAgentEntity, AgentModel> {
	private final ItemRenderer itemRenderer;

	public AgentCosmeticLayer(RenderLayerParent<AbsAgentEntity, AgentModel> parent, ItemRenderer itemRenderer) {
		super(parent);
		this.itemRenderer = itemRenderer;
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, AbsAgentEntity agent, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		// chipper
		ItemStack chip = agent.getChipBrainStack();
		if (!chip.isEmpty()) {
			pose.pushPose();
			this.getParentModel().head.translateAndRotate(pose);
			pose.translate(-0.34D, -0.12D, -0.12D);
			pose.mulPose(Axis.ZP.rotationDegrees(180f));
			pose.mulPose(Axis.YP.rotationDegrees(90f));
			pose.scale(0.15F, 0.15F, 0.15F);

			this.itemRenderer.renderStatic(chip, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, agent.level(), agent.getId());
			pose.popPose();
		}
		// melee
		ItemStack melee = agent.getMeleeStack();
		if (!melee.isEmpty() && melee.getItem() != agent.getMainHandItem().getItem()){
			pose.pushPose();
			this.getParentModel().body.translateAndRotate(pose);
			pose.translate(0.35D, 0.7D, 0.0D);
			pose.mulPose(Axis.XP.rotationDegrees(90f));
			pose.mulPose(Axis.YP.rotationDegrees(90f));
			pose.mulPose(Axis.YP.rotationDegrees(10f));
			pose.scale(0.8F, 0.8F, 0.8F);

			this.itemRenderer.renderStatic(melee, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, agent.level(), agent.getId());

			pose.popPose();
		}
		// gun
		ItemStack gun = agent.getGunStack();
		if(!gun.isEmpty() && gun.getItem() != agent.getMainHandItem().getItem()){
			pose.pushPose();

			this.getParentModel().body.translateAndRotate(pose);
			pose.translate(-0.2D, 0.40D, 0.3D);
			pose.mulPose(Axis.ZP.rotationDegrees(-110f));  // The "Tactical Slant"
			pose.mulPose(Axis.YP.rotationDegrees(90f));  // Face the right way
			pose.mulPose(Axis.XP.rotationDegrees(10f)); // Flip upright
			pose.scale(0.7F, 0.7F, 0.7F);

			this.itemRenderer.renderStatic(gun, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, agent.level(), agent.getId());
			pose.popPose();
		}
		// reload
		float aniTime = (agent.tickCount - agent.renderTimeTrigger + partialTicks) / 20f;
		if ((agent.getAniMovePoseStart() == A_RELOAD && aniTime >= 0.5 && aniTime <= 1) ||
			agent.getAniMovePoseStart() == A_STATION_RELOAD && (
				aniTime >= 0.75 && aniTime < 2.5 ||
				aniTime >= 2.75 && aniTime < 3 ||
				aniTime >= 3.25 && aniTime < 3.5)
		) {
			// 1. ANCHOR TO THE ARM
			// This moves the pivot to the shoulder/arm joint
			this.getParentModel().leftArm.translateAndRotate(pose);
			pose.translate(-0.4, 0, -0.75);
			pose.pushPose();
			pose.translate(0.5,0.5,0.5);
			pose.mulPose(Axis.XP.rotationDegrees(-90f));
			pose.scale(0.5F, 0.5F, 0.5F);
			pose.translate(-0.5,-0.5,-0.5);

			var model = Minecraft.getInstance().getModelManager().
				getModel(com.github.ptran779.breach_ptc.client.particle.MagazineParticle.MODEL_LOCATION);
			var vertexConsumer = buffer.getBuffer(net.minecraft.client.renderer.RenderType.solid());

			Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
				pose.last(),
				vertexConsumer,
				null, // No block state needed for raw JSON models
				model,
				1.0F, 1.0F, 1.0F, // RGB (White = no tint)
				packedLight,
				OverlayTexture.NO_OVERLAY
			);

			pose.popPose();
		}
	}
}