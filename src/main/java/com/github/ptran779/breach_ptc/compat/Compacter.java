package com.github.ptran779.breach_ptc.compat;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class Compacter {
	public static boolean TACZ_LOADED = false;
	public static boolean JEG_LOADED = false;
	static TaczCompat TACZ_COMPAT;

	private static final List<IGun> GUN_INTEGRATIONS = new ArrayList<>();

	public static void init() {
		if (TACZ_LOADED) TACZ_COMPAT = new TaczCompat();
	}

	public static boolean isAnyGun(Item item) {
		if (TACZ_LOADED & TACZ_COMPAT.isGun(item)) return true;

		return false;
	}
}
