package com.github.ptran779.breach_ptc.block;

import com.github.ptran779.breach_ptc.server.BlockInit;
import com.github.ptran779.breach_ptc.server.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


public class KSeedCoreUsed3 extends Block {
  public KSeedCoreUsed3(Properties pProperties) {
    super(pProperties);
  }

  public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
    super.onRemove(state, world, pos, newState, isMoving);

    if (!state.is(newState.getBlock())) {
      BlockPos above1 = pos.above();
	    BlockPos above2 = above1.below();
      if (world.getBlockState(above1).getBlock() == BlockInit.K_SEED_CORE_USED_2.get()) {
        world.removeBlock(above1, false);
      }
			if (world.getBlockState(above2).getBlock() == BlockInit.K_SEED_CORE_USED_1.get()) {
				world.removeBlock(above2, false);
			}
    }
  }
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    return new ItemStack(ItemInit.K_SEED_CORE_ITEM.get());
  }
}
