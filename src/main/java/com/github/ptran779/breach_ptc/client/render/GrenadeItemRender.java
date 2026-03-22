package com.github.ptran779.breach_ptc.client.render;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.AnimationHelper;
import com.github.ptran779.breach_ptc.client.ShareModel;
import com.github.ptran779.breach_ptc.client.animation.GrenadeAnimation;
import com.github.ptran779.breach_ptc.client.model.GrenadeModel;
import com.github.ptran779.breach_ptc.item.GrenadeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GrenadeItemRender extends BlockEntityWithoutLevelRenderer {
  protected GrenadeModel model;
	public static final ResourceLocation FRAG_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_frag.png");
	public static final ResourceLocation EMP_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_emp.png");
	public static final ResourceLocation INCENDIARY_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_incendiary.png");
	public static final ResourceLocation CORROSIVE_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_corrosive.png");
	public static final ResourceLocation CRYO_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_cryo.png");
	public static final ResourceLocation FLASHBANG_TX = new ResourceLocation(BreachPtc.MOD_ID, "textures/item/grenade_flashbang.png");

  public GrenadeItemRender(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
    super(pBlockEntityRenderDispatcher, pEntityModelSet);
    this.model = ShareModel.prepGrenadeModel(pEntityModelSet.bakeLayer(GrenadeModel.LAYER_LOCATION));
  }

  public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                           MultiBufferSource buffer, int light, int overlay) {
	  if (!(stack.getItem() instanceof GrenadeItem grenadeItem)) return;  // block render cause why not
		CompoundTag tag = stack.getOrCreateTag();
    if (tag.contains("DeployTick")){
      long deployTick = stack.getOrCreateTag().getLong("DeployTick");
      long currentTick = Minecraft.getInstance().level.getGameTime();
      AnimationHelper.animate(model, GrenadeAnimation.DEPLOY, (float) (currentTick - deployTick) /20, 1, false);
    } else {
      AnimationHelper.animate(model, GrenadeAnimation.DEPLOY, 0f, 1, false);   /// FIXME ? you meant reset pose?
    }
    poseStack.pushPose();
    poseStack.translate(0.5, -1, 0.5);
    if (context == ItemDisplayContext.GUI) { // Inventory slot
      poseStack.scale(1.5f, 1.5f, 1.5f); // Scale up 1.5x
      poseStack.mulPose(Axis.XP.rotationDegrees(30));
      poseStack.mulPose(Axis.YP.rotationDegrees(45));
      poseStack.translate(0, -0.5, 0);
    }

	  ResourceLocation texture = switch (grenadeItem.getGrenadeType()) {
		  case FRAG -> FRAG_TX;
		  case INCENDIARY -> INCENDIARY_TX;
		  case EMP -> EMP_TX;
		  case CORROSIVE -> CORROSIVE_TX;
		  case CRYO -> CRYO_TX;
		  case FLASHBANG -> FLASHBANG_TX;
	  };

    VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
    model.renderToBuffer(poseStack, consumer, light, overlay, 1f, 1f, 1f, 1f);
    poseStack.popPose();
  }
}
