package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.entity.inventory.AgentAdvanceConfigMenu;
import com.github.ptran779.breach_ptc.entity.inventory.AgentInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public final class MenuInit {
  public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BreachPtc.MOD_ID);

  public static final RegistryObject<MenuType<AgentInventoryMenu>>
	  BREACH_PTC_MENU1 = MENU_TYPES.register("aegisops_menu1", () -> IForgeMenuType.create(AgentInventoryMenu::new));
  public static final RegistryObject<MenuType<AgentAdvanceConfigMenu>>
	  BREACH_PTC_MENU2 = MENU_TYPES.register("aegisops_menu2", () -> IForgeMenuType.create(AgentAdvanceConfigMenu::new));
}