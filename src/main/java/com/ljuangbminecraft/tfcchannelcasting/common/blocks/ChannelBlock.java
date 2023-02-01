package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import java.util.Map;
import java.util.Optional;

import com.ljuangbminecraft.tfcchannelcasting.common.ChannelFlow;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.CrucibleBlock;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChannelBlock extends ExtendedBlock implements EntityBlockExtension
{
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream()
        .filter(facing -> facing.getKey() != Direction.UP).collect(Util.toMap());

    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty WITH_METAL = BooleanProperty.create("with_metal");
    
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static
    {
        final VoxelShape base  = box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);

        final VoxelShape east  = box(11.0D, 0.0D, 5.0D,  16.0D, 5.0D, 11.0D);
        final VoxelShape south = box(5.0D,  0.0D, 11.0D, 11.0D, 5.0D, 16.0D);
        final VoxelShape west  = box(0.0D,  0.0D, 5.0D,  5.0D,  5.0D, 11.0D);
        final VoxelShape north = box(5.0D,  0.0D, 0.0D,  11.0D, 5.0D, 10.0D);

        final VoxelShape[] directions = new VoxelShape[] {south, west, north, east};

        for (int i = 0; i < SHAPES.length; i++)
        {
            VoxelShape shape = base;
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                if (((i >> direction.get2DDataValue()) & 1) == 1)
                {
                    shape = Shapes.or(shape, directions[direction.get2DDataValue()]);
                }
            }
            SHAPES[i] = shape;
        }
    }

    private static BlockState updateConnectedSides(LevelAccessor level, BlockPos pos, BlockState state)
    {
        for (final Direction direction : Direction.values())
        {
            if (direction == Direction.UP) continue;

            final BlockPos adjacentPos = pos.relative(direction);
            final BlockState adjacentState = level.getBlockState(adjacentPos);
            final Block adjancentBlock = adjacentState.getBlock();
            final boolean isAdjacentConnectable = adjancentBlock instanceof ChannelBlock || adjancentBlock instanceof CrucibleBlock || adjancentBlock instanceof MoldBlock;
            state = state.setValue(DirectionPropertyBlock.getProperty(direction), isAdjacentConnectable);
        }

        return state;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        // On destroy, notify source channel that all flows going through this
        // channel have been broken
        if (!level.isClientSide()) 
        {
            level.getBlockEntity(pos, TFCCCBlockEntities.CHANNEL.get()).ifPresent(
                channel -> channel.notifyBrokenLink(channel.getNumberOfFlows())
            );
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion)
    {
        // On destroy, notify source channel that all flows going through this
        // channel have been broken
        if (!level.isClientSide()) 
        {
            level.getBlockEntity(pos, TFCCCBlockEntities.CHANNEL.get()).ifPresent(
                channel -> channel.notifyBrokenLink(channel.getNumberOfFlows())
            );
        }

        super.onBlockExploded(state, level, pos, explosion);
    }

    public ChannelBlock(ExtendedProperties properties) 
    {
        super(properties);
        this.registerDefaultState(
            this.defaultBlockState()
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(NORTH, false)
            .setValue(DOWN, false)
            .setValue(TRIGGERED, false)
            .setValue(WITH_METAL, false)
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[(state.getValue(NORTH) ? 1 << Direction.NORTH.get2DDataValue() : 0) |
            (state.getValue(EAST) ? 1 << Direction.EAST.get2DDataValue() : 0) |
            (state.getValue(SOUTH) ? 1 << Direction.SOUTH.get2DDataValue() : 0) |
            (state.getValue(WEST) ? 1 << Direction.WEST.get2DDataValue() : 0)];
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacentState, LevelAccessor level, BlockPos pos, BlockPos adjacentPos)
    {
        return updateConnectedSides(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        boolean flag = pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.above());
        boolean flag1 = pState.getValue(TRIGGERED);
        if (flag && !flag1) {
            activate(pLevel, pPos);
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 4);
        } else if (!flag && flag1) {
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)), 4);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(EAST, SOUTH, WEST, NORTH, DOWN, TRIGGERED, WITH_METAL);
    }

    /***
     * Right-clicking a channel block will cause it to try to find crucibles
     * adjacent to it and start a flow if appropriate.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            activate(level, pos);
        }
        return InteractionResult.FAIL;
    }

    public boolean activate(LevelAccessor level, BlockPos pos)
    {
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            Optional<CrucibleBlockEntity> crucible = level.getBlockEntity(pos.relative(dir), TFCBlockEntities.CRUCIBLE.get());
            if (crucible.isPresent())
            {
                ChannelFlow.fromCrucible(level, crucible.get(), pos);
                return true;
            }
        }
        return false;
    }
}
