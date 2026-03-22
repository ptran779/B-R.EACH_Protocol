package com.github.ptran779.breach_ptc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class PatrolList extends Item {
	public static final String NBT_NAME = "PatrolRoute";

	public PatrolList(Properties pProperties) {
		super(pProperties);
	}

	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag nbt = stack.getTag();
		int count = (nbt != null && nbt.contains(NBT_NAME, Tag.TAG_LIST)) ? nbt.getList(NBT_NAME, Tag.TAG_INT_ARRAY).size() : 0;

		tooltip.add(Component.literal("Stored Points: " + count).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal("• Right-Click (Block): Add Point").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("• Right-Click (Air): View Route").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("• Shift + Right-Click: Clear Route").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("• Shift + Right-Click (Offhand Map): Copy Route").withStyle(ChatFormatting.GRAY));
	}
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!level.isClientSide) {
			Player player = context.getPlayer();
			if (player == null) return InteractionResult.PASS;

			ItemStack stack = context.getItemInHand();
			CompoundTag nbt = stack.getOrCreateTag();

			if (player.isCrouching()) {
				ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);

				// Copy to Offhand Map if it exists
				if (context.getHand() == InteractionHand.MAIN_HAND && offhandStack.getItem() instanceof PatrolList) {
					if (nbt.contains(NBT_NAME, Tag.TAG_LIST)) {
						CompoundTag offhandNbt = offhandStack.getOrCreateTag();
						offhandNbt.put(NBT_NAME, nbt.getList(NBT_NAME, Tag.TAG_INT_ARRAY).copy());
						player.displayClientMessage(Component.literal("Patrol Route Copied to Offhand!").withStyle(ChatFormatting.AQUA), true);
					} else {
						player.displayClientMessage(Component.literal("No route to copy!").withStyle(ChatFormatting.RED), true);
					}
				}
				// Otherwise clear the map
				else if (nbt.contains(NBT_NAME)) {
					nbt.remove(NBT_NAME);
					player.displayClientMessage(Component.literal("Patrol Route Cleared!").withStyle(ChatFormatting.RED), true);
				}
				return InteractionResult.SUCCESS;
			}

			BlockPos pos = context.getClickedPos().above();

			ListTag routeList;
			if (nbt.contains(NBT_NAME, Tag.TAG_LIST)) {
				routeList = nbt.getList(NBT_NAME, Tag.TAG_INT_ARRAY);
			} else {
				routeList = new ListTag();
			}

			routeList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
			nbt.put(NBT_NAME, routeList);

			player.displayClientMessage(Component.literal("Point Added: " + pos.toShortString()).withStyle(ChatFormatting.GREEN), true);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.SUCCESS;
	}
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		CompoundTag nbt = stack.getOrCreateTag();

		if (player.isCrouching()) {
			if (!level.isClientSide) {
				ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);

				// Copy to Offhand Map if it exists
				if (hand == InteractionHand.MAIN_HAND && offhandStack.getItem() instanceof PatrolList) {
					if (nbt.contains(NBT_NAME, Tag.TAG_LIST)) {
						CompoundTag offhandNbt = offhandStack.getOrCreateTag();
						offhandNbt.put(NBT_NAME, nbt.getList(NBT_NAME, Tag.TAG_INT_ARRAY).copy());
						player.displayClientMessage(Component.literal("Patrol Route Copied to Offhand!").withStyle(ChatFormatting.AQUA), true);
					} else {
						player.displayClientMessage(Component.literal("No route to copy!").withStyle(ChatFormatting.RED), true);
					}
				}
				// Otherwise clear the map
				else if (nbt.contains(NBT_NAME)) {
					nbt.remove(NBT_NAME);
					player.displayClientMessage(Component.literal("Patrol Route Cleared!").withStyle(ChatFormatting.RED), true);
				}
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}

		if (nbt.contains(NBT_NAME, Tag.TAG_LIST)) {
			if (level.isClientSide) {
				ListTag routeList = nbt.getList(NBT_NAME, Tag.TAG_INT_ARRAY);

				for (int i = 0; i < routeList.size(); i++) {
					int[] coords = routeList.getIntArray(i);
					if (coords.length != 3) continue;

					double p1x = coords[0] + 0.5D;
					double p1y = coords[1] + 0.5D;
					double p1z = coords[2] + 0.5D;

					// 1. STRONG MAIN POST PARTICLES
					level.addParticle(ParticleTypes.END_ROD, p1x, p1y, p1z, 0.0D, 0.05D, 0.0D);
					level.addParticle(ParticleTypes.FLAME, p1x, p1y, p1z, 0.0D, 0.05D, 0.0D);
					level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p1x, p1y, p1z, 0.0D, 0.1D, 0.0D);

					// 2. ROUTE DIRECTION PARTICLES (Connects back to the start)
					if (routeList.size() > 1) {
						int nextIndex = (i + 1) % routeList.size(); // Wrap around to 0
						int[] nextCoords = routeList.getIntArray(nextIndex);

						if (nextCoords.length == 3) {
							double p2x = nextCoords[0] + 0.5D;
							double p2y = nextCoords[1] + 0.5D;
							double p2z = nextCoords[2] + 0.5D;

							double dx = p2x - p1x;
							double dy = p2y - p1y;
							double dz = p2z - p1z;
							double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

							// Halved segment count (roughly 1 particle per block)
							int segments = (int) Math.max(1, distance);

							for (int j = 1; j < segments; j++) {
								double t = (double) j / segments;
								double lx = p1x + dx * t;
								double ly = p1y + dy * t;
								double lz = p1z + dz * t;

								level.addParticle(ParticleTypes.HAPPY_VILLAGER, lx, ly, lz, 0.0D, 0.0D, 0.0D);
							}
						}
					}
				}
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		} else if (!level.isClientSide) {
			player.displayClientMessage(Component.literal("No Patrol Route stored.").withStyle(ChatFormatting.RED), true);
		}

		return InteractionResultHolder.pass(stack);
	}
}