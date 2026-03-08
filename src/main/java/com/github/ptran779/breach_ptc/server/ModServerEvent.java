package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.extra.VectorPursuer;
import com.github.ptran779.breach_ptc.entity.structure.AbstractAgentStruct;
import com.github.ptran779.breach_ptc.network.PacketHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = BreachPtc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModServerEvent {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.SOLDIER.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.SNIPER.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.HEAVY.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.DEMOLITION.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.MEDIC.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.ENGINEER.get(), AbsAgentEntity.createAttributes().build());
        event.put(EntityInit.SWORDMAN.get(), AbsAgentEntity.createAttributes().build());
        // drop pod
        AttributeSupplier podAttr = LivingEntity.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0).build();
        event.put(EntityInit.FALLING_DROP_POD.get(), podAttr);
        event.put(EntityInit.FALLING_HELL_POD.get(), podAttr);
        // deployable structure
        AttributeSupplier structAttr = AbstractAgentStruct.createAttributes().build();
        event.put(EntityInit.BD_TURRET.get(), structAttr);
        event.put(EntityInit.PORT_DISP.get(), structAttr);
        // pursue vector
        event.put(EntityInit.VECTOR_PURSUER.get(), VectorPursuer.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPacketHandler(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
