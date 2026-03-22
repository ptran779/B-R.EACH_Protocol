package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.effect.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class EffectInit {
	public static final DeferredRegister<MobEffect> EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, BreachPtc.MOD_ID);
	public static final RegistryObject<MobEffect> THERMAL_BLEED =
		EFFECTS.register("thermal_bleed", () -> new ThermalBleed(MobEffectCategory.HARMFUL, 0xE63900));
	public static final RegistryObject<MobEffect> CORROSIVE_SLUDGE =
		EFFECTS.register("corrosive_sludge", () -> new CorrosiveSludge(MobEffectCategory.HARMFUL, 0x7FFF00));
	public static final RegistryObject<MobEffect> EMP_DISRUPTOR =
		EFFECTS.register("emp_disruptor", () -> new EMPDisruptor(MobEffectCategory.HARMFUL, 0x00FFFF));
	public static final RegistryObject<MobEffect> FLASH_BANG =
		EFFECTS.register("flash_bang", () -> new FlashBang(MobEffectCategory.HARMFUL, 0xFFFFFF));
}
