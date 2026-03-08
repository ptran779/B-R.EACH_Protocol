package com.github.ptran779.breach_ptc.client.particle;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class MagazineParticle extends Particle {
	public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(BreachPtc.MOD_ID, "particle/magazine");

	// Rotation tracking variables
	private float rotX, rotY, rotZ;
	private float oRotX, oRotY, oRotZ; // "Old" rotations for smooth frame interpolation
	private final float spinSpeedX, spinSpeedY, spinSpeedZ;

	public MagazineParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
		super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);

		// 1. Setup Physics
		this.lifetime = 400;
		this.hasPhysics = true;
		this.gravity = 1.0F;

		// 2. Setup Random Spin Speeds
		// Gives each magazine a unique, chaotic tumble when ejected
		this.spinSpeedX = (this.random.nextFloat() - 0.5F) * 40F;
		this.spinSpeedY = (this.random.nextFloat() - 0.5F) * 40F;
		this.spinSpeedZ = (this.random.nextFloat() - 0.5F) * 40F;

		// Initial random rotation so they don't all spawn facing the exact same way
		this.rotX = this.random.nextFloat() * 360F;
		this.rotY = this.random.nextFloat() * 360F;
		this.rotZ = this.random.nextFloat() * 360F;
	}

	@Override
	public void tick() {
		// Save previous rotation for smooth rendering
		this.oRotX = this.rotX;
		this.oRotY = this.rotY;
		this.oRotZ = this.rotZ;

		// Base tick handles movement, gravity, and updating 'this.onGround'
		super.tick();

		// 3. Logic: Only spin if we are NOT on the ground
		if (!this.onGround) {
			this.rotX += spinSpeedX;
			this.rotY += spinSpeedY;
			this.rotZ += spinSpeedZ;
		} else {
			// Optional: Add friction so it stops sliding quickly once it hits the floor
			this.xd *= 0.75D;
			this.zd *= 0.75D;
		}
	}

	@Override
	public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL_LOCATION);

		Vec3 cameraPos = pRenderInfo.getPosition();
		float x = (float) (Mth.lerp(pPartialTicks, this.xo, this.x) - cameraPos.x());
		float y = (float) (Mth.lerp(pPartialTicks, this.yo, this.y) - cameraPos.y());
		float z = (float) (Mth.lerp(pPartialTicks, this.zo, this.z) - cameraPos.z());

		// Interpolate rotation for smooth 60+ FPS visuals
		float lerpedRotX = Mth.lerp(pPartialTicks, this.oRotX, this.rotX);
		float lerpedRotY = Mth.lerp(pPartialTicks, this.oRotY, this.rotY);
		float lerpedRotZ = Mth.lerp(pPartialTicks, this.oRotZ, this.rotZ);

		PoseStack poseStack = new PoseStack();
		// Move to the particle's physical location
		poseStack.translate(x, y-0.45, z);
		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		// rotate at center?
		poseStack.mulPose(Axis.XP.rotationDegrees(lerpedRotX));
		poseStack.mulPose(Axis.YP.rotationDegrees(lerpedRotY));
		poseStack.mulPose(Axis.ZP.rotationDegrees(lerpedRotZ));
		poseStack.scale(0.5F, 0.5F, 0.5F);

		poseStack.translate(-0.5, -0.5, -0.5);

		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
			poseStack.last(),
			pBuffer,
			null,
			model,
			1.0F, 1.0F, 1.0F,
			this.getLightColor(pPartialTicks),
			OverlayTexture.NO_OVERLAY
		);

		poseStack.popPose();
	}

	@Override
	public @NotNull ParticleRenderType getRenderType() {
		return ParticleRenderType.TERRAIN_SHEET;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
			return new MagazineParticle(level, x, y, z, dx, dy, dz);
		}
	}
}