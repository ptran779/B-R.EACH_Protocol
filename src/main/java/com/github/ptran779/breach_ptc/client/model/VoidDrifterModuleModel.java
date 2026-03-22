package com.github.ptran779.breach_ptc.client.model;// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.github.ptran779.breach_ptc.BreachPtc;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class VoidDrifterModuleModel extends AbstractAniModel {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation
		LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(BreachPtc.MOD_ID, "void_drifter_module_layer"), "main");

	public VoidDrifterModuleModel(ModelPart root) {
		super(RenderType::entitySolid, root.getChild("Main"));
		put("Main", root.getChild("Main"));
		put("Core", get("Main").getChild("Core"));
		put("Thrust", get("Main").getChild("Thrust"));
		put("RadialFrame", get("Main").getChild("RadialFrame"));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main = partdefinition.addOrReplaceChild("Main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Core = Main.addOrReplaceChild("Core", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = Core.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(48, 20).addBox(-6.0F, -62.0F, 18.0F, 12.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
			.texOffs(48, 0).addBox(-10.0F, -62.0F, 14.0F, 20.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r2 = Core.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 79).addBox(-5.0F, -57.0F, 17.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
			.texOffs(48, 34).addBox(-2.0F, -56.0F, 4.0F, 4.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
			.texOffs(0, 42).addBox(-10.0F, -57.0F, 13.0F, 20.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.0436F, 0.7854F, 0.0F));

		PartDefinition cube_r3 = Core.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(32, 85).addBox(-5.0F, -52.0F, 16.0F, 10.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
			.texOffs(0, 32).addBox(-10.0F, -52.0F, 12.0F, 20.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.0873F, 0.7854F, 0.0F));

		PartDefinition cube_r4 = Core.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(108, 59).addBox(-11.0F, -46.0F, 11.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(108, 51).addBox(9.0F, -46.0F, 11.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(96, 79).addBox(-3.0F, -46.0F, 18.0F, 6.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(96, 0).addBox(-4.0F, -47.0F, 15.0F, 8.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
			.texOffs(48, 10).addBox(-9.0F, -47.0F, 11.0F, 18.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.1309F, 0.7854F, 0.0F));

		PartDefinition cube_r5 = Core.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(90, 94).addBox(-4.0F, -42.0F, 12.0F, 8.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
			.texOffs(96, 69).addBox(-2.0F, -41.0F, 2.0F, 4.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
			.texOffs(0, 52).addBox(-8.0F, -42.0F, 8.0F, 16.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(48, 100).addBox(-3.0F, -36.0F, 11.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
			.texOffs(72, 52).addBox(-7.0F, -36.0F, 7.0F, 14.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.1745F, 0.7854F, 0.0F));

		PartDefinition cube_r6 = Core.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(66, 100).addBox(-3.0F, -31.0F, 9.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(82, 34).addBox(-6.0F, -31.0F, 5.0F, 12.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.2182F, 0.7854F, 0.0F));

		PartDefinition cube_r7 = Core.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(34, 106).addBox(-2.0F, -26.0F, 7.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(46, 109).addBox(-1.0F, -24.0F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(0, 91).addBox(-5.0F, -26.0F, 3.0F, 10.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.2618F, 0.7854F, 0.0F));

		PartDefinition cube_r8 = Core.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(22, 106).addBox(-2.0F, -21.0F, 6.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(92, 10).addBox(-4.0F, -21.0F, 2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.2618F, 0.7854F, 0.0F));

		PartDefinition cube_r9 = Core.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(74, 108).addBox(-1.0F, -16.0F, 5.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(28, 96).addBox(-3.0F, -16.0F, 1.0F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.3054F, 0.7854F, 0.0F));

		PartDefinition cube_r10 = Core.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(32, 62).addBox(-1.0F, -11.0F, 3.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(0, 101).addBox(-2.0F, -11.0F, 0.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.3491F, 0.7854F, 0.0F));

		PartDefinition cube_r11 = Core.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(14, 101).addBox(-1.0F, -15.0F, 7.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, 0.6545F, 0.7854F, 0.0F));

		PartDefinition cube_r12 = Core.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(66, 108).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 13.0F, 5.0F, -0.3927F, 0.7854F, 0.0F));

		PartDefinition Thrust = Main.addOrReplaceChild("Thrust", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r13 = Thrust.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 4.0F, 12.0F, new CubeDeformation(0.0F))
			.texOffs(0, 62).addBox(6.0F, -5.0F, -4.0F, 4.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
			.texOffs(40, 52).addBox(-10.0F, -5.0F, -4.0F, 4.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
			.texOffs(88, 20).addBox(-6.0F, -5.0F, 8.0F, 12.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(62, 85).addBox(-6.0F, -5.0F, -8.0F, 12.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.0F, -55.0F, 18.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r14 = Thrust.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(32, 69).addBox(-5.0F, -5.0F, 11.0F, 10.0F, 10.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, -55.0F, 10.0F, -0.4363F, 0.7854F, 0.0F));

		PartDefinition RadialFrame = Main.addOrReplaceChild("RadialFrame", CubeListBuilder.create().texOffs(62, 94).addBox(0.0F, -24.0F, 17.0F, 11.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r15 = RadialFrame.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(88, 29).addBox(-9.0F, -1.0F, -7.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -9.0F, 19.0F, 0.0F, -0.4363F, 0.0F));

		PartDefinition cube_r16 = RadialFrame.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(64, 69).addBox(-7.0F, -1.0F, -10.0F, 2.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(19.0F, -9.0F, 6.0F, 0.0F, 0.4363F, 0.0F));

		PartDefinition cube_r17 = RadialFrame.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(94, 85).addBox(-5.5F, -1.5F, -3.5F, 11.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.5F, -22.5F, 5.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r18 = RadialFrame.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(82, 103).addBox(-2.0F, 1.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(82, 44).addBox(-7.0F, -1.5F, -2.0F, 13.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.0F, -38.5F, 6.0F, 0.0F, -1.9635F, 0.0F));

		PartDefinition cube_r19 = RadialFrame.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(98, 103).addBox(-2.0F, 1.5F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(72, 62).addBox(-7.0F, -1.5F, -1.0F, 13.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -38.5F, 23.0F, 0.0F, 0.3927F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}
}