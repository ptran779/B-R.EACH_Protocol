package com.github.ptran779.breach_ptc.block_entity;

import com.github.ptran779.breach_ptc.block.HellPodBlock;
import com.github.ptran779.breach_ptc.server.BlockEntityInit;
import com.github.ptran779.breach_ptc.server.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HellPodBE extends BlockEntity {
  public int tickcount = -40;
  public HellPodBE(BlockPos pPos, BlockState pBlockState) {
    super(BlockEntityInit.HELL_POD_BE.get(), pPos, pBlockState);
  }

  public void tick() {
    tickcount++;
    if (!level.isClientSide()) {
      if (tickcount == 0) {
        ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD,
            worldPosition.getX(), worldPosition.getY() + 2.0, worldPosition.getZ(),
            10, 0.25, 0.25, 0.25, 0.02);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0f, 1.0f);
      } else if(tickcount == 10) {
        level.playSound(null, worldPosition, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
      } else if (tickcount == 40) {
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 1.0f);
      } else if (tickcount == 30) {
        BlockState current = level.getBlockState(worldPosition);
        level.setBlock(worldPosition, current.setValue(HellPodBlock.CLOSE, true), 3);
      } else if (tickcount >= 50) {
        level.setBlock(worldPosition, BlockInit.HELL_POD_USED_BOT.get().defaultBlockState(), 3);
        level.removeBlockEntity(worldPosition);
      }

      if(tickcount >= 10 && tickcount <= 30) {
        double minX = worldPosition.getX() - 0.5;
        double maxX = worldPosition.getX() + 0.5;
        double minY = worldPosition.getY();
        double maxY = worldPosition.getY() + Math.min((tickcount - 10) * 0.1, 2); // full drop pod interior
        double minZ = worldPosition.getZ() + - 0.5;
        double maxZ = worldPosition.getZ() + 0.5;

        level.getEntities(null, new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ))
            .forEach(entity -> {
              entity.hurtMarked = true;
              if (tickcount <= 30){
                entity.setDeltaMovement(0, 0.2, 0);
              } else {
                entity.teleportTo(entity.getX(), maxY, entity.getZ());
              }
            });
      }
    }
  }
}
