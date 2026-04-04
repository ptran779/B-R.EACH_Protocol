package com.github.ptran779.breach_ptc.server;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class AttributeInit {
	/// Modifier
	public static final AttributeModifier WELL_FEED_SPEED_BOOST =
		new AttributeModifier(UUID.fromString("5f27715e-f97d-4266-a4a5-f76cf488414b"), "Well-fed speed boost", 0.20,
			AttributeModifier.Operation.MULTIPLY_BASE);
}