package com.github.ptran779.breach_ptc.block;

import com.github.ptran779.breach_ptc.server.BlockInit;
import com.github.ptran779.breach_ptc.server.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


public class KSeedCoreUsed1 extends Block {
  public KSeedCoreUsed1(Properties pProperties) {
    super(pProperties);
  }
  public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
    super.onPlace(state, world, pos, oldState, isMoving);

    BlockPos below1 = pos.below();
	  BlockPos below2 = below1.below();
    world.setBlock(below1, BlockInit.K_SEED_CORE_USED_2.get().defaultBlockState(), Block.UPDATE_ALL);
	  world.setBlock(below2, BlockInit.K_SEED_CORE_USED_3.get().defaultBlockState(), Block.UPDATE_ALL);
  }

  public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
    super.onRemove(state, world, pos, newState, isMoving);

    if (!state.is(newState.getBlock())) {
      BlockPos below1 = pos.below();
	    BlockPos below2 = below1.below();
      if (world.getBlockState(below1).getBlock() == BlockInit.K_SEED_CORE_USED_2.get()) {
        world.removeBlock(below1, false);
      }
			if (world.getBlockState(below2).getBlock() == BlockInit.K_SEED_CORE_USED_3.get()) {
				world.removeBlock(below2, false);
			}
    }
  }
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    return new ItemStack(ItemInit.K_SEED_CORE_ITEM.get());
  }
}
