package com.github.ptran779.breach_ptc.item;

import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.server.EntityInit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BadgeItem extends Item {
	public enum BADGE_TYPE {soldier, medic, heavy, sniper, engineer, swordman, demolition}
	private final BADGE_TYPE type;
	public BadgeItem(Properties pProperties, BADGE_TYPE type) {
		super(pProperties);
		this.type = type;
	}

	private EntityType<? extends AbsAgentEntity> getTargetTypeFromBadge(){
		return switch (type){
			case soldier -> EntityInit.SOLDIER.get();
			case medic -> EntityInit.MEDIC.get();
			case heavy -> EntityInit.HEAVY.get();
			case sniper -> EntityInit.SNIPER.get();
			case engineer -> EntityInit.ENGINEER.get();
			case swordman -> EntityInit.SWORDMAN.get();
			case demolition -> EntityInit.DEMOLITION.get();
		};
	}

	// fixme critical: might cause issue with brain chip. monitor this action
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
		// 1. Only process on server side and only on your Agent entities
		if (!player.level().isClientSide && interactionTarget instanceof AbsAgentEntity oldAgent) {
			AbsAgentEntity newAgent = getTargetTypeFromBadge().create(player.level());
			if (newAgent != null && newAgent.getClass() != oldAgent.getClass()) {
				// keep name
				if (oldAgent.hasCustomName()) {
					newAgent.setCustomName(oldAgent.getCustomName());
					newAgent.setCustomNameVisible(oldAgent.isCustomNameVisible());
				}
				// copy data over
				CompoundTag savedData = new CompoundTag();
				oldAgent.addAdditionalSaveData(savedData); //
				oldAgent.discard();
//				// Clean up NBT if necessary (e.g., removing UUID so the new one gets its own)
//				savedData.remove("UUID");
				newAgent.readAdditionalSaveData(savedData); //
				newAgent.moveTo(oldAgent.getX(), oldAgent.getY(), oldAgent.getZ(), oldAgent.getYRot(), oldAgent.getXRot());

				player.level().addFreshEntity(newAgent);
				((ServerLevel) player.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,oldAgent.getX(), oldAgent.getY(), oldAgent.getZ(), 10,0,0,0, 0.01); // small upward drift

				// Consume badge if not in creative
				if (!player.getAbilities().instabuild) stack.shrink(1);

				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
}
