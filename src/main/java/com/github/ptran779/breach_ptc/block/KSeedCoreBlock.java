package com.github.ptran779.breach_ptc.block;

import com.github.ptran779.breach_ptc.block_entity.KSeedCoreBE;
import com.github.ptran779.breach_ptc.server.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KSeedCoreBlock extends BaseEntityBlock {
	public static final VoxelShape SHAPE_CLOSE = Shapes.or(
		Block.box(0, -32, 0, 0, 0, 16),
		Block.box(0, -32, 0, 16, 0, 0),
		Block.box(0, -32, 16, 16, 0, 16),
		Block.box(16, -32, 0, 16, 0, 16),
		Block.box(0, -32, 0, 16, -32, 16)
	);
	public static final VoxelShape SHAPE_DEPLOYED = Shapes.or(
		Block.box(0, -32, 0, 16, 0, 16)
	);
	public static final BooleanProperty DEPLOY = BooleanProperty.create("deploy");
	public KSeedCoreBlock(Properties pProperties) {
		super(pProperties);
		this.registerDefaultState(this.stateDefinition.any().setValue(DEPLOY, false));
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(DEPLOY);
	}

	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return state.getValue(DEPLOY)? SHAPE_DEPLOYED : SHAPE_CLOSE;
	}
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.INVISIBLE;
	}
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == BlockEntityInit.K_SEED_CORE_BE.get() ?
			(lvl, pos, blkState, be) -> ((KSeedCoreBE) be).tick() : null;
	}
	public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new KSeedCoreBE(blockPos, blockState);
	}
}
