package com.github.ptran779.breach_ptc.compat;

import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.world.item.Item;

public class TaczCompat implements IGun{
	public boolean isGun(Item item) {
		return item instanceof ModernKineticGunItem;
	}
}