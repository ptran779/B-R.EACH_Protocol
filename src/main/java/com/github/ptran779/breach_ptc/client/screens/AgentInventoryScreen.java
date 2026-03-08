package com.github.ptran779.breach_ptc.client.screens;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.client.widgets.ToggleButton;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventoryMenu;
import com.github.ptran779.breach_ptc.network.agent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import static com.github.ptran779.breach_ptc.network.PacketHandler.CHANNELS;

@OnlyIn(Dist.CLIENT)
public class AgentInventoryScreen extends AbstractContainerScreen<AgentInventoryMenu>{
  private static final Font font = Minecraft.getInstance().font;
  private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation(BreachPtc.MOD_ID,"textures/gui/a_inv_scr.png");
  private static final ResourceLocation BUTTON = new ResourceLocation(BreachPtc.MOD_ID,"textures/gui/a_inv_but.png");
  private final Player player;
  private final AbsAgentEntity agent;
	ToggleButton wanderBut, followBut, hostileBut, humanoidBut, specialBut, fastFireBut;
  DecimalFormat df = new DecimalFormat("#.#");

  protected void init(){
    super.init();

		// creation
	  wanderBut = new ToggleButton(this.leftPos + 227, this.topPos + 8, 31, 21,0, 0, 21, BUTTON, 186, 42,
        btn -> {
	        wanderBut.flip();
	        updateControlFlag1();
        }
    );
	  followBut = new ToggleButton(this.leftPos + 261, this.topPos + 8, 31, 21,31, 0, 21, BUTTON, 186, 42,
		  btn -> {
			  followBut.flip();
			  updateControlFlag1();
				if (followBut.stateOn){CHANNELS.sendToServer(new AgentFollowTargetPacket(agent.getId(), player.getUUID()));}
		  }
	  );
	  hostileBut = new ToggleButton(this.leftPos + 227, this.topPos + 32, 31, 21,62, 0, 21, BUTTON, 186, 42,
		  btn -> {
			  hostileBut.flip();
			  updateControlFlag1();
		  }
	  );
	  humanoidBut = new ToggleButton(this.leftPos + 261, this.topPos + 32, 31, 21,93, 0, 21, BUTTON, 186, 42,
		  btn -> {
			  humanoidBut.flip();
			  updateControlFlag1();
		  }
	  );
	  specialBut = new ToggleButton(this.leftPos + 227, this.topPos + 56, 31, 21,124, 0, 21, BUTTON, 186, 42,
		  btn -> {
			  specialBut.flip();
			  updateControlFlag1();
		  }
	  );
	  fastFireBut = new ToggleButton(this.leftPos + 261, this.topPos + 56, 31, 21,155, 0, 21, BUTTON, 186, 42,
		  btn -> {
			  fastFireBut.flip();
			  updateControlFlag1();
		  }
	  );
		// set base val
	  int flag = agent.getControlFlg1();
		wanderBut.stateOn = (flag & AbsAgentEntity.BF_WANDER) != 0;
	  followBut.stateOn = (flag & AbsAgentEntity.BF_FOLLOW) != 0;
	  hostileBut.stateOn = (flag & AbsAgentEntity.BF_TARGET_HOSTILE) != 0;
	  humanoidBut.stateOn = (flag & AbsAgentEntity.BF_TARGET_AGENT) != 0;
	  specialBut.stateOn = (flag & AbsAgentEntity.BF_ALLOW_SPECIAL) != 0;
	  fastFireBut.stateOn = (flag & AbsAgentEntity.BF_RAPID_SHOOTING) != 0;
	  // set tooltip
	  wanderBut.setTooltip(Tooltip.create(Component.literal("Toggle Wandering")));
	  followBut.setTooltip(Tooltip.create(Component.literal("Toggle Follow")));
	  hostileBut.setTooltip(Tooltip.create(Component.literal("Toggle Target Hostile")));
	  humanoidBut.setTooltip(Tooltip.create(Component.literal("Toggle Target Humanoid")));
	  specialBut.setTooltip(Tooltip.create(Component.literal("Toggle Allow Special")));
	  fastFireBut.setTooltip(Tooltip.create(Component.literal("Toggle Fast Firing")));
	  //render
		addRenderableWidget(wanderBut);
	  addRenderableWidget(followBut);
	  addRenderableWidget(hostileBut);
	  addRenderableWidget(humanoidBut);
	  addRenderableWidget(specialBut);
	  addRenderableWidget(fastFireBut);

    PlainTextButton advancedConfBut = new PlainTextButton(this.leftPos + 205, this.topPos + 37,
        18, 18,Component.empty(),
        btn -> {
          CHANNELS.sendToServer(new AgentAdvanceConfigPacket(agent.getId()));
        }, font
    );
    advancedConfBut.setTooltip(Tooltip.create(Component.literal("Advance Configuration")));
    addRenderableWidget(advancedConfBut);
	  PlainTextButton dismissBut = new PlainTextButton(this.leftPos + 227, this.topPos + 151, 65, 16,Component.empty(),
		  btn -> {
			  CHANNELS.sendToServer(new AgentDismissPacket(agent.getId()));
		  }, font
	  );
	  dismissBut.setTooltip(Tooltip.create(Component.literal("Dismiss this agent")));
		addRenderableWidget(dismissBut);
  }

  public AgentInventoryScreen(AgentInventoryMenu container, Inventory pPlayerInventory, Component pTitle) {
    super(container, pPlayerInventory, pTitle);
    this.imageHeight = 175;
    this.imageWidth = 300;
    this.agent = container.agent;
    this.inventoryLabelY = this.imageHeight - 94;
    this.player = pPlayerInventory.player;
    df.setRoundingMode(RoundingMode.CEILING);
  }

	protected void updateControlFlag1(){
		int newButtonFlags = agent.getControlFlg1() & ~0x3F;  // get rid of first 6 bit related to these // critical
		if (wanderBut.stateOn)   newButtonFlags |= AbsAgentEntity.BF_WANDER;
		if (followBut.stateOn)   newButtonFlags |= AbsAgentEntity.BF_FOLLOW;
		if (hostileBut.stateOn)  newButtonFlags |= AbsAgentEntity.BF_TARGET_HOSTILE;
		if (humanoidBut.stateOn) newButtonFlags |= AbsAgentEntity.BF_TARGET_AGENT;
		if (specialBut.stateOn)  newButtonFlags |= AbsAgentEntity.BF_ALLOW_SPECIAL;
		if (fastFireBut.stateOn) newButtonFlags |= AbsAgentEntity.BF_RAPID_SHOOTING;

		CHANNELS.sendToServer(new AgentConFlg1Packet(agent.getId(), newButtonFlags));
	}

  @Override
  public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    this.renderCom(pGuiGraphics);
  }

  protected void renderCom(GuiGraphics pGuiGraphics) {
    //somewhat crude and not really work
    Vec3 look = agent.getLookAngle();
    double angleRad = Math.atan2(-look.z, look.x);

    float pitch = (float) Math.PI;
    float yaw = (float) Math.PI/2 + (float) angleRad;
    // Build rotation quaternion (pitch then yaw)
    Quaternionf pose = new Quaternionf().rotationYXZ(yaw, -pitch, 0); // negative pitch flips correctly
    InventoryScreen.renderEntityInInventory(pGuiGraphics, leftPos+33, topPos+72, 30, pose, null, agent);
  }

  @Override
  protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
    renderBackground(pGuiGraphics);
    pGuiGraphics.blit(CONTAINER_BACKGROUND,(this.width - this.imageWidth) / 2, (this.height - this.imageHeight) / 2,0,0,this.imageWidth,this.imageHeight, 300, 175);
  }

  @Override
  protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    // control flag
    pGuiGraphics.drawString(font, agent.getName().getString() +" the " + agent.getAgentType(), 65, 15, 0x00CFFF, false);
    pGuiGraphics.drawString(font, "Commander: " + agent.getOwner(), 65, 40, 0x00CFFF, false);

    pGuiGraphics.drawString(font, agent.getVirtualAmmo() + "/" +agent.getMaxVirtualAmmo(), 242, 110, 0x00CFFF, false);
    pGuiGraphics.drawString(font, agent.getFood() + "/" +agent.maxfood, 242, 133, 0x00CFFF, false);
  }

  @Override
  protected void renderTooltip(GuiGraphics pGuiGraphics, int x, int y) {
    super.renderTooltip(pGuiGraphics, x, y);
    // Example condition — replace with hover bounds check
    if (isHovering(x, y, this.leftPos + 227, this.topPos + 94, 65,25)) {
      pGuiGraphics.renderTooltip(font, Component.literal("Virtual Ammo"), x, y);
    } else if (isHovering(x, y, this.leftPos + 227, this.topPos + 123, 65,25)) {
      pGuiGraphics.renderTooltip(font, Component.literal("Food"), x, y);
    }
  }

  private boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseX <= x + width &&
        mouseY >= y && mouseY <= y + height;
  }
}