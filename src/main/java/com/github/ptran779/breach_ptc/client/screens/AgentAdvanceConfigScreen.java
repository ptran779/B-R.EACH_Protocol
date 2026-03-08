package com.github.ptran779.breach_ptc.client.screens;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.ai.api.ScoreCompiler;
import com.github.ptran779.breach_ptc.client.widgets.DropDownWidget;
import com.github.ptran779.breach_ptc.client.widgets.MultiOptionButton;
import com.github.ptran779.breach_ptc.client.widgets.ToggleButton;
import com.github.ptran779.breach_ptc.config.SkinManager;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.inventory.AgentAdvanceConfigMenu;
import com.github.ptran779.breach_ptc.item.BrainChipItem;
import com.github.ptran779.breach_ptc.network.advConfScr.SetATrainConf;
import com.github.ptran779.breach_ptc.network.advConfScr.SetInputSenStream;
import com.github.ptran779.breach_ptc.network.advConfScr.SetScoreStream;
import com.github.ptran779.breach_ptc.network.agent.AgentBrainChipPacket;
import com.github.ptran779.breach_ptc.network.advConfScr.ChangeSkinPacket;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import com.github.ptran779.breach_ptc.network.agent.AgentInventoryPacket;
import com.github.ptran779.breach_ptc.network.ml_packet.CreateNewBrain;
import com.github.ptran779.breach_ptc.network.advConfScr.ReloadBrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.math.RoundingMode;
import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class AgentAdvanceConfigScreen extends AbstractContainerScreen<AgentAdvanceConfigMenu>{
  private static final Font font = Minecraft.getInstance().font;
  private static final ResourceLocation BACKGROUND = new ResourceLocation(BreachPtc.MOD_ID,"textures/gui/a_inv_scr2.png");
  private static final ResourceLocation BUTTON = new ResourceLocation(BreachPtc.MOD_ID,"textures/gui/a_inv_but2.png");

  private final Player player;
  private final AbsAgentEntity agent;
  DecimalFormat df = new DecimalFormat("#.#");
  MultiOptionButton chipConfigBut, inputSentivityBut, costFuncBut;
	ToggleButton autoTrainBut, autoCollectBut;
	private EditBox inSenBox, costFuncBox, impMinBoxBut, explorBoxBut;

	public AgentAdvanceConfigScreen(AgentAdvanceConfigMenu container, Inventory pPlayerInventory, Component pTitle) {
		super(container, pPlayerInventory, pTitle);
		this.imageHeight = 175;
		this.imageWidth = 300;
		this.agent = container.agent;
		this.inventoryLabelY = this.imageHeight - 94;
		this.player = pPlayerInventory.player;
		df.setRoundingMode(RoundingMode.CEILING);
	}

  protected void init(){
    super.init();
    PlainTextButton goback = new PlainTextButton(this.leftPos+227, this.topPos+148, 65, 19, Component.empty(),
	    bnt->{
			PacketHandler.CHANNELS.sendToServer(new AgentInventoryPacket(agent.getId()));
	    }, font);
		addRenderableWidget(goback);
		DropDownWidget skinDropdown = new DropDownWidget(this.leftPos + 63, this.topPos + 27,76,14, 8, agent.getSkin(),
	    SkinManager.getAllSkin(agent.getFemale()), selected -> {
        PacketHandler.CHANNELS.sendToServer(new ChangeSkinPacket(agent.getId(), selected)); // server sync
        agent.setSkin(selected);  // if server too slow, use this to flush on client quickly
        agent.flushSkinCache();  // if server slow, we might have problem :)
	      //fixme critical -- check for cache usage, cause this is local client, how does multi server deal with?
      }
    );
    this.addRenderableWidget(skinDropdown);

    chipConfigBut = new MultiOptionButton(this.leftPos+63, this.topPos+46, 76, 16, 0, 0, 16, BUTTON, 120, 64, bnt->{
	    ItemStack stack = menu.getSlot(0).getItem();
			if (stack.getItem() instanceof BrainChipItem brainChipItem) {
		    if (menu.mlInput.get() != agent.getSensorSize() || menu.mlOutput.get() != agent.getBehaviorSize()) {
					if (chipConfigBut.option == 1) {
						chipConfigBut.option = 2;
						return;
					}
					//clean old brain if not match
			    PacketHandler.CHANNELS.sendToServer(new CreateNewBrain(brainChipItem.getOrCreateUUID(stack),agent.getSensorSize(), agent.getBehaviorSize(), new byte[0]));
		    }
				PacketHandler.CHANNELS.sendToServer(new AgentBrainChipPacket(agent.getId(), brainChipItem.getOrCreateUUID(stack)));
			}
		});
	  chipConfigBut.setTooltip(Tooltip.create(Component.literal("Edit ML model")));
    this.addRenderableWidget(chipConfigBut);

	  Button brainReload = new PlainTextButton(this.leftPos+64, this.topPos+67, 74, 11, Component.empty(), bnt->{
		  PacketHandler.CHANNELS.sendToServer(new ReloadBrain(agent.getId()));
	  }, font);
	  this.addRenderableWidget(brainReload);
	  brainReload.setTooltip(Tooltip.create(Component.literal("Reload Brain Usage. Use Brain if Valid Chip Model")));

	  ///Input
	  inSenBox = new EditBox(font, this.leftPos+64, this.topPos+100, 158, 11, Component.empty());
	  inSenBox.setMaxLength(4096);
	  inSenBox.setResponder(txt ->{inputSentivityBut.option = 2;inputSentivityBut.active = true;});
	  this.addRenderableWidget(inSenBox);
	  inputSentivityBut = new MultiOptionButton(this.leftPos+144, this.topPos+83, 13, 13, 76, 0, 13, BUTTON, 120, 64, bnt->{
		  inputSentivityBut.active = false;
			float[] out = SetInputSenStream.parseAndValidate(inSenBox.getValue());
			if (out == null){inputSentivityBut.option = 1;}
			else {
				inputSentivityBut.option = 0;
				PacketHandler.CHANNELS.sendToServer(new SetInputSenStream(agent.getId(), out));
			}
	  });
		this.addRenderableWidget(inputSentivityBut);
	  inputSentivityBut.setTooltip(Tooltip.create(Component.literal("set input sensitivity level")));

		/// cost
	  costFuncBox = new EditBox(font, this.leftPos+64, this.topPos+132, 158, 11, Component.empty());
	  costFuncBox.setMaxLength(4096);
	  costFuncBox.setResponder(txt ->{costFuncBut.option = 2;costFuncBut.active = true;});
		this.addRenderableWidget(costFuncBox);
	  costFuncBut = new MultiOptionButton(this.leftPos+144, this.topPos+115, 13, 13, 76, 0, 13, BUTTON, 120, 64, bnt -> {
		  try {
			  costFuncBut.active = false;
			  ScoreCompiler score = new ScoreCompiler(costFuncBox.getValue());
				score.validate(agent.getSensorSize(), agent.getScrCustVarSize());
			  costFuncBut.option = 0;
				PacketHandler.CHANNELS.sendToServer(new SetScoreStream(agent.getId(), score.getInstructions(), score.getConstants()));
		  }
			catch (RuntimeException e){
				costFuncBut.option = 1;
			}
	    });
	  this.addRenderableWidget(costFuncBut);
	  costFuncBut.setTooltip(Tooltip.create(Component.literal("set cost func")));

	  autoTrainBut = new ToggleButton(this.leftPos+227, this.topPos+92, 31, 11, 89, 0, 11, BUTTON, 120, 64, bnt->{
		  autoTrainBut.stateOn = ! autoTrainBut.stateOn;
			if (autoTrainBut.stateOn) {
				autoCollectBut.stateOn = true;
				autoCollectBut.active = false;
			} else {
				autoCollectBut.active = true;
			}
	  });
	  autoCollectBut = new ToggleButton(this.leftPos+261, this.topPos+92, 31, 11, 89, 22, 11, BUTTON, 120, 64, bnt->{
		  autoCollectBut.stateOn = ! autoCollectBut.stateOn;
	  });
	  impMinBoxBut = new EditBox(font, this.leftPos+263, this.topPos+106, 28, 9, Component.empty());
	  explorBoxBut = new EditBox(font, this.leftPos+263, this.topPos+119, 28, 9, Component.empty());

		autoTrainBut.setTooltip(Tooltip.create(Component.literal("Toggle auto train")));
	  autoCollectBut.setTooltip(Tooltip.create(Component.literal("Toggle exp collection. Must have valid cost func")));
	  impMinBoxBut.setTooltip(Tooltip.create(Component.literal("If auto train and has not improve for x session, turn " +
		  "autotrain off")));
	  explorBoxBut.setTooltip(Tooltip.create(Component.literal("How often agent try to pick a random action (used to " +
		  "encourage trying random thing (1 == every decision is random))")));

	  this.addRenderableWidget(autoTrainBut);
	  this.addRenderableWidget(autoCollectBut);
	  this.addRenderableWidget(impMinBoxBut);
	  this.addRenderableWidget(explorBoxBut);

		Button commitTrainConfig = new PlainTextButton(this.leftPos+227, this.topPos+131, 35, 13, Component.empty(), bnt ->{
			int impRate;
			try {impRate = Integer.parseInt(impMinBoxBut.getValue());}
			catch (NumberFormatException e) {
				impRate = 1; // Safe default
				impMinBoxBut.setValue("1"); // Reset the UI so the user knows it failed
			}
			float expRate;
			try {expRate = Float.parseFloat(explorBoxBut.getValue());}
			catch (NumberFormatException e) {
				expRate = 0; // Safe default
				explorBoxBut.setValue("0"); // Reset the UI so the user knows it failed
			}
			PacketHandler.CHANNELS.sendToServer(new SetATrainConf(agent.getId(), autoTrainBut.stateOn, autoCollectBut.stateOn, Math.max(impRate, 1), Math.max(expRate, 0)));
			impMinBoxBut.setValue(String.valueOf(Math.max(impRate, 1)));
			explorBoxBut.setValue(String.valueOf(Math.max(expRate, 0)));
		}, font);
	  commitTrainConfig.setTooltip(Tooltip.create(Component.literal("Push Auto Train Config")));
		this.addRenderableWidget(commitTrainConfig);
  }

	public void updateInSenBox(String datArr){
		inSenBox.setValue(datArr);
		inputSentivityBut.option=0;
	}
	public void updateCostFuncBox(String datArr){
		costFuncBox.setValue(datArr);
		if (datArr.isEmpty()) {costFuncBut.option=1;}
		else {costFuncBut.option=0;}
	}
	public void updateATrainConf(boolean autoTrain, boolean collectExp, int impTime, float exploreRate){
		autoTrainBut.stateOn = autoTrain;
		autoCollectBut.stateOn = collectExp;
		impMinBoxBut.setValue(String.valueOf(impTime));
		explorBoxBut.setValue(String.valueOf(exploreRate));
	}
	// forward mouse scrolling to child widget
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    // Forward scroll to all children
    for (GuiEventListener widget : this.children()) {
      if (widget.mouseScrolled(mouseX, mouseY, delta)) return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }
  public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    this.renderCom(pGuiGraphics);
  }
  protected void renderCom(GuiGraphics pGuiGraphics) {
    Vec3 look = agent.getLookAngle();
    double angleRad = Math.atan2(-look.z, look.x);

    float pitch = (float) Math.PI;
    float yaw = (float) Math.PI/2 + (float) angleRad;
    // Build rotation quaternion (pitch then yaw)
    Quaternionf pose = new Quaternionf().rotationYXZ(yaw, -pitch, 0); // negative pitch flips correctly
    InventoryScreen.renderEntityInInventory(pGuiGraphics, leftPos+33, topPos+72, 30, pose, null, agent);
  }
  protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
    renderBackground(pGuiGraphics);
    pGuiGraphics.blit(BACKGROUND,(this.width - this.imageWidth) / 2, (this.height - this.imageHeight) / 2,0,0,this.imageWidth,this.imageHeight, 300, 175);
  }
  protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    // Skin Selection
    pGuiGraphics.drawString(font, "Skin Selection", 66, 11, 0x00CFFF, false);
  }
  protected void containerTick() {
	  super.containerTick();
	  if (chipConfigBut.option != 2) {
		  if (this.menu.getSlot(0).getItem().getItem() instanceof BrainChipItem) {
			  chipConfigBut.active = true;
			  if (this.menu.mlInput.get() == agent.getSensorSize() && this.menu.mlOutput.get() == agent.getBehaviorSize()) {
				  chipConfigBut.option = 3;
			  } else {
				  chipConfigBut.option = 1;
			  }
		  } else {
			  chipConfigBut.option = 0;
			  chipConfigBut.active = false;
		  }
	  }
  }
}