package com.github.ptran779.breach_ptc.block;

import com.github.ptran779.breach_ptc.server.BlockInit;
import com.github.ptran779.breach_ptc.server.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


public class KSeedCoreUsed2 extends Block {
  public KSeedCoreUsed2(Properties pProperties) {
    super(pProperties);
  }

  public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
    super.onRemove(state, world, pos, newState, isMoving);

    if (!state.is(newState.getBlock())) {
      BlockPos below1 = pos.below();
	    BlockPos above = pos.above();
      if (world.getBlockState(below1).getBlock() == BlockInit.K_SEED_CORE_USED_3.get()) {
        world.removeBlock(below1, false);
      }
			if (world.getBlockState(above).getBlock() == BlockInit.K_SEED_CORE_USED_1.get()) {
				world.removeBlock(above, false);
			}
    }
  }
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    return new ItemStack(ItemInit.K_SEED_CORE_ITEM.get());
  }
}
