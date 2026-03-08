package com.github.ptran779.breach_ptc.server;

import com.github.ptran779.breach_ptc.BreachPtc;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BreachPtc.MOD_ID);

	public static final RegistryObject<SimpleParticleType> MAGAZINE_PARTICLE =
		PARTICLE_TYPES.register("magazine_particle", ()-> new SimpleParticleType(true));
}
