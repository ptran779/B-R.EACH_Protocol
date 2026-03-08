package com.github.ptran779.breach_ptc;

import com.github.ptran779.breach_ptc.config.ServerConfig;
import com.github.ptran779.breach_ptc.server.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BreachPtc.MOD_ID)
public class BreachPtc {
  public static final String MOD_ID = "breach_ptc";
	public static final Logger LOGGER = LogUtils.getLogger();
    public BreachPtc() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

      ItemInit.ITEMS.register(modEventBus);
      CreativeTabInit.CREATIVE_MODE_TABS.register(modEventBus);

      EntityInit.ENTITIES.register(modEventBus);
      MenuInit.MENU_TYPES.register(modEventBus);

      AttributeInit.ATTRIBUTES.register(modEventBus);
      EffectInit.EFFECTS.register(modEventBus);

      BlockInit.BLOCKS.register(modEventBus);
      BlockEntityInit.BLOCK_ENTITY.register(modEventBus);

      ParticleInit.PARTICLE_TYPES.register(modEventBus);

      ServerConfig.register();
  }
}
