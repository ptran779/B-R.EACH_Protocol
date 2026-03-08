package com.github.ptran779.breach_ptc.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IHealItem {
  boolean canHeal(LivingEntity target);
  void heal(LivingEntity target, ItemStack stack);
  int getAniMove();
  boolean computeEffect(LivingEntity target, int tickcount, ItemStack stack);
}
