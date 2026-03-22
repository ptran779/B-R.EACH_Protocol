package com.github.ptran779.breach_ptc.client.model;
// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.github.ptran779.breach_ptc.BreachPtc;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KSeedCoreModel extends AbstractAniModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(BreachPtc.MOD_ID,
		"k_seed_core_layer"), "main");
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor

	public KSeedCoreModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull, root.getChild("Main"));
		put("Main", root.getChild("Main"));
		put("Shell", get("Main").getChild("Shell"));
		put("Pannels", get("Shell").getChild("Pannels"));
		put("Nose", get("Shell").getChild("Nose"));
		put("BarFrame", get("Shell").getChild("BarFrame"));
		put("CryoTube", get("Main").getChild("CryoTube"));
		put("Frame", get("CryoTube").getChild("Frame"));
		put("LeftDoor", get("CryoTube").getChild("LeftDoor"));
		put("RightDoor", get("CryoTube").getChild("RightDoor"));
		put("EHatch", get("Main").getChild("EHatch"));
		put("WHatch", get("Main").getChild("WHatch"));
		put("Fins", get("Main").getChild("Fins"));
		put("n1", get("Fins").getChild("n1"));
		put("s1", get("Fins").getChild("s1"));
		put("w1", get("Fins").getChild("w1"));
		put("e1", get("Fins").getChild("e1"));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main = partdefinition.addOrReplaceChild("Main", CubeListBuilder.create(), PartPose.offset(0.0F, 50.0F, 0.0F));

		PartDefinition Shell = Main.addOrReplaceChild("Shell", CubeListBuilder.create(), PartPose.offset(0.0F, -17.0F, 11.75F));

		PartDefinition cube_r1 = Shell.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(116, 169).addBox(-5.0F, -8.0F, -1.0F, 10.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r2 = Shell.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(156, 149).addBox(0.0F, -8.0F, -5.5F, 1.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, 0.0F, -11.25F, 0.0F, 0.0F, 0.3927F));

		PartDefinition cube_r3 = Shell.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 153).addBox(-1.0F, -8.0F, -5.0F, 1.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0F, 0.0F, -11.75F, 0.0F, 0.0F, -0.3927F));

		PartDefinition cube_r4 = Shell.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(168, 139).addBox(-5.0F, -8.0F, 0.0F, 10.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -24.75F, -0.3927F, 0.0F, 0.0F));

		PartDefinition Pannels = Shell.addOrReplaceChild("Pannels", CubeListBuilder.create().texOffs(60, 55).addBox(12.0F, 0.0F, -20.75F, 1.0F, 16.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(104, 149).addBox(13.0F, 3.0F, -16.75F, 1.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(72, 0).addBox(-13.0F, 0.0F, -20.75F, 1.0F, 16.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(126, 149).addBox(-14.0F, 3.0F, -16.75F, 1.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(34, 135).addBox(-9.0F, 0.0F, -24.75F, 18.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(158, 11).addBox(-5.0F, 3.0F, -25.75F, 10.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(112, 82).addBox(-9.0F, 0.0F, 0.25F, 18.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(158, 101).addBox(-5.0F, 3.0F, 1.25F, 10.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r5 = Pannels.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(136, 117).addBox(-8.0F, -10.0F, -1.0F, 16.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r6 = Pannels.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(72, 106).addBox(-1.0F, -10.0F, -8.0F, 1.0F, 13.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.25F, 25.0F, -11.75F, 0.0F, 0.0F, -0.3927F));

		PartDefinition cube_r7 = Pannels.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(38, 106).addBox(0.0F, -10.0F, -8.0F, 1.0F, 13.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.25F, 25.0F, -11.75F, 0.0F, 0.0F, 0.3927F));

		PartDefinition cube_r8 = Pannels.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(136, 65).addBox(-8.0F, -10.0F, 0.0F, 16.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 25.0F, -3.5F, -0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r9 = Pannels.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(136, 51).addBox(-8.0F, -10.0F, -1.0F, 16.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 25.0F, -20.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r10 = Pannels.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(116, 25).addBox(0.0F, -10.0F, -8.5F, 1.0F, 10.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, 0.0F, -11.25F, 0.0F, 0.0F, 0.3927F));

		PartDefinition cube_r11 = Pannels.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 127).addBox(-1.0F, -10.0F, -8.0F, 1.0F, 10.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0F, 0.0F, -11.75F, 0.0F, 0.0F, -0.3927F));

		PartDefinition cube_r12 = Pannels.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(136, 128).addBox(-8.0F, -10.0F, 0.0F, 16.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -24.75F, -0.3927F, 0.0F, 0.0F));

		PartDefinition Nose = Shell.addOrReplaceChild("Nose", CubeListBuilder.create().texOffs(0, 170).addBox(-2.0F, 32.0F, -13.75F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(136, 139).addBox(-4.0F, 30.0F, -15.75F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(110, 11).addBox(-6.0F, 28.0F, -17.75F, 12.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 38).addBox(-8.0F, 27.0F, -19.75F, 16.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition BarFrame = Shell.addOrReplaceChild("BarFrame", CubeListBuilder.create().texOffs(54, 100).addBox(8.0F, 24.0F, -3.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(170, 59).addBox(-12.0F, -2.0F, -3.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(156, 166).addBox(-12.0F, 0.0F, -2.75F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(170, 53).addBox(-12.0F, 16.0F, -3.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(82, 173).addBox(-11.0F, -4.0F, -3.75F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(94, 173).addBox(-10.0F, -9.0F, -3.75F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(170, 129).addBox(-11.0F, 18.0F, -3.75F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(20, 178).addBox(-10.0F, 21.0F, -3.75F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(110, 25).addBox(-9.0F, 24.0F, -3.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 178).addBox(8.0F, 21.0F, -3.75F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(98, 82).addBox(8.0F, 18.0F, -3.75F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(138, 169).addBox(8.0F, -9.0F, -3.75F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(140, 101).addBox(8.0F, -4.0F, -3.75F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(170, 47).addBox(8.0F, 16.0F, -3.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(22, 153).addBox(9.0F, 0.0F, -2.75F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(38, 100).addBox(8.0F, -2.0F, -3.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(170, 71).addBox(8.0F, -2.0F, -23.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(112, 33).addBox(-9.0F, 24.0F, -20.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(178, 27).addBox(-10.0F, 21.0F, -21.75F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(70, 173).addBox(-11.0F, 18.0F, -22.75F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(128, 177).addBox(-10.0F, -9.0F, -21.75F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(116, 177).addBox(-11.0F, -4.0F, -22.75F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(170, 123).addBox(-12.0F, 16.0F, -23.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 169).addBox(-12.0F, 0.0F, -23.75F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(170, 117).addBox(-12.0F, -2.0F, -23.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(168, 166).addBox(9.0F, 0.0F, -23.75F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(170, 65).addBox(8.0F, 16.0F, -23.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 177).addBox(8.0F, -4.0F, -22.75F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(138, 176).addBox(8.0F, -9.0F, -21.75F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(16, 172).addBox(8.0F, 18.0F, -22.75F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(178, 22).addBox(8.0F, 21.0F, -21.75F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(110, 29).addBox(8.0F, 24.0F, -20.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition CryoTube = Main.addOrReplaceChild("CryoTube", CubeListBuilder.create(), PartPose.offset(0.0F, -27.0F, 0.0F));

		PartDefinition Frame = CryoTube.addOrReplaceChild("Frame", CubeListBuilder.create().texOffs(106, 117).addBox(-7.0F, -37.0F, -9.0F, 14.0F, 31.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(58, 152).addBox(-2.0F, -30.0F, -4.0F, 4.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(72, 34).addBox(-4.0F, -20.0F, -4.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(92, 34).addBox(-4.0F, -30.0F, -4.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 55).addBox(8.0F, -37.0F, -7.0F, 1.0F, 31.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(30, 55).addBox(-9.0F, -37.0F, -7.0F, 1.0F, 31.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-9.0F, -6.0F, -9.0F, 18.0F, 1.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 19).addBox(-9.0F, -38.0F, -9.0F, 18.0F, 1.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(148, 149).addBox(-9.0F, -37.0F, -9.0F, 2.0F, 31.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(34, 152).addBox(7.0F, -37.0F, -9.0F, 2.0F, 31.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(42, 152).addBox(7.0F, -37.0F, 7.0F, 2.0F, 31.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(50, 152).addBox(-9.0F, -37.0F, 7.0F, 2.0F, 31.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition LeftDoor = CryoTube.addOrReplaceChild("LeftDoor", CubeListBuilder.create().texOffs(72, 135).addBox(-0.5F, -30.5F, -0.5F, 1.0F, 31.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.5F, -0.5F, 8.5F));

		PartDefinition RightDoor = CryoTube.addOrReplaceChild("RightDoor", CubeListBuilder.create().texOffs(88, 135).addBox(-0.5F, -30.5F, -0.5F, 1.0F, 31.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(6.5F, -0.5F, 8.5F));

		PartDefinition EHatch = Main.addOrReplaceChild("EHatch", CubeListBuilder.create().texOffs(98, 55).addBox(-0.5F, -9.0F, -9.0F, 1.0F, 9.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.0F, -26.5F, 0.0F));

		PartDefinition WHatch = Main.addOrReplaceChild("WHatch", CubeListBuilder.create().texOffs(0, 100).addBox(-0.5F, -9.0F, -9.0F, 1.0F, 9.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, -26.5F, 0.0F));

		PartDefinition Fins = Main.addOrReplaceChild("Fins", CubeListBuilder.create(), PartPose.offset(0.0F, -8.0F, 0.0F));

		PartDefinition n1 = Fins.addOrReplaceChild("n1", CubeListBuilder.create().texOffs(162, 0).addBox(-5.0F, -2.0F, -5.0F, 10.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(162, 5).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(106, 106).addBox(-8.0F, -1.0F, -10.0F, 16.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, -9.0F));

		PartDefinition s1 = Fins.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(112, 99).addBox(-5.0F, 0.0F, 2.0F, 10.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(110, 0).addBox(-8.0F, -1.0F, 0.0F, 16.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(158, 112).addBox(-5.0F, -2.0F, 1.0F, 10.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 9.0F));

		PartDefinition w1 = Fins.addOrReplaceChild("w1", CubeListBuilder.create().texOffs(150, 25).addBox(2.0F, 0.0F, -5.0F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(64, 38).addBox(0.0F, -1.0F, -8.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(150, 79).addBox(1.0F, -2.0F, -5.0F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, -18.0F, 0.0F));

		PartDefinition e1 = Fins.addOrReplaceChild("e1", CubeListBuilder.create().texOffs(150, 90).addBox(-5.0F, -2.0F, -5.0F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(150, 36).addBox(-6.0F, 0.0F, -5.0F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(60, 89).addBox(-10.0F, -1.0F, -8.0F, 10.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.0F, -18.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}
}