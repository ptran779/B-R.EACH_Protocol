package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.BreachPtc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.github.ptran779.breach_ptc.attribute.AgentAttribute.AGENT_ATTACK_SPEED;

public class AttributeInit {
  public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, BreachPtc.MOD_ID);

  public static final RegistryObject<Attribute> AGENT_ATTACK_SPEED_ATTR =ATTRIBUTES.register("agent_attack_speed", ()-> AGENT_ATTACK_SPEED);
}